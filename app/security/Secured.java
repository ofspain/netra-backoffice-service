package security;

import org.pac4j.core.config.Config;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.context.PlayFrameworkParameters;
import play.libs.typedmap.TypedKey;
import play.mvc.Http;
import play.mvc.Security;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class Secured extends Security.Authenticator {



    public static final TypedKey<UserProfile> USER_PROFILE_KEY = TypedKey.create("userProfile");

    private final Config config;

    @Inject
    public Secured(Config config) {
        this.config = config;
    }

    @Override
    public Optional<String> getUsername(Http.Request request) {
        PlayWebContext context = new PlayWebContext(request);
        FrameworkParameters parameters = new PlayFrameworkParameters(request);
        SessionStore sessionStore = config.getSessionStoreFactory().newSessionStore(parameters);
        ProfileManager profileManager = new ProfileManager(context, sessionStore);

        Optional<UserProfile> profile = profileManager.getProfile();

        if (profile.isPresent()) {
            // Store profile in request attributes for later access
            request = request.addAttr(USER_PROFILE_KEY, profile.get());  // returns a new request with the attribute

            return Optional.of(profile.get().getId());
        }

        return Optional.empty();
    }
    // Helper method to get user profile from request
    public static Optional<UserProfile> getUserProfile(Http.Request request) {
        return request.attrs().getOptional(USER_PROFILE_KEY);
    }
}