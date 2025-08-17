package controllers.admin;

import com.netra.commons.models.FinancialInstitution;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class FinancialInstitutionController extends Controller {

    public Result createNewFinInst(Http.Request request){
        FinancialInstitution institution = new FinancialInstitution();

        return ok(views.html.admin.fin_ints_form.render(institution, request));
    }

    public Result saveNewFinInst(Http.Request request){

        return ok("success");
    }
}
