package controllers.admin;

import com.google.inject.Inject;
import com.netra.commons.models.CustomerUser;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.models.Identity;
import com.netra.commons.requests.CreateIdentityRequest;
import com.netra.commons.util.BasicUtil;
import org.apache.commons.lang3.StringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.CustomerUserService;
import services.FinancialInstitutionService;
import utilities.FormDataValidators;
import utilities.PaginatedResult;
import utilities.PaginationParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netra.commons.util.BasicUtil.encodeUrlBoundId;

public class CustomerUserController extends Controller {


    private final FormFactory formFactory;
    private final Form<CustomerUser> customerUserForm;

    private final CustomerUserService customerUserService;
    private final FinancialInstitutionService financialInstitutionService;


    @Inject
    public CustomerUserController( FormFactory formFactory, FinancialInstitutionService financialInstitutionService,
                                   CustomerUserService customerUserService) {
        this.formFactory = formFactory;
        this.customerUserForm = this.formFactory.form(CustomerUser.class);
        this.customerUserService = customerUserService;
        this.financialInstitutionService = financialInstitutionService;

    }

    public Result createNewUser(Http.Request request){
        CustomerUser user = new CustomerUser();
        Form<CustomerUser> formData = customerUserForm.fill(user);
        List<FinancialInstitution> financialInstitutions = financialInstitutionService.findCacheableActiveFinInst();
        return ok(views.html.admin.customer_user_form.render(formData, user, financialInstitutions, request));
    }


    public Result index(Http.Request request){

        String pageStr = request.queryString("page").orElse("1");
        int page = 1;
        if(StringUtils.isNumeric(pageStr)){
            page = Integer.parseInt(pageStr);
        }
        String limitStr = request.queryString("limit").orElse("20");
        int limit = 1;
        if(StringUtils.isNumeric(limitStr)){
            limit = Integer.parseInt(limitStr);
        }


        String sortBy = request.queryString("sort").orElse("created_at");
        String direction = request.queryString("sort_direction").orElse("DESC");

        PaginationParams paginationParams = new PaginationParams(page, limit, sortBy, direction);


        try {
            PaginatedResult<CustomerUser> paginatedResult = customerUserService
                    .findAll(paginationParams).toCompletableFuture().get();

            System.out.println("ITEMS "+paginatedResult.getItems().size());

            List<CustomerUser> listed = paginatedResult.getItems();
            Map<String,Object> metaData = new HashMap<>();
            metaData.put("sort_direction", direction);
            metaData.put("page", page);
            metaData.put("limit", limit);
            metaData.put("sort_by", sortBy);
            metaData.put("page_count", 0);
//            metaData.put("page_count", paginatedResult.getTotalPages());
            return ok(views.html.admin.customer_user_lists.render(listed, metaData, request));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Result viewUser(String encodedUserId, Http.Request request){
        Long decodedId = BasicUtil.decodeIdStringFromUrl(encodedUserId);
        CustomerUser user = customerUserService.findCustomerUser("id", decodedId);

        System.out.println("ACCT DETAILS "+user.getAccounts());

        return ok(views.html.admin.customer_user_single.render(user, request));
    }

    public Result editUser(String encodedUserId, Http.Request request){
        Long id = BasicUtil.decodeIdStringFromUrl(encodedUserId);
        CustomerUser user = customerUserService.findCustomerUser("id", id);
        Form<CustomerUser> formData = customerUserForm.fill(user);
        List<FinancialInstitution> financialInstitutions = financialInstitutionService.findCacheableActiveFinInst();
        return ok(views.html.admin.customer_user_form.render(formData, user, financialInstitutions, request));
    }

    public Result updateOldUser(String encodedUserId, Http.Request request){
        return ok("");
    }

    public Result saveNewUser(Http.Request request){
        Form<CustomerUser> formData = customerUserForm.bindFromRequest(request);

        Map<String,String> raw = formData.rawData();

        System.out.println(raw);


        formData = FormDataValidators.validateCustomerUser(formData,customerUserService);
        CustomerUser user = formData.get();

        if (formData.hasErrors()) {
            for(ValidationError error :formData.errors()){
                System.out.println(error.key() + " "+error.message());
            }

            List<FinancialInstitution> financialInstitutions = financialInstitutionService.findCacheableActiveFinInst();
            return badRequest(views.html.admin.customer_user_form.render(formData, user, financialInstitutions, request));
        }


        //todo: clean up exception handling here
        try {
            Identity identity = new Identity();
            identity.setDisabled(user.getDisabled());
            identity.setUsername(user.getUserPhone());
            identity.setPassword(raw.get("identity.password"));
            CreateIdentityRequest identityRequest = new CreateIdentityRequest(identity);
            user = customerUserService.saveCustomerUser(user, identityRequest, "").toCompletableFuture().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        String hashedID = encodeUrlBoundId(user.getId());

        return redirect(routes.CustomerUserController.viewUser(hashedID));
    }

    public Result deleteUser(String encodedUserId, Http.Request request){
        return ok("");
    }
}
