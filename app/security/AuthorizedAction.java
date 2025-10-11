package security;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.context.PlayFrameworkParameters;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
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

        String[] requiredRoles = configuration != null ? configuration.value() : new String[0];

        // 🔄 FLEXIBLE AUTHORIZATION LOGIC
        boolean authorized;

        //

        if (requiredRoles.length == 0) {
            // 🟢 No specific role required - just ensure user has ANY role (authentication only)
            Authorizer anyRoleAuthorizer = config.getAuthorizers().get("anyRoleAuthorizer");
            if (anyRoleAuthorizer != null) {
                authorized = anyRoleAuthorizer.isAuthorized(context, sessionStore, List.of(profile));
                System.out.println("🔓 Using anyRole authorizer - User has at least one role: " + authorized);
            } else {
                // Fallback: just check if user is authenticated (has roles)
                authorized = profile.getRoles() != null && !profile.getRoles().isEmpty();
                System.out.println("🔓 Fallback anyRole check - User has roles: " + authorized);
            }
        } else {
            // 🔐 Specific role required - use dynamic role authorizer
            Authorizer dynamicAuthorizer = config.getAuthorizers().get("requireRoleAuthorizer");
            if (dynamicAuthorizer instanceof RoleAuthorizer) {
                RoleAuthorizer roleAuthorizer = (RoleAuthorizer) dynamicAuthorizer;
                roleAuthorizer.setRequiredRoles(requiredRoles); // Inject the required role
                authorized = roleAuthorizer.isAuthorized(context, sessionStore, List.of(profile));
                System.out.println("🔐 Using dynamicRole authorizer - Required: " + requiredRoles + ", Authorized: " + authorized);
            } else {
                // Fallback: manual role check
                authorized = profile.getRoles() != null && profile.getRoles().contains(requiredRoles);
                System.out.println("🔐 Fallback role check - Required: " + requiredRoles + ", Authorized: " + authorized);
            }
        }

        if (!authorized) {
            String message = (requiredRoles == null || requiredRoles.length == 0)
                    ? "Authentication failed - user has no roles"
                    : "Insufficient permissions - required role: " + StringUtils.join(requiredRoles, ",");
            return CompletableFuture.completedFuture(forbidden(message));
        }

        // Store profile in request for controller access
        Http.Request newRequest = request.addAttr(USER_PROFILE_KEY, profile);
        return delegate.call(newRequest);
    }
}