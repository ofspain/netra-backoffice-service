package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import services.S3Service;

import javax.inject.Inject;

public class FilesController  extends Controller {


    private final S3Service s3Service;

    @Inject
    public FilesController(S3Service s3Service){
        this.s3Service = s3Service;
    }


    public Result s3Thumb(String key){
        return ok("success");
    }

}
