package security;

import com.netra.commons.util.BasicUtil;
import com.netra.commons.util.Constants;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.Configuration;
import play.mvc.Http;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class SecurityConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);
    private final Config config;

    private final String[] WHITE_LISTED_PATH = {"health"};

    @Inject
    public SecurityConfig() {
        //test $env:JWT_SECRET = "your-very-long-secret-key-at-least-32-char-your-very-long-secret-key-at-least-32-char"
        // JWT secret (HMAC-SHA256, 256-bit minimum)
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET environment variable must be at least 32 bytes");
        }

        // Configure JWT authenticator with custom validation
        SecretSignatureConfiguration signatureConfig = new SecretSignatureConfiguration(jwtSecret);
        JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(signatureConfig);

        // Set custom expiration time if needed (e.g., 1 hour)
        // jwtAuthenticator.setExpirationTime(new Date(System.currentTimeMillis() + 3600000));

        // Create a custom authenticator that wraps the JWT authenticator and adds claim validation
        Authenticator customAuthenticator = (ctx, credentials) -> {
            String path = ctx.webContext().getPath();
            System.out.println("Path visited>>>>> "+path);
            if (Arrays.asList(WHITE_LISTED_PATH).contains(path)) {
                LOGGER.debug("Whitelisted path {}, skipping JWT validation", path);
                return Optional.of(credentials); // short-circuit success
            }
            TokenCredentials tokenCredentials = (TokenCredentials) credentials;

            try {
                // First, let the JWT authenticator do its job (signature verification, expiration check)
                Optional<Credentials> result = jwtAuthenticator.validate(ctx, credentials);

                if (result.isPresent()) {
                    UserProfile profile = tokenCredentials.getUserProfile();
                    if (profile != null) {
                        // Additional custom claim validation
                        validateCustomClaims(profile);
                    }
                }

                return result;
            } catch (CredentialsException e) {
                LOGGER.error("JWT validation failed: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                LOGGER.error("Unexpected error during JWT validation: {}", e.getMessage());
                throw new CredentialsException("Invalid token", e);
            }
        };

        // Configure HeaderClient for multiple headers
        HeaderClient headerClient = new HeaderClient();
        headerClient.setHeaderName("Authorization");
        headerClient.setPrefixHeader("Bearer ");
        headerClient.setAuthenticator(customAuthenticator);

        // Custom token extractor for multiple headers
        headerClient.setCredentialsExtractor(ctx -> {
            // Try Authorization header first
            String authHeader = ctx.webContext().getRequestHeader(Constants.REQUEST_AUTH_BEARER_KEY).orElse("");
            if (authHeader.startsWith("Bearer ")) {
                return Optional.of(new TokenCredentials(authHeader.substring(7)));
            }else{
                //todo: throw relevant exception here
            }

            // Fallback to X-Authorization
            String customHeader = ctx.webContext().getRequestHeader(Constants.REQUEST_AUTH_DOMAIN_X_KEY).orElse("");
            if (BasicUtil.validString(customHeader)) {
                //todo: do shennanigans domain validation here eg: domain in header == domain in body
            }else{
                //todo: throw relevant exception here too
            }

            return Optional.empty();
        });

        // Pac4j config
        config = new Config(headerClient);

        // For stateless applications, you might want to use a null session store
        // But Play Framework typically uses session stores
        config.setSessionStoreFactory(parameters -> new org.pac4j.play.store.PlayCookieSessionStore());

        // Role-based authorizers
        config.addAuthorizer("admin", new RoleAuthorizer("ADMIN"));
        config.addAuthorizer("user", new RoleAuthorizer("USER"));
        config.addAuthorizer("anyRole", new AnyRoleAuthorizer());
    }

    private void validateCustomClaims(UserProfile profile) {
        // Validate issuer
        String issuer = Optional.ofNullable(profile.getAttribute("iss"))
                .map(Object::toString)
                .orElse(null);

        //todo: change to domain name of auth service
        if (!"your-auth-service".equals(issuer)) {
            throw new CredentialsException("Invalid issuer: " + issuer);
        }

        // Validate custom claim
        String customClaim = (String) profile.getAttribute("custom_claim");
        if (customClaim == null || !customClaim.matches("^[a-zA-Z0-9]+$")) {
            throw new CredentialsException("Invalid or missing custom_claim");
        }

        // Validate roles
        Set<String> roles = profile.getRoles();
        if (roles == null || roles.isEmpty()) {
            throw new CredentialsException("Missing roles claim");
        }

        // You can add more custom validations here
    }

    public Config getConfig() {
        return config;
    }

    // Role-based authorizer for specific role
    public static class RoleAuthorizer implements Authorizer {
        private final String requiredRole;

        public RoleAuthorizer(String requiredRole) {
            this.requiredRole = requiredRole;
        }

        @Override
        public boolean isAuthorized(WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
            if (CommonHelper.isEmpty(profiles)) {
                return false;
            }

            boolean authorized = profiles.stream()
                    .anyMatch(profile -> profile.getRoles() != null &&
                            profile.getRoles().contains(requiredRole));

            if (!authorized) {
                LOGGER.warn("Unauthorized access attempt to {} resource by {}",
                        requiredRole,
                        profiles.stream().map(UserProfile::getId).collect(Collectors.joining(",")));
            }

            return authorized;
        }
    }

    // Authorizer that requires any role (user must be authenticated with at least one role)
    public static class AnyRoleAuthorizer implements Authorizer {
        @Override
        public boolean isAuthorized(WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
            if (CommonHelper.isEmpty(profiles)) {
                return false;
            }

            return profiles.stream()
                    .anyMatch(profile -> profile.getRoles() != null && !profile.getRoles().isEmpty());
        }
    }
}