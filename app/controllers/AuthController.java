package controllers;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class AuthController extends Controller {

    public Result logout(Http.Request request){
        return ok(views.html.login.render());
    }
}
