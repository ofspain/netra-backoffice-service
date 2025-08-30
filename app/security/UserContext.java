package security;

import com.google.inject.Singleton;
import org.pac4j.core.profile.UserProfile;
import play.mvc.Http;

import java.util.Optional;

@Singleton
public class UserContext {

    public Optional<UserProfile> getCurrentUser(Http.Request request) {
        return Secured.getUserProfile(request);
    }

    public String getCurrentUserId(Http.Request request) {
        return getCurrentUser(request).map(UserProfile::getId).orElse(null);
    }

    public String getUsername(Http.Request request) {
        return getCurrentUser(request)
                .map(profile -> (String) profile.getAttribute("username"))
                .orElse(null);
    }

    public String getEmail(Http.Request request) {
        return getCurrentUser(request)
                .map(profile -> (String) profile.getAttribute("email"))
                .orElse(null);
    }

    public boolean hasRole(Http.Request request, String role) {
        return getCurrentUser(request)
                .map(profile -> profile.getRoles() != null && profile.getRoles().contains(role))
                .orElse(false);
    }

    public boolean isAdmin(Http.Request request) {
        return hasRole(request, "ADMIN");
    }
}