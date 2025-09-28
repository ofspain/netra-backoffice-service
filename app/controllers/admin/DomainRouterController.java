package controllers.admin;

import play.mvc.Controller;
import play.mvc.Result;

public class DomainRouterController  extends Controller {

    public Result domainIndex(String hashedDomainType){
        return ok("");
    }

    public Result editDomain(String hashedDomainType, String hashedDomainId){

        return ok("");
    }
}
