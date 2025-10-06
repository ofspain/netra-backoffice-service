package controllers.admin;

import com.netra.commons.enums.DomainType;
import com.netra.commons.models.endpoint.*;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.util.BasicUtil;
import dtos.DomainIdentity;
import org.apache.commons.lang3.StringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.FinancialInstitutionService;
import services.S3Service;
import utilities.FormDataValidators;
import utilities.PaginatedResult;
import utilities.PaginationHelper;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;


import static com.netra.commons.util.BasicUtil.decodeIdStringFromUrl;
import static com.netra.commons.util.BasicUtil.encodeUrlBoundId;

public class FinancialInstitutionController extends Controller {

    private final FormFactory formFactory;
    private final Form<FinancialInstitution> institutionForm;

    private final FinancialInstitutionService finInstService;

    private final S3Service s3Service;

    @Inject
    public FinancialInstitutionController(FormFactory formFactory, FinancialInstitutionService finInstService, S3Service s3Service) {
        this.formFactory = formFactory;
        this.institutionForm = this.formFactory.form(FinancialInstitution.class);
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



//        EndpointConfig endpointConfig = formData.get().getEndpointConfig();
//        endpointConfig.setDomainOwnerType(DomainType.FINANCIAL_INSTITUTION);
//        endpointConfig.setDomainOwnerCode(formData.get().getDomainCode());
//
//        String endpointFallbackStaticResponse = raw.get("static.response.value");
//        String endpointFallbackRedirectUrl = raw.get("redirect.url.value");
//        String endpointFallbackException = raw.get("exception.message.value");
//        Map<FallbackConfig.FallbackType, String> fallbackTypeValues = new HashMap<>();
//        fallbackTypeValues.put(FallbackConfig.FallbackType.STATIC_RESPONSE, endpointFallbackStaticResponse);
//        fallbackTypeValues.put(FallbackConfig.FallbackType.REDIRECT_ENDPOINT, endpointFallbackRedirectUrl);
//        fallbackTypeValues.put(FallbackConfig.FallbackType.EXCEPTION, endpointFallbackException);
//
//        Map<String, String> endpointErrors = FormDataValidators.validateEndpointConfig(endpointConfig, "endpointConfig", fallbackTypeValues);
//
//        for (Map.Entry<String, String> entry : endpointErrors.entrySet()) {
//            formData = formData.withError(entry.getKey(), entry.getValue());
//        }


        String logoBase64 = formData.rawData().get("logo_binary");

        if (formData.hasErrors()) {
            for(ValidationError error :formData.errors()){
                System.out.println(error.key() + " "+error.message());
            }
            FinancialInstitution logoed = new FinancialInstitution();
            logoed.setLogoKey(logoBase64);
            return badRequest(views.html.admin.fin_ints_form.render(formData, logoed, request));
        }

        FinancialInstitution institution = formData.get();


        if(BasicUtil.validString(logoBase64)){
             institution.setLogoKey(uploadLogo(logoBase64));
        }

        //todo: clean up exception handling here
        try {
            institution = finInstService.saveFinancialInstitutionOnly(institution).toCompletableFuture().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        String action = raw.get("action");
        String hashedID = encodeUrlBoundId(institution.getId());
        if ("to-endpoint".equals(action)) {
            String hashedDomainType = BasicUtil.encodeURLBoundString(DomainType.FINANCIAL_INSTITUTION.name());
            return redirect(routes.EndpointConfigController.showForm("update",hashedDomainType, hashedID));
        }

        return redirect(routes.FinancialInstitutionController.viewInstitute(hashedID));
    }


    public Result saveAndRedirectToConfig(Http.Request request) {

        Form<FinancialInstitution> formData = institutionForm.bindFromRequest(request);

        Map<String, String> raw = formData.rawData();
        formData = FormDataValidators.validateFinancialInstitution(formData, finInstService);

        // build endpoint config defaults
        EndpointConfig endpointConfig = formData.get().getEndpointConfig();
        endpointConfig.setDomainOwnerType(DomainType.FINANCIAL_INSTITUTION);
        endpointConfig.setDomainOwnerCode(formData.get().getDomainCode());

        String endpointFallbackStaticResponse = raw.get("static.response.value");
        String endpointFallbackRedirectUrl = raw.get("redirect.url.value");
        String endpointFallbackException = raw.get("exception.message.value");

        Map<FallbackConfig.FallbackType, String> fallbackTypeValues = new HashMap<>();
        fallbackTypeValues.put(FallbackConfig.FallbackType.STATIC_RESPONSE, endpointFallbackStaticResponse);
        fallbackTypeValues.put(FallbackConfig.FallbackType.REDIRECT_ENDPOINT, endpointFallbackRedirectUrl);
        fallbackTypeValues.put(FallbackConfig.FallbackType.EXCEPTION, endpointFallbackException);

        Map<String, String> endpointErrors = FormDataValidators.validateEndpointConfig(endpointConfig, "endpointConfig", fallbackTypeValues);
        for (Map.Entry<String, String> entry : endpointErrors.entrySet()) {
            formData = formData.withError(entry.getKey(), entry.getValue());
        }

        String logoBase64 = formData.rawData().get("logo_binary");

        if (formData.hasErrors()) {
            FinancialInstitution logoed = new FinancialInstitution();
            logoed.setLogoKey(logoBase64);
            return badRequest(views.html.admin.fin_ints_form.render(formData, logoed, request));
        }

        FinancialInstitution institution = formData.get();

        if (BasicUtil.validString(logoBase64)) {
            institution.setLogoKey(uploadLogo(logoBase64));
        }

        try {
            institution = finInstService.saveFinancialInstitutionWithEndpoint(institution).toCompletableFuture().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        String hashedDomainType = BasicUtil.encodeURLBoundString(DomainType.FINANCIAL_INSTITUTION.name());
        String hashedId = encodeUrlBoundId(institution.getId());


        // 🔑 redirect to EndpointConfig form after save
        return redirect(routes.EndpointConfigController.showForm("save",hashedDomainType, hashedId));
    }

    public Result updateAndRedirectToConfig(String hashedId, Http.Request request) {
        Long id = decodeIdStringFromUrl(hashedId);

        Form<FinancialInstitution> formData = institutionForm.bindFromRequest(request);

        String logoBase64 = formData.rawData().get("logo_binary");

        if (formData.hasErrors()) {
            FinancialInstitution logoed = new FinancialInstitution();
            logoed.setLogoKey(logoBase64);
            return badRequest(views.html.admin.fin_ints_form.render(formData, logoed, request));
        }

        FinancialInstitution institution = formData.get();
        institution.setId(id);

        if (BasicUtil.validString(logoBase64)) {
            institution.setLogoKey(uploadLogo(logoBase64));
        }

        try {
            institution = finInstService.saveFinancialInstitutionWithEndpoint(institution).toCompletableFuture().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        String hashedDomainType = BasicUtil.encodeURLBoundString(DomainType.FINANCIAL_INSTITUTION.name());


        // 🔑 redirect to EndpointConfig form after update
        return redirect(routes.EndpointConfigController.showForm("update",hashedDomainType, hashedId));
    }


    private String uploadLogo(String dataUri){

      String key = s3Service.uploadBase64(dataUri, "logos");
      return key;
    }

    public Result updateOldFinInst(String hashedId, Http.Request request){
        Long id = decodeIdStringFromUrl(hashedId);

        //todo: implement this guy
        return ok("");
    }

    public Result viewInstitute(String hashedId, Http.Request request){
        Long id = decodeIdStringFromUrl(hashedId);

        //todo: clean up exception handling here
        try {
            FinancialInstitution institution =  finInstService.findById(id).toCompletableFuture().get();

            EndpointConfig endpointConfig = institution.getEndpointConfig();
            DomainIdentity domainIdentity = new DomainIdentity(
                    DomainType.FINANCIAL_INSTITUTION.name(), institution.getId(), institution.getUpdatedAt(),
                    institution.getDomainCode(), institution.getCreatedAt()
            );
            return ok(views.html.admin.domain_single.render(institution,domainIdentity, endpointConfig, request));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public Result editInstitute(String hashedId, Http.Request request){
        Long id = decodeIdStringFromUrl(hashedId);

        //todo: clean up exception handling here
        try {
            FinancialInstitution institution = finInstService.findById(id).toCompletableFuture().get();
            Form<FinancialInstitution> formData = institutionForm.fill(institution);
            return ok(views.html.admin.fin_ints_form.render(formData, institution, request));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Result deleteInstitute(String hashedId, Http.Request request){
        Long id = decodeIdStringFromUrl(hashedId);
        //todo: complete the implementation

        FinancialInstitution institution = null;
        try {
            institution = finInstService.findById(id).toCompletableFuture().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        Form<FinancialInstitution> formData = institutionForm.fill(institution);
        return ok(views.html.admin.fin_ints_form.render(formData, institution, request));
    }


    public Result index(Http.Request request){
//        String pageStr = request.queryString("page").orElse("1");
//        int page = 1;
//        if(StringUtils.isNumeric(pageStr)){
//            page = Integer.parseInt(pageStr);
//        }
//        String limitStr = request.queryString("limit").orElse("20");
//        int limit = 1;
//        if(StringUtils.isNumeric(limitStr)){
//            limit = Integer.parseInt(limitStr);
//        }
//
//
//        String sortBy = request.queryString("sort").orElse("created_at");
//        String direction = request.queryString("sort_direction").orElse("DESC");
//
//
//        try {
//            PaginatedResult<FinancialInstitution> paginatedResult = finInstService
//                    .findAll(limit, page, sortBy, direction).toCompletableFuture().get();
//
//            List<FinancialInstitution> listed = paginatedResult.getItems();
//            Map<String,Object> metaData = new HashMap<>();
//            metaData.put("sort_direction", direction);
//            metaData.put("page", page);
//            metaData.put("limit", limit);
//            metaData.put("sort_by", sortBy);
//            metaData.put("page_count", paginatedResult.getTotalPages());
//            return ok(views.html.admin.fin_inst_lists.render(listed, metaData, request));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }


        return PaginationHelper.renderPaginatedList(
                request,
                params -> finInstService.findAll(params.getLimit(), params.getPage(), params.getSortBy(), params.getDirection()),
                "created_at",
                "DESC",
                (listed, metaData, req) -> views.html.admin.fin_inst_lists.render(listed, metaData, req)
        );

    }



    private FinancialInstitution dummyInstitution(){
        FinancialInstitution institution = new FinancialInstitution();
        institution.setId(1l);
        institution.setCode("FBN");
        institution.setName("First Bank Of Nigeria");
        institution.setDomainCode("fbn");
        institution.setCreatedAt(LocalDateTime.now());
        institution.setEndpointConfig(new EndpointConfig());
        institution.setDisabled(false);

        institution.setLogoKey("logos/b6c2dd13-6c8d-4dc2-9d5e-eac83669d83d.png");

        return institution;
    }

}
