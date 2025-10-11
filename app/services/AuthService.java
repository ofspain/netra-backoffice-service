package services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.CustomerUser;
import com.netra.commons.util.BasicUtil;
import controllers.routes;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import services.db.JdbcWrapper;
import utilities.components.IdentityServiceSettings;
import utilities.dto.AuthResponse;
import utilities.dto.LoginOutcome;
import utilities.dto.LoginResult;
import utilities.rest.RestClientService;

import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.time.format.DateTimeFormatter;



@Singleton
public class AuthService {
    private final JdbcWrapper jdbcClient;
    private final RestClientService restClientService;

    private final IdentityServiceSettings authrexSettings;
    private final CustomerUserService customerUserService;


    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;



    @Inject
    public AuthService(JdbcWrapper jdbcClient, RestClientService restClientService,
                       IdentityServiceSettings authrexSettings, CustomerUserService customerUserService){
        this.jdbcClient = jdbcClient;
        this.restClientService = restClientService;
        this.authrexSettings = authrexSettings;
        this.customerUserService = customerUserService;
    }

    public AuthResponse refreshToken(String refreshUUID){
        return null;
    }

    public LoginOutcome processLogin(String username, String password){
        Map<String,String> loginRequest = Map.of("username",username, "password", password );

        String fullPath = authrexSettings.getAuthrexBaseUrl() + authrexSettings.getLoginPath();
        var response = restClientService.post(fullPath, loginRequest).toCompletableFuture().join();

        try {
            System.out.println("REPONSE "+new ObjectMapper().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to login " +
                    response.getStatusCode() + " - " + response.getStatusText());
        }


        AuthResponse authResponse = response.getBodyDataAs(AuthResponse.class, "data");


        // Decode the JWT to extract info (issuer, subject, roles, etc.)
        DecodedJWT jwt = JWT.decode(authResponse.getAccessToken());

        // Extract claims
        String usernameFromToken = jwt.getSubject();
        String issuer = jwt.getIssuer();
        String domainCode = jwt.getClaim("domain_code").asString();
        String domainType = jwt.getClaim("domain_type").asString();
        String identityUuid = jwt.getClaim("identity_uuid").asString();
        String lastLoginRaw = jwt.getClaim("last_login").asString();
        String lastPasswordChangeRaw = jwt.getClaim("last_password_change").asString();
        List<String> roles = jwt.getClaim("roles").asList(String.class);

        LocalDateTime lastLogin = (lastLoginRaw != null) ? LocalDateTime.parse(lastLoginRaw, DATE_FORMATTER) : null;
        LocalDateTime lastPasswordChange = (lastPasswordChangeRaw != null) ? LocalDateTime.parse(lastPasswordChangeRaw, DATE_FORMATTER) : null;

        // Return enriched response
        LoginResult loginResult = new LoginResult(
                authResponse.getAccessToken(),
                authResponse.getTokenType(),
                authResponse.getExpiresIn(),
                authResponse.getRefreshToken(),
                usernameFromToken,
                issuer,
                domainCode,
                DomainType.valueOf(domainType),
                identityUuid,
                roles,
                lastLogin,
                lastPasswordChange
        );

        // Determine navigation path
        Call redirectPath = determineRedirect(domainType, identityUuid);

        return new LoginOutcome(loginResult, redirectPath);

    }



    private Call determineRedirect(String domainTypeStr, String identityUUID) {
        if (domainTypeStr == null || identityUUID == null) {
            return routes.AuthController.logout();
        }

        DomainType domainType;
        try {
            domainType = DomainType.valueOf(domainTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Unknown or invalid domain type
            return routes.AuthController.logout();
        }

        return switch (domainType) {
            case CUSTOMER -> {
                CustomerUser customerUser = customerUserService.findCustomerUser("identity_uuid", identityUUID);
                if (customerUser == null) {
                    yield routes.AuthController.logout();
                }
                String encodedId = BasicUtil.encodeUrlBoundId(customerUser.getId());
                yield controllers.customer.user.routes.CustomerUserDashboard.index(encodedId);
            }

            default -> routes.AuthController.logout();
        };
    }

}
