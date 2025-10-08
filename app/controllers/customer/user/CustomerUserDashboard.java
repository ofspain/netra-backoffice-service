package controllers.customer.user;


import com.google.inject.Inject;
import com.netra.commons.models.CustomerUser;
import com.netra.commons.util.BasicUtil;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.CustomerUserService;

public class CustomerUserDashboard extends Controller {

    private final CustomerUserService customerUserService;
    @Inject
    public CustomerUserDashboard(CustomerUserService customerUserService){
        this.customerUserService = customerUserService;
    }

    public Result index(String hashedId, Http.Request request){
        Long id = BasicUtil.decodeIdStringFromUrl(hashedId);
        CustomerUser user = customerUserService.findCustomerUser("id", id);
        return ok(user.getUserPhone());
    }
}
