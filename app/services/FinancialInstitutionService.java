package services;


import com.netra.commons.database.EnhancedBeanPropertyRowMapper;
import com.netra.commons.exceptions.AppDataAccessException;
import com.netra.commons.models.EndpointConfig;
import com.netra.commons.models.FinancialInstitution;
import services.db.ResultSetToBeanMapper;
import services.db.JdbcWrapper;
import utilities.PaginatedResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CompletionStage;


@Singleton
public class FinancialInstitutionService {

    private final JdbcWrapper jdbcClient;
    private final EndpointConfigService endpointConfigService;

    @Inject
    public FinancialInstitutionService(JdbcWrapper jdbcClient, EndpointConfigService endpointConfigService){

        this.jdbcClient = jdbcClient;
        this.endpointConfigService = endpointConfigService;
    }


    public Boolean findFinancialInstitutionExistenceByUniqueKey(String column, String value){

        String sql = formUniqueColumnSearchSQL(column);

        Long countObject = jdbcClient.sql(sql)
                .param(column)
                .param(value)
                .query(rs -> {
                    try {
                        return rs.next() ? rs.getLong("count_object") : null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }); //.toCompletableFuture().join()

        return null != countObject && countObject > 0;
    }

    private String formUniqueColumnSearchSQL(String column){

        // Validate column name before using it to prevent SQL injection
        if (!List.of("id", "domain_code", "code").contains(column)) {
            throw new IllegalArgumentException("Invalid search key: " + column);
        }


        String sql = "SELECT * FROM financial_institutions WHERE " + column + " = ?";

        return sql;
    }
    public FinancialInstitution findMinimalFinancialInstitutionByUniqueKey(String column, String value){


        String sql = formUniqueColumnSearchSQL(column);

        FinancialInstitution financialInstitution = jdbcClient.sql(sql)
                .param(value)
                .query(rs -> {
                    try {
                        if (rs.next()) {
                            // Use your EnhancedBeanPropertyRowMapper
                            return new EnhancedBeanPropertyRowMapper<FinancialInstitution>().mapRow(rs, 1);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });//.toCompletableFuture().join()


        return financialInstitution;

    }


    // Find single by ID
    public CompletionStage<FinancialInstitution> findById(Long id) {

        return jdbcClient.call("find_financial_institution_by_id")
                .param(id)
                .queryAsync(rs -> {
                    try {
                        if (rs.next()) {
                            return ResultSetToBeanMapper.mapToFinancialInstitution(rs);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        throw new AppDataAccessException(e.getMessage());
                    }
                    return null;
                });
    }


    // Find by criteria with custom mapping
    public CompletionStage<List<FinancialInstitution>> findByCriteria(String namePattern, Boolean activeOnly) {
        return jdbcClient.call("find_financial_institutions_by_criteria")
                .param(namePattern)
                .param(activeOnly)
                .queryAsync(rs -> ResultSetToBeanMapper.mapToFinancialInstitutions(rs, this::enrichWithAdditionalData));
    }

    // Transactional save with endpoint
    public CompletionStage<FinancialInstitution> saveFinancialInstitutionWithEndpoint(FinancialInstitution fi) {
        EndpointConfig endpointConfig = fi.getEndpointConfig();
        if(null == endpointConfig){
            throw new IllegalArgumentException("EndpointConfig can not be null at this point");
        }
        return jdbcClient.withTransaction(connection -> {
            try {
                // Upsert endpoint config - return the input object if no result set
                EndpointConfig savedEndpoint = jdbcClient.call("upsert_endpoint_config", connection)
                        .param(endpointConfig.getId())
                        .param(endpointConfig.getDomainCode())
                        .param(endpointConfig.getDomainType().toString())
                        //todo ... other endpoint params
                        .execute(rs -> {
                            try {
                                if (rs.next()) {
                                    return ResultSetToBeanMapper.mapResultSetToEndpointConfig(rs);
                                }
                                // If no result set, return the original input
                                return endpointConfig;
                            } catch (SQLException e) {
                                throw new AppDataAccessException("Endpoint config upsert failed", e);
                            }
                        });

                // Upsert financial institution - return input if no result set
                FinancialInstitution savedFI = jdbcClient.call("upsert_financial_institution", connection)
                        .param(fi.getId())
                        .param(fi.getName())
                        .param(fi.getCode())
                        .param(fi.getDomainCode())
                        .param(fi.getDisabled())
                        .param(fi.getLogoKey())
                        .param(savedEndpoint.getId())
                        .param(true)
                        .param(true)
                        .execute(rs -> {
                            try {
                                if (rs.next()) {
                                    return ResultSetToBeanMapper.mapToFinancialInstitution(rs);
                                }
                                // If no result set, return the original input with updated endpoint reference
                                fi.setEndpointConfig(savedEndpoint);
                                return fi;
                            } catch (SQLException e) {
                                throw new AppDataAccessException("Financial institution upsert failed", e);
                            }
                        });

                savedFI.setEndpointConfig(savedEndpoint);
                return savedFI;

            } catch (SQLException e) {
                throw new AppDataAccessException("Transaction failed", e);
            }
        });
    }


    public CompletionStage<FinancialInstitution> saveFinancialInstitutionWithEndpoint_(FinancialInstitution fi) {
        EndpointConfig endpointConfig = fi.getEndpointConfig();
        if (endpointConfig == null) {
            throw new IllegalArgumentException("EndpointConfig cannot be null at this point");
        }

        return jdbcClient.withConditionalTransaction(
                connection -> {
                    try {
                        // 1. Upsert endpoint config
                        EndpointConfig savedEndpoint = jdbcClient.call("upsert_endpoint_config", connection)
                                .param(endpointConfig.getId())
                                .param(endpointConfig.getDomainCode())
                                .param(endpointConfig.getDomainType().toString())
                                // ... other endpoint params
                                .execute(rs -> {
                                    try {
                                        if (rs.next()) {
                                            return ResultSetToBeanMapper.mapResultSetToEndpointConfig(rs);
                                        }
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        throw new RuntimeException(e);
                                    }
                                    return endpointConfig; // no result → return input
                                });

                        // 2. Upsert financial institution
                        FinancialInstitution savedFI = jdbcClient.call("upsert_financial_institution", connection)
                                .param(fi.getId())
                                .param(fi.getName())
                                .param(fi.getCode())
                                .param(fi.getDomainCode())
                                .param(fi.getDisabled())
                                .param(fi.getLogoKey())
                                .param(savedEndpoint.getId())
                                .param(true)
                                .param(true)
                                .execute(rs -> {
                                    try {
                                        if (rs.next()) {
                                            return ResultSetToBeanMapper.mapToFinancialInstitution(rs);
                                        }
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        throw new RuntimeException(e);
                                    }
                                    fi.setEndpointConfig(savedEndpoint);
                                    return fi; // no result → return input
                                });

                        savedFI.setEndpointConfig(savedEndpoint);
                        return savedFI;

                    } catch (SQLException e) {
                        e.printStackTrace();
                        throw new AppDataAccessException("Transaction failed", e);
                    }
                },
                ex -> ex instanceof SQLException      // rollback on SQL problems
                        || ex instanceof AppDataAccessException // rollback on domain problems
        );
    }

    // Helper method to resolve endpoint config
    private EndpointConfig resolveEndpointConfig(Long endpointConfigId) {
        try {
            return endpointConfigService.findById(endpointConfigId);
        } catch (Exception e) {
            // Return minimal endpoint config if resolution fails
            EndpointConfig minimal = new EndpointConfig();
            minimal.setId(endpointConfigId);
            return minimal;
        }
    }

    // Get financial institution with full endpoint config
    public CompletionStage<FinancialInstitution> getWithEndpointConfig(Long id) {
        return jdbcClient.call("get_financial_institution_with_endpoint")
                .param(id)
                .queryAsync(this::mapToFinancialInstitutionWithEndpoint);
    }




    // Synchronous version
    public FinancialInstitution getWithEndpointConfigSync(Long id) {
        return jdbcClient.call("get_financial_institution_with_endpoint")
                .param(id)
                .query(this::mapToFinancialInstitutionWithEndpoint);
    }


    // Find all with pagination
    public CompletionStage<PaginatedResult<FinancialInstitution>> findAll(
            int limit, int offset, String sortBy, String sortDir) {

        return jdbcClient.call("find_all_financial_institutions")
                .param(limit)
                .param(offset)
                .param(sortBy)
                .param(sortDir)
                .queryAsync(rs -> ResultSetToBeanMapper.mapToFinancialInstitutionsPaginated(rs, limit, offset));
    }

    // Find all with endpoints and pagination
    public CompletionStage<PaginatedResult<FinancialInstitution>> findAllWithEndpoints(
            int limit, int offset, String sortBy, String sortDir) {

        return jdbcClient.call("find_all_financial_institutions_with_endpoints")
                .param(limit)
                .param(offset)
                .param(sortBy)
                .param(sortDir)
                .queryAsync(rs -> ResultSetToBeanMapper.mapToFinancialInstitutionsWithEndpointsPaginated(rs, limit, offset));
    }

    // Mapper method for the combined result
    private FinancialInstitution mapToFinancialInstitutionWithEndpoint(ResultSet rs) {
        try {
            if (rs.next()) {
                return ResultSetToBeanMapper.mapToFinancialInstitutionWithEndpoint(rs);
            } else {
                throw new AppDataAccessException("Financial institution not found");
            }
        } catch (Exception e) {
            throw new AppDataAccessException("Failed to map financial institution with endpoint", e);
        }
    }


    // Custom enrichment function
    private FinancialInstitution enrichWithAdditionalData(FinancialInstitution fi) {
        // Add any additional data or transformations here
      //  fi.setAdditionalMetadata("enriched_data");
        return fi;
    }


}
