package services.db;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.netra.commons.exceptions.AppDataAccessException;
import play.db.Database;
import play.libs.concurrent.HttpExecutionContext;

/**
 * JDBC helper that supports both stored procedures and PostgreSQL functions (returns TABLE).
 * Auto-detects whether the target is a function or procedure via pg_catalog.
 */
@Singleton
public class JdbcWrapper {

    private static final Logger LOGGER = Logger.getLogger(JdbcWrapper.class.getName());
    private final Database db;
    private final Executor blockingExecutor;

    @Inject
    public JdbcWrapper(Database db, HttpExecutionContext httpExecutionContext) {
        this.db = db;
        this.blockingExecutor = httpExecutionContext.current();
    }

    /* =========================================================
       ============= TRANSACTION MANAGEMENT =====================
       ========================================================= */

    public <T> CompletionStage<T> withConditionalTransaction(
            Function<Connection, T> transactionFunction,
            Function<Exception, Boolean> shouldRollback) {

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = db.getConnection()) {
                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                try {
                    T result = transactionFunction.apply(conn);
                    conn.commit();
                    return result;

                } catch (Exception e) {
                    if (shouldRollback.apply(e)) {
                        try {
                            conn.rollback();
                            LOGGER.severe("Rolled back due to: " + e.getMessage());
                        } catch (SQLException rollbackEx) {
                            throw new AppDataAccessException("Rollback failed", rollbackEx);
                        }
                        throw new AppDataAccessException("Transaction rolled back", e);
                    } else {
                        try {
                            conn.commit();
                            LOGGER.warning("Committed despite exception: " + e.getMessage());
                        } catch (SQLException commitEx) {
                            throw new AppDataAccessException("Commit failed after exception", commitEx);
                        }
                        throw e;
                    }
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }

            } catch (Exception outer) {
                throw new AppDataAccessException("Transaction execution failed", outer);
            }
        }, blockingExecutor);
    }

    public <T> CompletionStage<T> withTransaction(Function<Connection, T> transactionFunction) {
        return CompletableFuture.supplyAsync(() ->
                db.withTransaction(conn -> {
                    try {
                        return transactionFunction.apply(conn);
                    } catch (Exception e) {
                        LOGGER.severe("Transaction failed: " + e.getMessage());
                        throw new AppDataAccessException("Transaction execution failed", e);
                    }
                }), blockingExecutor);
    }

    /* =========================================================
       ================== PROCEDURE CALL ========================
       ========================================================= */

    public class ProcedureCall {
        private final String procedureName;
        private final List<Object> singleParameters = new ArrayList<>();
        private final Map<Integer, Integer> outParameters = new HashMap<>();
        private Connection connection;
        private Boolean cachedIsFunction = null; // avoid repeated catalog lookups

        private ProcedureCall(String procedureName) {
            this.procedureName = procedureName;
        }

        private ProcedureCall(String procedureName, Connection connection) {
            this.procedureName = procedureName;
            this.connection = connection;
        }

        public ProcedureCall param(Object value) {
            singleParameters.add(value);
            return this;
        }

        public ProcedureCall outParam(int position, int sqlType) {
            outParameters.put(position, sqlType);
            return this;
        }

        /* =========================================================
           ================== MAIN QUERY METHODS ===================
           ========================================================= */

        public <T> CompletionStage<T> queryAsync(Function<ResultSet, T> mapper) {
            if (singleParameters.isEmpty()) {
                throw new IllegalStateException("No parameters provided");
            }

            final List<Object> params = new ArrayList<>(singleParameters);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    return db.withConnection(conn -> {
                        boolean isFunction = determineIfFunction(conn);
                        if (isFunction) {
                            return executeFunctionQuery(conn, params, mapper);
                        } else {
                            return executeProcedureQuery(conn, params, mapper);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.severe("Failed to query " + procedureName + ": " + e.getMessage());
                    throw new AppDataAccessException("Procedure/function query failed", e);
                }
            }, blockingExecutor);
        }

        public <T> T query(Function<ResultSet, T> mapper) {
            if (singleParameters.isEmpty()) {
                throw new IllegalStateException("No parameters provided");
            }

            try {
                return db.withConnection(conn -> {
                    boolean isFunction = determineIfFunction(conn);
                    if (isFunction) {
                        return executeFunctionQuery(conn, singleParameters, mapper);
                    } else {
                        return executeProcedureQuery(conn, singleParameters, mapper);
                    }
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to query " + procedureName + ": " + e.getMessage());
                throw new AppDataAccessException("Procedure/function query failed", e);
            }
        }

        /* =========================================================
           ================== INTERNAL HELPERS =====================
           ========================================================= */

        private boolean determineIfFunction(Connection conn) {
            if (cachedIsFunction != null) return cachedIsFunction;
            String sql = "SELECT COUNT(*) FROM pg_catalog.pg_proc WHERE proname = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, procedureName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        cachedIsFunction = rs.getInt(1) > 0;
                        return cachedIsFunction;
                    }
                }
            } catch (SQLException e) {
                LOGGER.warning("Could not determine if " + procedureName + " is function: " + e.getMessage());
            }
            cachedIsFunction = false;
            return false;
        }

        private <T> T executeFunctionQuery(Connection conn, List<Object> params, Function<ResultSet, T> mapper) {
            String sql = "SELECT * FROM " + procedureName + "(" +
                    String.join(",", Collections.nCopies(params.size(), "?")) + ")";
            LOGGER.info("Executing PostgreSQL function: " + sql);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    return mapper.apply(rs);
                }
            } catch (SQLException e) {
                throw new AppDataAccessException("Function query failed for " + procedureName, e);
            }
        }

        private <T> T executeProcedureQuery(Connection conn, List<Object> params, Function<ResultSet, T> mapper) {
            try (CallableStatement stmt = prepareCallable(conn, params)) {
                boolean hasResultSet = stmt.execute();
                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        return mapper.apply(rs);
                    }
                }
                throw new SQLException("No result set returned from procedure " + procedureName);
            } catch (SQLException e) {
                throw new AppDataAccessException("Procedure call failed for " + procedureName, e);
            }
        }

        private CallableStatement prepareCallable(Connection conn, List<Object> params) throws SQLException {
            String placeholders = params.stream().map(p -> "?").collect(Collectors.joining(","));
            String callSql = "{ call " + procedureName + "(" + placeholders + ") }";
            CallableStatement stmt = conn.prepareCall(callSql);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            for (Map.Entry<Integer, Integer> e : outParameters.entrySet()) {
                stmt.registerOutParameter(e.getKey(), e.getValue());
            }
            return stmt;
        }
    }

    /* =========================================================
       ================== BASIC QUERY WRAPPER ==================
       ========================================================= */

    public class QueryCall {
        private final String sql;
        private final List<Object> parameters = new ArrayList<>();

        private QueryCall(String sql) {
            this.sql = sql;
        }

        public QueryCall param(Object value) {
            parameters.add(value);
            return this;
        }

        public <T> CompletionStage<T> queryAsync(Function<ResultSet, T> mapper) {
            final String finalSql = sql;
            final List<Object> finalParams = new ArrayList<>(parameters);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    return db.withConnection(conn -> {
                        try (PreparedStatement stmt = conn.prepareStatement(finalSql)) {
                            for (int i = 0; i < finalParams.size(); i++) {
                                stmt.setObject(i + 1, finalParams.get(i));
                            }
                            try (ResultSet rs = stmt.executeQuery()) {
                                return mapper.apply(rs);
                            }
                        }
                    });
                } catch (Exception e) {
                    LOGGER.severe("Failed to execute query: " + e.getMessage());
                    throw new AppDataAccessException("Query failed", e);
                }
            }, blockingExecutor);
        }

        public <T> T query(Function<ResultSet, T> mapper) {
            try {
                return db.withConnection(conn -> {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        for (int i = 0; i < parameters.size(); i++) {
                            stmt.setObject(i + 1, parameters.get(i));
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
                            return mapper.apply(rs);
                        }
                    }
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to execute query: " + e.getMessage());
                throw new AppDataAccessException("Query failed", e);
            }
        }
    }

    /* =========================================================
       ================== ENTRY POINTS =========================
       ========================================================= */

    public ProcedureCall call(String name) {
        return new ProcedureCall(name);
    }

    public ProcedureCall call(String name, Connection conn) {
        return new ProcedureCall(name, conn);
    }

    public QueryCall sql(String sql) {
        return new QueryCall(sql);
    }
}
