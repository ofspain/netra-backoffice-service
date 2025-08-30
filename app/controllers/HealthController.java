package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http;
import play.mvc.Security;
import security.Authorized;
import security.Secured;

import javax.inject.Singleton;

@Singleton
public class HealthController extends Controller {

    // Public endpoint
    public Result health() {
        return ok("Service is healthy");
    }

    // Secure endpoint - should require JWT
    @Security.Authenticated(Secured.class)
    public Result secureHealth(Http.Request request) {
        return ok("Secure endpoint - should require valid JWT");
    }

    @Security.Authenticated(Secured.class)     // ← Requires JWT
    @Authorized("ADMIN")                       // ← Requires ADMIN role
    public Result manageUsers(Http.Request request) {
        return ok("Admin User Management - requires JWT + ADMIN role");
    }
}