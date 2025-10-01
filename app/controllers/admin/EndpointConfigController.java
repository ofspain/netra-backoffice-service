package controllers.admin;

import com.netra.commons.contracts.Domain;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.models.Switcher;
import com.netra.commons.models.endpoint.EndpointConfig;
import com.netra.commons.util.BasicUtil;
import dtos.DomainIdentity;
import play.mvc.*;
import services.FinancialInstitutionService;

import javax.inject.Inject;

import java.time.LocalDateTime;

import static com.netra.commons.util.BasicUtil.decodeIdStringFromUrl;

public class EndpointConfigController extends Controller {

    private final FinancialInstitutionService finInstService;

    @Inject
    public EndpointConfigController(FinancialInstitutionService finInstService) {
        this.finInstService = finInstService;
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


        // TODO: fetch FI + its endpoint config, and render dedicated form
        return ok(views.html.admin.endpoint_config_form.render(domainOwner, ec, request));
    }

    public Result save(Http.Request request) {

        // TODO: revoke all form data here and save ec plus update domain if needed
        return ok("");
    }
}

