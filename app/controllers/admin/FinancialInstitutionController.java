package controllers.admin;

import com.netra.commons.enums.DomainType;
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
import services.FinancialInstitutionService;
import services.S3Service;
import services.db.JdbcWrapper;
import utilities.FormDataValidators;
import utilities.dto.EntityWithUpload;
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

    private final FinancialInstitutionService finInstService;

    private final S3Service s3Service;

    @Inject
    public FinancialInstitutionController(FormFactory formFactory, FinancialInstitutionService finInstService, S3Service s3Service) {
        this.formFactory = formFactory;
        this.institutionForm = formFactory.form(FinancialInstitution.class);
        this.finInstService = finInstService;
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


        formData = FormDataValidators.validateFinancialInstitution(formData,finInstService);


        EndpointConfig endpointConfig = formData.get().getEndpointConfig();
        endpointConfig.setDomainType(DomainType.FINANCIAL_INSTITUTION);
        endpointConfig.setDomainCode(formData.get().getDomainCode());

        Map<String, String> endpointErrors = FormDataValidators.validateEndpointConfig(endpointConfig, "endpointConfig");

        for (Map.Entry<String, String> entry : endpointErrors.entrySet()) {
            formData = formData.withError(entry.getKey(), entry.getValue());
        }


        String logoBase64 = formData.rawData().get("logo_binary");

        if (formData.hasErrors()) {
            for(ValidationError error :formData.errors()){
                System.out.println(error.key() + " "+error.message());
            }
            FinancialInstitution logoed = new FinancialInstitution();
            logoed.setLogoKey(logoBase64);
            return badRequest(views.html.admin.fin_ints_form.render(formData, logoed, request));
        }


//        String logoActionStr = request.body().asFormUrlEncoded().get("logoKey_action")[0];
//        FileUpload.FileAction logoAction = FileUpload.FileAction.fixActionTypeFromString(logoActionStr);

        FinancialInstitution institution = formData.get();


        if(BasicUtil.validString(logoBase64)){
             institution.setLogoKey(uploadLogo(logoBase64));
        }



        institution = finInstService.saveFinancialInstitution(institution);

        return ok(views.html.admin.fin_inst_single.render(institution,request));
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
