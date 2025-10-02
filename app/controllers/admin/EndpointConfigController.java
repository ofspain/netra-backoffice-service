package controllers.admin;

import com.netra.commons.contracts.Domain;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.models.Switcher;
import com.netra.commons.models.endpoint.EndpointConfig;
import com.netra.commons.util.BasicUtil;
import dtos.DomainIdentity;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.*;
import services.EndpointConfigService;
import services.FinancialInstitutionService;

import javax.inject.Inject;

import java.time.LocalDateTime;

import static com.netra.commons.util.BasicUtil.decodeIdStringFromUrl;

public class EndpointConfigController extends Controller {

    private final FinancialInstitutionService finInstService;
    private final EndpointConfigService endpointConfigService;

    private final FormFactory formFactory;
    private final Form<EndpointConfig> endpointConfigForm;


    @Inject
    public EndpointConfigController(FinancialInstitutionService finInstService, EndpointConfigService endpointConfigService,
                                    FormFactory formFactory) {
        this.finInstService = finInstService;
        this.formFactory = formFactory;
        this.endpointConfigForm = this.formFactory.form(EndpointConfig.class);
        this.endpointConfigService = endpointConfigService;
    }

    public Result showForm(String mode, String hashedDomainType, String hashedId, Http.Request request) {
        Long domainId = decodeIdStringFromUrl(hashedId);
        String domainTypeStr = BasicUtil.decodeStringFromURL(hashedDomainType);
        DomainType domainType = DomainType.valueOf(domainTypeStr);
        LocalDateTime lastUpdated;
        LocalDateTime createdAt;

        EndpointConfig ec = new EndpointConfig();
        if(mode.contains("update")){
            //todo: load ec for domain here and reinitialize ec
        }
        Domain domain;
        switch (domainType){
            case FINANCIAL_INSTITUTION -> {
                domain = finInstService.findMinimalFinancialInstitutionByUniqueKey("id", domainId);
                lastUpdated = ((FinancialInstitution) domain).getUpdatedAt();
                createdAt = ((FinancialInstitution) domain).getCreatedAt();
            }
            default -> {
                throw new RuntimeException("Domain not mapped");
            }
        }

        DomainIdentity domainOwner = new DomainIdentity(domainTypeStr,domainId,lastUpdated,domain.getDomainCode(),createdAt);


        String endpointsJson = play.libs.Json.toJson(ec.getEndpoints()).toString();
        Form<EndpointConfig> formData = endpointConfigForm.fill(ec);
        // TODO: fetch FI + its endpoint config, and render dedicated form
        return ok(views.html.admin.endpoint_config_form.render(domainOwner, ec, formData, endpointsJson, request));
    }

    public Result save(Http.Request request) {

        // TODO: revoke all form data here and save ec plus update domain if needed
        return ok("");
    }

    public Result update(String encodeId, Http.Request request) {

        // TODO: revoke all form data here and save ec plus update domain if needed
        return ok("");
    }
}

