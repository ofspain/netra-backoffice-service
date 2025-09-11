package services.db;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.netra.commons.exceptions.AppDataAccessException;

import play.db.Database;
import play.libs.concurrent.HttpExecutionContext;

@Singleton
public class JdbcWrapper {

    private static final Logger LOGGER = Logger.getLogger(JdbcWrapper.class.getName());
    private final Database db;

    private final Executor blockingExecutor; // Play DB EC recommended

    @Inject
    public JdbcWrapper(Database db,  HttpExecutionContext httpExecutionContext) {
        this.db = db;
        blockingExecutor = httpExecutionContext.current();
    }

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

    // Transaction execution method
    public <T> CompletionStage<T> withTransaction(Function<Connection, T> transactionFunction) {
        return CompletableFuture.supplyAsync(() -> {
            return db.withTransaction(conn -> {
                try {
                    return transactionFunction.apply(conn);
                } catch (Exception e) {
                    LOGGER.severe("Transaction failed: " + e.getMessage());
                    throw new AppDataAccessException("Transaction execution failed", e);
                }
            });
        });
    }

    // Builder for stored procedure calls (single or batch)
    public class ProcedureCall {
        private final String procedureName;
        private final List<List<Object>> batchParameters = new ArrayList<>(); // For batch processing
        private final Map<Integer, Integer> outParameters = new HashMap<>();
        private List<Object> singleParameters = null; // For single execution
        private Connection connection; // For transaction context

        private ProcedureCall(String procedureName) {
            this.procedureName = procedureName;
        }

        private ProcedureCall(String procedureName, Connection connection) {
            this.procedureName = procedureName;
            this.connection = connection;
        }

        // Add single parameter set for non-batch execution
        public ProcedureCall param(Object value) {
            if (singleParameters == null) {
                singleParameters = new ArrayList<>();
            }
            singleParameters.add(value);
            return this;
        }

        // Add parameter set for batch execution
        public ProcedureCall addBatch(Object... values) {
            batchParameters.add(List.of(values));
            singleParameters = null; // Invalidate single execution
            return this;
        }

        // Register output parameter by position and SQL type
        public ProcedureCall outParam(int position, int sqlType) {
            outParameters.put(position, sqlType);
            return this;
        }

        // Execute within transaction context (blocking)
        public <T> T execute(Function<ResultSet, T> mapper) throws SQLException {
            if (connection == null) {
                throw new IllegalStateException("Transaction context required. Use call() with connection parameter.");
            }
            if (singleParameters == null && batchParameters.isEmpty()) {
                throw new IllegalStateException("No parameters provided for procedure call");
            }
            if (!batchParameters.isEmpty()) {
                throw new UnsupportedOperationException("Batch execution not supported in transaction context");
            }

            try (CallableStatement stmt = prepareStatement(connection, singleParameters)) {
                boolean hasResultSet = stmt.execute();
                if (hasResultSet) {
                    return mapper.apply(stmt.getResultSet());
                }
                throw new SQLException("No result set returned");
            }
        }

        // Execute without result mapping within transaction context
        public void execute() throws SQLException {
            if (connection == null) {
                throw new IllegalStateException("Transaction context required. Use call() with connection parameter.");
            }
            if (singleParameters == null && batchParameters.isEmpty()) {
                throw new IllegalStateException("No parameters provided for procedure call");
            }
            if (!batchParameters.isEmpty()) {
                throw new UnsupportedOperationException("Batch execution not supported in transaction context");
            }

            try (CallableStatement stmt = prepareStatement(connection, singleParameters)) {
                stmt.execute();
            }
        }

        // Execute single procedure call (async - non-transactional)
        public CompletionStage<Void> executeAsync() {
            if (singleParameters == null && batchParameters.isEmpty()) {
                throw new IllegalStateException("No parameters provided for procedure call");
            }
            if (!batchParameters.isEmpty()) {
                return executeBatchAsync();
            }
            return CompletableFuture.runAsync(() -> {
                try {
                    db.withConnection(conn -> {
                        try (CallableStatement stmt = prepareStatement(conn, singleParameters)) {
                            stmt.execute();
                        }
                    });
                } catch (Exception e) {
                    LOGGER.severe("Failed to execute procedure " + procedureName + ": " + e.getMessage());
                    throw new AppDataAccessException("Procedure execution failed", e);
                }
            });
        }

        // Execute batch procedure calls (async - non-transactional)
        public CompletionStage<Void> executeBatchAsync() {
            if (batchParameters.isEmpty()) {
                throw new IllegalStateException("No batch parameters provided for procedure call");
            }
            return CompletableFuture.runAsync(() -> {
                try {
                    db.withConnection(conn -> {
                        conn.setAutoCommit(false); // Start transaction
                        try (CallableStatement stmt = prepareBatchStatement(conn)) {
                            for (List<Object> params : batchParameters) {
                                setParameters(stmt, params);
                                stmt.addBatch();
                            }
                            stmt.executeBatch();
                            conn.commit(); // Commit transaction
                        } catch (SQLException e) {
                            conn.rollback(); // Roll back on error
                            throw e;
                        } finally {
                            conn.setAutoCommit(true); // Restore default
                        }
                    });
                } catch (Exception e) {
                    LOGGER.severe("Failed to execute batch procedure " + procedureName + ": " + e.getMessage());
                    throw new AppDataAccessException("Batch procedure execution failed", e);
                }
            });
        }

