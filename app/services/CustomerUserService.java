package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netra.commons.exceptions.AppDataAccessException;
import com.netra.commons.models.CustomerUser;
import com.netra.commons.models.Identity;
import com.netra.commons.requests.CreateIdentityRequest;
import play.libs.Json;
import services.db.JdbcWrapper;
import services.db.ResultSetToBeanMapper;
import utilities.PaginatedResult;
import utilities.components.IdentityServiceSettings;
import utilities.rest.RestClientService;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import static utilities.MapperUtil.toJsonb;


@Singleton
public class CustomerUserService {

    private final JdbcWrapper jdbcClient;
    private final IdentityServiceSettings authrexSettings;

    private final RestClientService restClientService;


    @Inject
    public CustomerUserService(JdbcWrapper jdbcClient, IdentityServiceSettings authrexSettings, RestClientService restClientService){

        this.jdbcClient = jdbcClient;
        this.authrexSettings = authrexSettings;
        this.restClientService = restClientService;
    }


    public CompletionStage<PaginatedResult<CustomerUser>> findAll(
            int limit, int offset, String sortBy, String sortDir) {
        System.out.println("finding all user....");

        return jdbcClient.sql("SELECT * FROM find_all_customer_users(?, ?, ?, ?)")
                .param(limit)
                .param(offset)
                .param(sortBy)
                .param(sortDir)
                .queryAsync(rs -> ResultSetToBeanMapper.mapToCustomerUser(rs, limit, offset));
    }

    public CompletionStage<CustomerUser> saveCustomerUser(CustomerUser user, CreateIdentityRequest identityRequest, String prefix) {
        try {
            String fullPath = authrexSettings.getAuthrexBaseUrl() + authrexSettings.getCustomerUserRegistrationPath();
            var response = restClientService.post(fullPath, identityRequest).toCompletableFuture().join();

            System.out.println("REPONSE "+new ObjectMapper().writeValueAsString(response));

            if (!response.isSuccess()) {
                throw new IllegalStateException("Failed to save user identity " +
                        response.getStatusCode() + " - " + response.getStatusText());
            }


            Identity createdIdentity = response.getBodyDataAs(Identity.class, "data");

            System.out.println("created identity: "+Json.mapper().writeValueAsString(createdIdentity));

            System.out.println("✅ Registered identity UUID: " + createdIdentity.getIdentityUuid());


            CompletionStage<CustomerUser> result =
                    jdbcClient.call("upsert_customer_user")
                            .param(user.getId())
                            .param(user.getName())
                            .param(user.getDisabled())
                            .param(user.getUserPhone())
                            .param(user.getUserEmail())
                            .param(toJsonb(user.getAccounts()))
                            .param(createdIdentity.getIdentityUuid())
                            .queryAsync(rs -> {
                                try {
                                    if (rs.next()) {
                                        return ResultSetToBeanMapper.mapToCustomerUser(rs, prefix);
                                    }
                                    return user;
                                } catch (SQLException e) {
                                    throw new AppDataAccessException("Financial institution upsert failed", e);
                                }
                            });

            return result;

        } catch (Exception e) {
            throw new AppDataAccessException("Transaction failed while processing a db action", e);
        }
    }


    public CustomerUser findCustomerUser(String column, Object value){


        String sql = formUniqueColumnSearchSQL(column);

        CustomerUser customerUser = jdbcClient.sql(sql)
                .param(value)
                .query(rs -> {
                    try {
                        if (rs.next()) {

                            return ResultSetToBeanMapper.mapToCustomerUser(rs, "");
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });//.toCompletableFuture().join()


        return customerUser;

    }

    private String formUniqueColumnSearchSQL(String column){

        // Validate column name before using it to prevent SQL injection
        if (!List.of("id", "user_phone", "identity_uuid").contains(column)) {
            throw new IllegalArgumentException("Invalid search key: " + column);
        }


        String sql = "SELECT * FROM customer_users WHERE " + column + " = ?";

        return sql;
    }



}
