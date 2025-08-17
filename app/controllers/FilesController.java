package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class FilesController  extends Controller {

    public Result s3Signed(String key){
        return ok("success");
    }
}
