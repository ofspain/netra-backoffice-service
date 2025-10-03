package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netra.commons.exceptions.AppDataAccessException;
import com.netra.commons.models.endpoint.EndpointConfig;
import services.db.JdbcWrapper;
import services.db.ResultSetToBeanMapper;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static utilities.MapperUtil.toJsonb;

public class EndpointConfigService {

    private final JdbcWrapper jdbcWrapper;

    @Inject
    public EndpointConfigService(JdbcWrapper jdbcWrapper) {
        this.jdbcWrapper = jdbcWrapper;
    }

    // 1. Find by ID - Use mapper instance
    public EndpointConfig findById(Long id) {
        return jdbcWrapper.call("find_endpoint_config_by_id")
                .param(id)
                .query(ResultSetToBeanMapper::mapResultSetToEndpointConfig); // Method reference
    }

    // 1b. Async version of findById
    public CompletionStage<EndpointConfig> findByIdAsync(Long id) {
        return jdbcWrapper.call("find_endpoint_config_by_id")
                .param(id)
                .queryAsync(ResultSetToBeanMapper::mapResultSetToEndpointConfig); // Method reference
    }

    // 2. Search by domain code and type
    public List<EndpointConfig> findByDomain(String domainCode, String domainType, int limit, int offset) {
        return jdbcWrapper.call("find_endpoint_configs_by_domain")
                .param(domainCode)
                .param(domainType)
                .param(limit)
                .param(offset)
                .query(rs -> {
                    List<EndpointConfig> results = new ArrayList<>();
                    while (true) {
                        try {
                            if (!rs.next()) break;
                        } catch (SQLException e) {
                            throw new AppDataAccessException(e.getMessage());
                        }
                        results.add(ResultSetToBeanMapper.mapResultSetToEndpointConfig(rs)); // Use mapper instance
                    }
                    return results;
                });
    }

    // 2b. Async version of findByDomain
    public CompletionStage<List<EndpointConfig>> findByDomainAsync(String domainCode, String domainType, int limit, int offset) {
        return jdbcWrapper.call("find_endpoint_configs_by_domain")
                .param(domainCode)
                .param(domainType)
                .param(limit)
                .param(offset)
                .queryAsync(rs -> {
                    List<EndpointConfig> results = new ArrayList<>();
                    while (true) {
                        try {
                            if (!rs.next()) break;
                        } catch (SQLException e) {
                            throw new AppDataAccessException(e.getMessage());
                        }
                        results.add(ResultSetToBeanMapper.mapResultSetToEndpointConfig(rs)); // Use mapper instance
                    }
                    return results;
                });
    }

    // 3. Advanced search
    public SearchResult<EndpointConfig> search(
            String domainCode, String domainType, String baseUrlPattern,
            Boolean requiresAuth, String authType, Boolean useProxy,
            Boolean hasUniqueTransaction, Boolean hasMultipleTransaction,
            int limit, int offset) {

        return jdbcWrapper.call("search_endpoint_configs")
                .param(domainCode)
                .param(domainType)
                .param(baseUrlPattern)
                .param(requiresAuth)
                .param(authType)
                .param(useProxy)
                .param(hasUniqueTransaction)
                .param(hasMultipleTransaction)
                .param(limit)
                .param(offset)
                .query(rs -> {
                    List<EndpointConfig> items = new ArrayList<>();
                    long totalCount = 0;

                    try{
                        while (rs.next()) {
                            items.add(ResultSetToBeanMapper.mapResultSetToEndpointConfig(rs)); // Use mapper instance
                            totalCount = rs.getLong("total_count");
                        }
                    }catch (SQLException ex){
                        throw new AppDataAccessException(ex.getMessage());
                    }

                    return new SearchResult<>(items, totalCount);
                });
    }

    // 3b. Async version of search
    public CompletionStage<SearchResult<EndpointConfig>> searchAsync(
            String domainCode, String domainType, String baseUrlPattern,
            Boolean requiresAuth, String authType, Boolean useProxy,
            Boolean hasUniqueTransaction, Boolean hasMultipleTransaction,
            int limit, int offset) {

        return jdbcWrapper.call("search_endpoint_configs")
                .param(domainCode)
                .param(domainType)
                .param(baseUrlPattern)
                .param(requiresAuth)
                .param(authType)
                .param(useProxy)
                .param(hasUniqueTransaction)
                .param(hasMultipleTransaction)
                .param(limit)
                .param(offset)
                .queryAsync(rs -> {
                    List<EndpointConfig> items = new ArrayList<>();
                    long totalCount = 0;

                    try{
                        while (rs.next()) {
                            items.add(ResultSetToBeanMapper.mapResultSetToEndpointConfig(rs)); // Use mapper instance
                            totalCount = rs.getLong("total_count");
                        }
                    }catch(SQLException exp){
                        throw new AppDataAccessException(exp.getMessage());
                    }

                    return new SearchResult<>(items, totalCount);
                });
    }

    public CompletionStage<EndpointConfig> saveEndpointConfig(EndpointConfig endpointConfig) {
        if (endpointConfig == null) {
            throw new IllegalArgumentException("EndpointConfig cannot be null at this point");
        }

        return jdbcWrapper.withTransaction(connection -> {
            try {
                // Call upsert_endpoint_config
                return jdbcWrapper.call("upsert_endpoint_config", connection)
                        .param(endpointConfig.getId() == null ? null : endpointConfig.getId())       // p_id BIGINT
                        .param(endpointConfig.getDomainOwnerId())                                   // p_domain_owner_id BIGINT
                        .param(endpointConfig.getDomainOwnerType() == null ? null : endpointConfig.getDomainOwnerType().toString()) // p_domain_owner_type VARCHAR
                        .param(endpointConfig.getDomainOwnerCode())                                 // p_domain_owner_code VARCHAR
                        .param(endpointConfig.getDescription())                                     // p_description TEXT
                        .param(toJsonb(endpointConfig.getNetwork()))                                // p_network_config JSONB
                        .param(toJsonb(endpointConfig.getSecurity()))                               // p_security_config JSONB
                        .param(toJsonb(endpointConfig.getEndpoints()))                              // p_endpoints JSONB
                        .param(toJsonb(endpointConfig.getResilience()))                             // p_resilience_config JSONB
                        .param(toJsonb(endpointConfig.getMetadata()))                               // p_metadata JSONB
                        .execute(rs -> {
                            try {
                                if (rs.next()) {
                                    // Map SQL result back into EndpointConfig domain
                                    return ResultSetToBeanMapper.mapResultSetToEndpointConfig(rs);
                                }
                                // If no result, return input object as fallback
                                return endpointConfig;
                            } catch (SQLException e) {
                                throw new AppDataAccessException("EndpointConfig upsert failed", e);
                            }
                        });

            } catch (SQLException e) {
                throw new AppDataAccessException("Transaction failed while processing a db action", e);
            } catch (JsonProcessingException ex) {
                throw new AppDataAccessException("Transaction failed while wrapping value to JSON string", ex);
            }
        });
    }




    // Simple search result container
    public static class SearchResult<T> {
        private final List<T> items;
        private final long totalCount;

        public SearchResult(List<T> items, long totalCount) {
            this.items = items;
            this.totalCount = totalCount;
        }

        public List<T> getItems() { return items; }
        public long getTotalCount() { return totalCount; }
    }
}