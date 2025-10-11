package security;


import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RoleAuthorizer implements Authorizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleAuthorizer.class);

    private String[] requiredRoles;

    public void setRequiredRoles(String requiredRoles[]){
        this.requiredRoles = requiredRoles;
    }

    @Override
    public boolean isAuthorized(WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return false;
        }

        UserProfile profile = profiles.get(0);
        Set<String> userRoles = profile.getRoles();

        if (userRoles == null || userRoles.isEmpty()) {
            LOGGER.warn("User has no roles assigned");
            return false;
        }

        // Extract required role from the request context or annotation

        if (requiredRoles == null) {
            LOGGER.warn("No required role specified in authorization check");
            return false;
        }

        boolean authorized = Arrays.stream(requiredRoles)
                .allMatch(userRoles::contains);

        if (!authorized) {
            LOGGER.warn("User roles {} do not contain required role: {}",
                    userRoles, requiredRoles);
        }

        return authorized;
    }
}
