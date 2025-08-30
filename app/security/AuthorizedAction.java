package security;


import org.pac4j.core.config.Config;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.context.PlayFrameworkParameters;
import play.libs.typedmap.TypedKey;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static security.Secured.USER_PROFILE_KEY;


@Singleton
public class AuthorizedAction extends Action<Authorized> {

    private final Config config;

    @Inject
    public AuthorizedAction(Config config) {
        this.config = config;
    }

    @Override
    public CompletionStage<Result> call(Http.Request request) {
        PlayWebContext context = new PlayWebContext(request);
        FrameworkParameters parameters = new PlayFrameworkParameters(request);
        SessionStore sessionStore = config.getSessionStoreFactory().newSessionStore(parameters);
        ProfileManager profileManager = new ProfileManager(context, sessionStore);

        Optional<UserProfile> profileOpt = profileManager.getProfile();

        if (profileOpt.isEmpty()) {
            return CompletableFuture.completedFuture(unauthorized("Not authenticated"));
        }

        UserProfile profile = profileOpt.get();

        // Check if user has the required role
        if (configuration != null && configuration.value() != null && !configuration.value().isEmpty()) {
            String requiredRole = configuration.value();
            if (profile.getRoles() == null || !profile.getRoles().contains(requiredRole)) {
                return CompletableFuture.completedFuture(forbidden("Insufficient permissions"));
            }
        }

        // Store profile in request for controller access
        Http.Request newRequest = request.addAttr(USER_PROFILE_KEY, profile);
        return delegate.call(newRequest);
    }
}
