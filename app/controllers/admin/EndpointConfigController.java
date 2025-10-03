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
import play.data.validation.ValidationError;
import play.mvc.*;
import services.EndpointConfigService;
import services.FinancialInstitutionService;
import utilities.FormDataValidators;
import utilities.components.DomainHandler;
import utilities.components.DomainHandlerRegistry;

import javax.inject.Inject;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.netra.commons.enums.DomainType.FINANCIAL_INSTITUTION;
import static com.netra.commons.util.BasicUtil.decodeIdStringFromUrl;

public class EndpointConfigController extends Controller {

    private final FinancialInstitutionService finInstService;
    private final EndpointConfigService endpointConfigService;

    private final FormFactory formFactory;
    private final Form<EndpointConfig> endpointConfigForm;

    private final DomainHandlerRegistry handlerRegistry;


    @Inject
    public EndpointConfigController(FinancialInstitutionService finInstService, EndpointConfigService endpointConfigService,
                                    FormFactory formFactory, DomainHandlerRegistry handlerRegistry) {
        this.finInstService = finInstService;
        this.formFactory = formFactory;
        this.endpointConfigForm = this.formFactory.form(EndpointConfig.class);
        this.endpointConfigService = endpointConfigService;

        this.handlerRegistry = handlerRegistry;
    }

    public Result showForm(String mode, String hashedDomainType, String hashedId, Http.Request request) {
        Long domainId = decodeIdStringFromUrl(hashedId);
        String domainTypeStr = BasicUtil.decodeStringFromURL(hashedDomainType);
        DomainType domainType = DomainType.valueOf(domainTypeStr);

        DomainHandler<Domain> handler = handlerRegistry.getHandler(domainType);
        Domain domain = handler.resolve(domainId);

        // timestamps and metadata are handler-specific
        LocalDateTime createdAt = handler.getCreatedAt(domain);
        LocalDateTime lastUpdated = handler.getUpdatedAt(domain);

        DomainIdentity domainOwner = new DomainIdentity(
                domainTypeStr,
                domainId,
                lastUpdated,
                handler.getDomainCode(domain),
                createdAt
        );

        EndpointConfig ec = new EndpointConfig();
        if (mode.contains("update")) {
            // TODO: load ec for domain here
        }

        String endpointsJson = play.libs.Json.toJson(ec.getEndpoints()).toString();
        Form<EndpointConfig> formData = endpointConfigForm.fill(ec);

        return ok(views.html.admin.endpoint_config_form.render(domainOwner, ec, formData, endpointsJson, request));
    }


    public Result save(Http.Request request) {
        Form<EndpointConfig> formData = endpointConfigForm.bindFromRequest(request);
        Map<String,String> raw = formData.rawData();

        String domainTypeStr = raw.get("domainOwnerType");
        String domainCode = raw.get("domainOwnerCode");
        Long domainId = Long.parseLong(raw.get("domainOwnerId"));

        DomainType domainType = DomainType.valueOf(domainTypeStr);

        // use strategy to resolve domain
        DomainHandler<Domain> handler = handlerRegistry.getHandler(domainType);
        Domain domain = handler.resolve(domainId);

        DomainIdentity domainOwner = new DomainIdentity(domainTypeStr, domainId, null, domainCode, null);

        formData = FormDataValidators.validateEndpointConfig(formData, new HashMap<>());

        if (formData.hasErrors()) {
            EndpointConfig erroredEC = formData.get();
            String endpointsJson = erroredEC.getEndpoints() != null
                    ? play.libs.Json.toJson(erroredEC.getEndpoints()).toString()
                    : "";

            return badRequest(views.html.admin.endpoint_config_form.render(domainOwner, erroredEC, formData, endpointsJson, request));
        }

        EndpointConfig endpointConfig = formData.get();
        try {
            endpointConfigService.saveEndpointConfig(endpointConfig).toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save endpoint config", e);
        }

        // use strategy to redirect
        return handler.redirectToView(domain);
    }

    public Result update(String encodedId, Http.Request request) {
        Long domainId = decodeIdStringFromUrl(encodedId);
        String domainTypeStr = request.body().asFormUrlEncoded().get("domainOwnerType")[0];
        DomainType domainType = DomainType.valueOf(domainTypeStr);

        DomainHandler<Domain> handler = handlerRegistry.getHandler(domainType);
        Domain domain = handler.resolve(domainId);

        // TODO: revoke form data, update EndpointConfig, maybe update domain

        // when done, redirect
        return handler.redirectToView(domain);
    }

}

