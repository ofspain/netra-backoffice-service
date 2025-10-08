package controllers;

import com.typesafe.config.Config;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import security.SessionTokenManager;
import services.AuthService;
import utilities.dto.AuthResponse;
import utilities.dto.LoginOutcome;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthController extends Controller {

    private final Config pac4jConfig;
    private final FormFactory formFactory;
    private final AuthService authService;
    private final SessionTokenManager sessionTokenManager;


    @Inject
    public AuthController(Config pac4jConfig, FormFactory formFactory,
                          AuthService authService, SessionTokenManager sessionTokenManager) {

        this.pac4jConfig = pac4jConfig;
        this.formFactory = formFactory;
        this.authService = authService;
        this.sessionTokenManager = sessionTokenManager;
    }

    public Result logout(Http.Request request){

            return ok(views.html.login.render(request))
                    .withSession(sessionTokenManager.clear(request.session()));
    }
    public Result loginForm(Http.Request request) {
        return ok(views.html.login.render(request));
    }


    public Result login(Http.Request request) {
        DynamicForm loginForm = formFactory.form().bindFromRequest(request);
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        LoginOutcome outcome = authService.processLogin(username, password);


        Http.Session updatedSession = sessionTokenManager.saveTokens(
                request.session(),
                new AuthResponse(
                        outcome.loginResult().accessToken(),
                        outcome.loginResult().tokenType(),
                        outcome.loginResult().expiresIn(),
                        outcome.loginResult().lastLogin(),
                        outcome.loginResult().refreshToken()
                ),
                outcome.loginResult().username()
        );

        return redirect(outcome.redirectPath())
                .withSession(updatedSession);
    }

}
