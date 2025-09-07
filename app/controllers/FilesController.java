package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import scalas.utils.StaticAssets;
import services.S3Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.coobird.thumbnailator.Thumbnails;


public class FilesController  extends Controller {


    private final S3Service s3Service;

    @Inject
    public FilesController(S3Service s3Service){
        this.s3Service = s3Service;
    }


    public Result s3Thumb(String key) {
        try {
            // 1. Get original file from S3
            byte[] fileBytes = s3Service.getObject(key);

            // 2. Resize to 56x56 thumbnail
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(fileBytes))
                    .size(56, 56)
                    .outputFormat("png")   // force PNG thumbnails
                    .toOutputStream(baos);

            byte[] thumbBytes = baos.toByteArray();

            // 3. Return as HTTP response
            return ok(thumbBytes).as("image/png");

        } catch (Exception e) {
            // fallback to placeholder
            return redirect(StaticAssets.getUrl("imgs/logo-placeholder.png"));
        }
    }


}
