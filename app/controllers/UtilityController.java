package controllers;

import play.filters.csrf.CSRF;
import play.mvc.Controller;
import play.mvc.Http;

import java.util.Optional;

public class UtilityController extends Controller {
    public static String csrfToken(Http.Request request) {
        String token = "";
        Optional<CSRF.Token> optionalToken = CSRF.getToken(request);
        if(optionalToken.isPresent()) {
            token = optionalToken.get().value();
        }
        return token;
    }
}