        // Execute single procedure and map result set (async - non-transactional)
        public <T> CompletionStage<T> queryAsync(Function<ResultSet, T> mapper) {
            if (singleParameters == null) {
                throw new IllegalStateException("Query requires single parameter set, use executeBatch for batch");
            }

            // Store parameters in a final variable for use in the lambda
            final List<Object> params = new ArrayList<>(singleParameters);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    return db.withConnection(conn -> {
                        try (CallableStatement stmt = prepareStatement(conn, params)) {
                            boolean hasResultSet = stmt.execute();
                            if (hasResultSet) {
                                return mapper.apply(stmt.getResultSet());
                            }
                            throw new SQLException("No result set returned");
                        }
                    });
                } catch (Exception e) {
                    LOGGER.severe("Failed to query procedure " + procedureName + ": " + e.getMessage());
                    throw new AppDataAccessException("Procedure query failed", e);
                }
            });
        }

        public <T> T query(Function<ResultSet, T> mapper) {
            if (singleParameters == null) {
                throw new IllegalStateException("Query requires single parameter set, use executeBatch for batch");
            }

            // Store parameters in a final variable for use in the try block
            final List<Object> params = new ArrayList<>(singleParameters);

            try {
                return db.withConnection(conn -> {
                    try (CallableStatement stmt = prepareStatement(conn, params)) {
                        boolean hasResultSet = stmt.execute();
                        if (hasResultSet) {
                            return mapper.apply(stmt.getResultSet());
                        }
                        throw new SQLException("No result set returned");
                    }
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to query procedure " + procedureName + ": " + e.getMessage());
                throw new AppDataAccessException("Procedure query failed", e);
            }
        }


        // Execute single procedure and return output parameters (async - non-transactional)
        public <T> CompletionStage<T> executeForOutputAsync(int paramIndex, Class<T> type) {
            if (singleParameters == null) {
                throw new IllegalStateException(
                        "Output parameters require single execution, use executeBatch for batch"
                );
            }

            // Store parameters in a final variable for use in the lambda
            final List<Object> params = new ArrayList<>(singleParameters);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    return db.withConnection(conn -> {
                        try (CallableStatement stmt = prepareStatement(conn, params)) {
                            stmt.execute();

                            Object raw = stmt.getObject(paramIndex);
                            return type.cast(raw);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.severe("Failed to execute procedure " + procedureName + ": " + e.getMessage());
                    throw new AppDataAccessException("Procedure execution failed", e);
                }
            });
        }
        private CallableStatement prepareStatement(Connection conn, List<Object> params) throws SQLException {
            String placeholders = params.stream().map(p -> "?").collect(Collectors.joining(","));
            String outPlaceholders = outParameters.keySet().stream()
                    .map(p -> "?")
                    .collect(Collectors.joining(","));
            String callSql = "{call " + procedureName + "(" +
                    (placeholders.isEmpty() ? "" : placeholders) +
                    (outPlaceholders.isEmpty() ? "" : (placeholders.isEmpty() ? "" : ",") + outPlaceholders) + ")}";

            CallableStatement stmt = conn.prepareCall(callSql);
            setParameters(stmt, params);
            for (Map.Entry<Integer, Integer> outParam : outParameters.entrySet()) {
                stmt.registerOutParameter(outParam.getKey(), outParam.getValue());
            }
            return stmt;
        }


        private CallableStatement prepareBatchStatement(Connection conn) throws SQLException {
            String placeholders = batchParameters.get(0).stream()
                    .map(p -> "?")
                    .collect(Collectors.joining(","));
            String callSql = "{call " + procedureName + "(" + placeholders + ")}";
            return conn.prepareCall(callSql);
        }

        private void setParameters(CallableStatement stmt, List<Object> params) throws SQLException {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) param);
                } else if (param == null) {
                    stmt.setNull(i + 1, java.sql.Types.NULL);
                } else {
                    stmt.setObject(i + 1, param);
                }
            }
        }
    }

    // Start a stored procedure call within transaction context
    public ProcedureCall call(String procedureName, Connection connection) {
        return new ProcedureCall(procedureName, connection);
    }

    // Start a stored procedure call (non-transactional)
    public ProcedureCall call(String procedureName) {
        return new ProcedureCall(procedureName);
    }

    public QueryCall sql(String sql) {
        return new QueryCall(sql);
    }

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

//        public <T> CompletionStage<T> query(Function<ResultSet, T> mapper) {
//            return CompletableFuture.supplyAsync(() -> {
//                try {
//                    return db.withConnection(conn -> {
//                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
//                            for (int i = 0; i < parameters.size(); i++) {
//                                stmt.setObject(i + 1, parameters.get(i));
//                            }
//                            return mapper.apply(stmt.executeQuery());
//                        }
//                    });
//                } catch (Exception e) {
//                    LOGGER.severe("Failed to execute query: " + e.getMessage());
//                    throw new AppDataAccessException("Query failed", e);
//                }
//            });
//        }

        public <T> CompletionStage<T> queryAsync(Function<ResultSet, T> mapper) {
            // Store parameters in final variables for use in the lambda
            final String finalSql = sql;
            final List<Object> finalParams = new ArrayList<>(parameters);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    return db.withConnection(conn -> {
                        try (PreparedStatement stmt = conn.prepareStatement(finalSql)) {
                            for (int i = 0; i < finalParams.size(); i++) {
                                stmt.setObject(i + 1, finalParams.get(i));
                            }
                            return mapper.apply(stmt.executeQuery());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.severe("Failed to execute query: " + e.getMessage());
                    throw new AppDataAccessException("Query failed", e);
                }
            });
        }

        public <T> T query(Function<ResultSet, T> mapper) {
            try {
                return db.withConnection(conn -> {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        for (int i = 0; i < parameters.size(); i++) {
                            stmt.setObject(i + 1, parameters.get(i));
                        }
                        return mapper.apply(stmt.executeQuery());
                    }
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to execute query: " + e.getMessage());
                throw new AppDataAccessException("Query failed", e);
            }
        }

    }


}
