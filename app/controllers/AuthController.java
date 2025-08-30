package controllers;

import com.typesafe.config.Config;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthController extends Controller {

    private final Config pac4jConfig;

    @Inject
    public AuthController(Config pac4jConfig) {
        this.pac4jConfig = pac4jConfig;
    }

    public Result logout(Http.Request request){
        //todo: clear user's session and invalidate all user's session on cache, also invalidate token if need be
        return ok(views.html.login.render());
    }

    public Result login() {
        return ok(views.html.login.render());
    }
}
