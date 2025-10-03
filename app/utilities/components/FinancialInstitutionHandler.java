package utilities.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.models.endpoint.EndpointConfig;
import com.netra.commons.util.BasicUtil;
import controllers.admin.routes;
import dtos.DomainIdentity;
import play.mvc.Result;
import services.FinancialInstitutionService;

import java.time.LocalDateTime;

import static play.mvc.Results.redirect;

@Singleton
public class FinancialInstitutionHandler implements DomainHandler<FinancialInstitution> {

    private final FinancialInstitutionService finInstService;

    @Inject
    public FinancialInstitutionHandler(FinancialInstitutionService finInstService) {
        this.finInstService = finInstService;
    }

    @Override
    public FinancialInstitution resolve(Long domainId) {
        return finInstService.findMinimalFinancialInstitutionByUniqueKey("id", domainId);
    }

    @Override
    public Result redirectToView(FinancialInstitution domain) {
        String hashedDomainId = BasicUtil.encodeUrlBoundId(domain.getId());
        return redirect(routes.FinancialInstitutionController.viewInstitute(hashedDomainId));
    }

    @Override
    public LocalDateTime getCreatedAt(FinancialInstitution domain) {
        return domain.getCreatedAt();
    }

    @Override
    public LocalDateTime getUpdatedAt(FinancialInstitution domain) {
        return domain.getUpdatedAt();
    }

    @Override
    public EndpointConfig resolveEndpointConfig(Long domainId){
        //todo:use id to load fin_inst
        //retrieve its endpointConfig (or return new if endpointConfig id is null)
        return new EndpointConfig();
    }

}

