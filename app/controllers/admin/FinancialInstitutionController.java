package controllers.admin;

import com.netra.commons.models.EndpointConfig;
import com.netra.commons.models.FinancialInstitution;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FinancialInstitutionController extends Controller {

    public Result createNewFinInst(Http.Request request){
        FinancialInstitution institution = new FinancialInstitution();

        return ok(views.html.admin.fin_ints_form.render(institution, request));
    }

    public Result saveNewFinInst(Http.Request request){

        return ok("success");
    }

    public Result viewInstitute(Long id, Http.Request request){

        return ok(views.html.admin.fin_inst_single.render(dummyInstitution(),request));
    }

    public Result editInstitute(Long id, Http.Request request){

        return ok(views.html.admin.fin_ints_form.render(dummyInstitution(), request));
    }

    public Result deleteInstitute(Long id, Http.Request request){

        return ok(views.html.admin.fin_ints_form.render(dummyInstitution(), request));

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
