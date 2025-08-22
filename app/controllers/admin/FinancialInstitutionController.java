package controllers.admin;

import com.netra.commons.models.EndpointConfig;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.util.BasicUtil;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.S3Service;
import services.db.JdbcWrapper;
import utilities.dto.FileUpload;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;

public class FinancialInstitutionController extends Controller {

    private final FormFactory formFactory;
    private final Form<FinancialInstitution> institutionForm;

    private final JdbcWrapper jdbcClient;

    private final S3Service s3Service;

    @Inject
    public FinancialInstitutionController(FormFactory formFactory, JdbcWrapper db, S3Service s3Service) {
        this.formFactory = formFactory;
        this.institutionForm = formFactory.form(FinancialInstitution.class);
        this.jdbcClient = db;
        this.s3Service = s3Service;
    }

    public Result createNewFinInst(Http.Request request){
        FinancialInstitution institution = new FinancialInstitution();
        Form<FinancialInstitution> formData = institutionForm.fill(institution);
        return ok(views.html.admin.fin_ints_form.render(formData, institution, request));
    }

    public Result saveNewFinInst(Http.Request request){

        Form<FinancialInstitution> formData = institutionForm.bindFromRequest(request);

        Map<String,String> raw = formData.rawData();

        System.out.println(raw);


        //todo: validate here
        formData.withError("name", "Name already taken");
        formData.withError("code", "Code already taken");
        System.out.println(formData.hasGlobalErrors()+" form has error>> "+formData.hasErrors());
        if (formData.hasErrors()) {
            System.out.println("rteurning here....");
            for(ValidationError error :formData.errors()){
                System.out.println(error.key() + " "+error.message());
            }
            return badRequest(views.html.admin.fin_ints_form.render(formData, new FinancialInstitution(), request));
        }

        // Extract uploader-specific fields separately
        String logoBase64 = request.body().asFormUrlEncoded().get("logoFile_binary")[0];
        String logoActionStr = request.body().asFormUrlEncoded().get("logoFile_action")[0];
        FileUpload.FileAction logoAction = FileUpload.FileAction.fixActionTypeFromString(logoActionStr);

        FinancialInstitution institution = formData.get();


        if(BasicUtil.validString(logoActionStr)){
             institution.setLogoKey(uploadLogo(logoActionStr));
        }




        //  Persist the main entity
        //saveFinancialInstitution(institution);



        return ok("success");
    }

    private String uploadLogo(String dataUri){

      String key = s3Service.uploadBase64(dataUri, "logos");
      return key;
    }

    public Result updateOldFinInst(Long id, Http.Request request){

        return ok("success");
    }

    public Result viewInstitute(Long id, Http.Request request){

        return ok(views.html.admin.fin_inst_single.render(dummyInstitution(),request));
    }

    public Result editInstitute(Long id, Http.Request request){
        FinancialInstitution dummy = dummyInstitution();
        Form<FinancialInstitution> formData = institutionForm.fill(dummy);
        BasicUtil.encodeUrlBoundId(id);//todo: use to decode and encode url bound id
        return ok(views.html.admin.fin_ints_form.render(formData, dummy, request));
    }

    public Result deleteInstitute(Long id, Http.Request request){

        FinancialInstitution dummy = dummyInstitution();
        Form<FinancialInstitution> formData = institutionForm.fill(dummy);
        return ok(views.html.admin.fin_ints_form.render(formData, dummy, request));
    }


    public Result index(Integer page, Http.Request request){
        List<FinancialInstitution> dummies = new ArrayList<>();

        for(int i=0; i<20; i++){
            dummies.add(dummyInstitution());
        }
        return ok(views.html.admin.fin_inst_lists.render(dummies, 2, 100, request));
    }



    private FinancialInstitution dummyInstitution(){
        FinancialInstitution institution = new FinancialInstitution();
        institution.setId(1l);
        institution.setCode("FBN");
        institution.setName("First Bank of Nigeria");
        institution.setDomainCode("fbn");
        institution.setCreatedAt(LocalDateTime.now());
        institution.setEndpointConfig(new EndpointConfig());

        return institution;
    }
}
