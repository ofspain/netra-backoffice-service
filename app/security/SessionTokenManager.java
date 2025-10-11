package security;

import com.netra.commons.util.Constants;
import play.mvc.Http;
import services.AuthService;
import utilities.dto.AuthResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Optional;

/**
 * Centralized helper for managing JWT & refresh tokens inside Play sessions.
 */
@Singleton
public class SessionTokenManager {

    public static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";
    private static final String TOKEN_EXPIRY_KEY = "ACCESS_TOKEN_EXPIRY";
    private static final String USERNAME_KEY = "USERNAME";

    public static final String DOMAIN_CODE_KEY = "x-domain-code";

    private final AuthService authService; // your existing login/refresh service

    @Inject
    public SessionTokenManager(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Saves authentication info into Play's session cookie.
     */
    public Http.Session saveTokens(Http.Session session, AuthResponse authResponse, String username) {
        Instant expiryTime = Instant.now().plusSeconds(authResponse.getExpiresIn());
        return session
                .adding(ACCESS_TOKEN_KEY, authResponse.getAccessToken())
                .adding(REFRESH_TOKEN_KEY, authResponse.getRefreshToken())
                .adding(TOKEN_EXPIRY_KEY, expiryTime.toString())
                .adding(USERNAME_KEY, username)
                .adding(DOMAIN_CODE_KEY, authResponse.getDomainCode())
                .adding(Constants.REQUEST_AUTH_DOMAIN_X_KEY, authResponse.getDomainType().name());
    }

    /**
     * Retrieves the stored access token.
     */
    public Optional<String> getAccessToken(Http.Session session) {
        return session.getOptional(ACCESS_TOKEN_KEY);
    }

    /**
     * Checks if the stored token is expired.
     */
    public boolean isTokenExpired(Http.Session session) {
        Optional<String> expiryRaw = session.getOptional(TOKEN_EXPIRY_KEY);
        if (expiryRaw.isEmpty()) {
            return true;
        }

        try {
            Instant expiryTime = Instant.parse(expiryRaw.get());
            return Instant.now().isAfter(expiryTime.minusSeconds(30)); // small safety buffer
        } catch (Exception e) {
            return true; // corrupted expiry → treat as expired
        }
    }

    /**
     * Tries to refresh the token using the refresh token if expired.
     */
    public Http.Session ensureValidToken(Http.Session session) {
        if (!isTokenExpired(session)) {
            return session;
        }

        Optional<String> refreshTokenOpt = session.getOptional(REFRESH_TOKEN_KEY);
        if (refreshTokenOpt.isEmpty()) {
            return session.removing(ACCESS_TOKEN_KEY).removing(TOKEN_EXPIRY_KEY);
        }

        try {
            AuthResponse refreshed = authService.refreshToken(refreshTokenOpt.get());
            String username = session.getOptional(USERNAME_KEY).orElse("unknown");
            return saveTokens(session, refreshed, username);
        } catch (Exception e) {
            // Refresh failed — clean up session
            return session.removing(ACCESS_TOKEN_KEY)
                    .removing(REFRESH_TOKEN_KEY)
                    .removing(TOKEN_EXPIRY_KEY);
        }
    }

    /**
     * Clears all authentication info from session (logout).
     */
    public Http.Session clear(Http.Session session) {
        return session.removing(ACCESS_TOKEN_KEY)
                .removing(REFRESH_TOKEN_KEY)
                .removing(TOKEN_EXPIRY_KEY)
                .removing(USERNAME_KEY);
    }
}
