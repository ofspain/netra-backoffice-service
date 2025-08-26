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
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.netra.commons.exceptions.AppDataAccessException;
import play.db.Database;
import scalas.services.FlywayInitializer;

@Singleton
public class JdbcWrapper {

    private static final Logger LOGGER = Logger.getLogger(JdbcWrapper.class.getName());
    private final Database db;

    @Inject
    public JdbcWrapper(Database db, FlywayInitializer flywayInitializer) {
        this.db = db;
    }

    // Builder for stored procedure calls (single or batch)
    public class ProcedureCall {
        private final String procedureName;
        private final List<List<Object>> batchParameters = new ArrayList<>(); // For batch processing
        private final Map<Integer, Integer> outParameters = new HashMap<>();
        private List<Object> singleParameters = null; // For single execution

        private ProcedureCall(String procedureName) {
            this.procedureName = procedureName;
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

        // Execute single procedure call
        public CompletionStage<Void> execute() {
            if (singleParameters == null && batchParameters.isEmpty()) {
                throw new IllegalStateException("No parameters provided for procedure call");
            }
            if (!batchParameters.isEmpty()) {
                return executeBatch();
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

        // Execute batch procedure calls
        public CompletionStage<Void> executeBatch() {
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

        // Execute single procedure and map result set
        public <T> CompletionStage<T> query(Function<ResultSet, T> mapper) {
            if (singleParameters == null) {
                throw new IllegalStateException("Query requires single parameter set, use executeBatch for batch");
            }
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return db.withConnection(conn -> {
                        try (CallableStatement stmt = prepareStatement(conn, singleParameters)) {
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

        // Execute single procedure and return output parameters
        public <T> CompletionStage<T> executeForOutput(int paramIndex, Class<T> type) {
            if (singleParameters == null) {
                throw new IllegalStateException(
                        "Output parameters require single execution, use executeBatch for batch"
                );
            }
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return db.withConnection(conn -> {
                        try (CallableStatement stmt = prepareStatement(conn, singleParameters)) {
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
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param == null) {
                    stmt.setNull(i + 1, java.sql.Types.NULL);
                } else {
                    stmt.setObject(i + 1, param);
                }
            }
        }
    }


    // Start a stored procedure call
    public ProcedureCall call(String procedureName) {
        return new ProcedureCall(procedureName);
    }


    public QueryCall sql(String sql) {
        return new QueryCall(sql);
    }


    public class QueryCall {
        private final String sql;
        private final List<Object> parameters = new ArrayList<>();

        private static final Logger LOGGER = Logger.getLogger(JdbcWrapper.class.getName());


        private QueryCall(String sql) {
            this.sql = sql;
        }

        public QueryCall param(Object value) {
            parameters.add(value);
            return this;
        }

        public <T> CompletionStage<T> query(Function<ResultSet, T> mapper) {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        }
    }
}
