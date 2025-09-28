package controllers.admin;

import com.netra.commons.models.endpoint.EndpointConfig;
import com.netra.commons.util.BasicUtil;
import dtos.DomainIdentity;
import play.mvc.*;
import javax.inject.Inject;

import static com.netra.commons.util.BasicUtil.decodeIdStringFromUrl;

public class EndpointConfigController extends Controller {

    @Inject
    public EndpointConfigController() {}

    public Result showForm(String hashedDomainType, String hashedId, Http.Request request) {
        Long domainId = decodeIdStringFromUrl(hashedId);
        String domainType = BasicUtil.decodeStringFromURL(hashedDomainType);
        String path = request.path();
        EndpointConfig ec = new EndpointConfig();
        if(path.contains("update")){
            //todo: load ec for domain here and reinitialize ec
        }
        //todo: use switch statement and domainType to load the actual domain here, then extract neede ppt into the dto

        DomainIdentity domainOwner = new DomainIdentity("",0l,null,"",null);


        // TODO: fetch FI + its endpoint config, and render dedicated form
        return ok(views.html.admin.endpoint_config_form.render(domainOwner, ec, request));
    }

    public Result save(Http.Request request) {

        // TODO: revoke all form data here and save ec plus update domain if needed
        return ok("");
    }
}

