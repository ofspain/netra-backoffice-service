package utilities;

import com.netra.commons.enums.DomainType;
import com.netra.commons.models.EndpointConfig;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.util.BasicUtil;
import play.data.Form;
import services.FinancialInstitutionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FormDataValidators {

    public static Form<FinancialInstitution> validateFinancialInstitution(Form<FinancialInstitution> fiForm, FinancialInstitutionService financialInstService){
        FinancialInstitution financialInstitution = fiForm.get();
        Long id = financialInstitution.getId();
        boolean isUpdate = null != id;

        String name = financialInstitution.getName();
        if(!BasicUtil.validString(name)){
            fiForm = fiForm.withError("name", "Institution name can not be empty");
        }

        String code = financialInstitution.getCode();
        if(!BasicUtil.validString(code)){
            fiForm = fiForm.withError("code", "Code assigned by regulator cannot be empty");
        }else{


            FinancialInstitution potentialOld = financialInstService.findFinancialInstitutionByUniqueKey("code", code);

            if (potentialOld != null) {
                if (!isUpdate || !potentialOld.getId().equals(id)) {
                    fiForm = fiForm.withError("code", "Code: "+code +" already taken, try another");
                }
            }
        }

        String domainCode = financialInstitution.getDomainCode();
        if(!BasicUtil.validString(domainCode)){
            fiForm = fiForm.withError("domainCode", "Code as used in official email is required");
        }else{

            FinancialInstitution potentialOld = financialInstService.findFinancialInstitutionByUniqueKey("domain_code", domainCode);

            if (potentialOld != null) {
                if (!isUpdate || !potentialOld.getId().equals(id)) {
                    fiForm = fiForm.withError("domainCode", "Domain Code: "+domainCode +" already taken, try another");
                }
            }
        }




        return fiForm;
    }

    private static String keyConcatenate(String baseKey, String fieldName) {
        return baseKey + "." + fieldName;
    }

    public static Map<String, String> validateEndpointConfig(EndpointConfig config, String basePrefix) {
        Map<String, String> errors = new HashMap<>();

        if (config == null) {
            errors.put(basePrefix, "Endpoint configuration must not be null");
            return errors;
        }

        // Domain code
        String domainCode = config.getDomainCode();
        if (!BasicUtil.validString(domainCode)) {
            errors.put(keyConcatenate(basePrefix, "domainCode"), "Domain code is required for endpoint");
        }

        // Domain type
        DomainType domainType = config.getDomainType();
        if (domainType == null) {
            errors.put(keyConcatenate(basePrefix, "domainType"), "Domain Type is required for endpoint");
        }

        // Base URL
        String baseUrl = config.getBaseUrl();
        if (!BasicUtil.validString(baseUrl)) {
            errors.put(keyConcatenate(basePrefix, "baseUrl"), "Base URL is compulsory for endpoint configuration");
        }

        // Timeout
        if (config.getTimeoutMillis() <= 0) {
            errors.put(keyConcatenate(basePrefix, "timeoutMillis"), "Timeout must be a positive number");
        }

        // Proxy
        if (config.isUseProxy()) {
            EndpointConfig.ProxyConfig proxy = config.getProxy();
            if (proxy == null) {
                errors.put(keyConcatenate(basePrefix, "proxy"), "Proxy configuration must be provided when proxy is enabled");
            } else {
                errors.putAll(validateProxyConfig(proxy, keyConcatenate(basePrefix, "proxy")));
            }
        }

        // Endpoint details
        EndpointConfig.EndpointDetail unique = config.getUniqueTransaction();
        EndpointConfig.EndpointDetail multiple = config.getMultipleTransaction();

        if (unique == null && multiple == null) {
            errors.put(basePrefix, "At least one of uniqueTransaction or multipleTransaction must be provided");
        } else {
            if (unique != null) {
                errors.putAll(validateEndpointDetail(unique, keyConcatenate(basePrefix, "uniqueTransaction")));
            }
            if (multiple != null) {
                errors.putAll(validateEndpointDetail(multiple, keyConcatenate(basePrefix, "multipleTransaction")));
            }
        }

        return errors;
    }

    private static Map<String, String> validateProxyConfig(EndpointConfig.ProxyConfig proxy, String basePrefix) {
        Map<String, String> errors = new HashMap<>();

        if (!BasicUtil.validString(proxy.getHost())) {
            errors.put(keyConcatenate(basePrefix, "host"), "Proxy host must not be empty");
        }

        Integer port = proxy.getPort();
        if (port == null || port <= 0 || port > 65535) {
            errors.put(keyConcatenate(basePrefix, "port"), "Proxy port must be between 1 and 65535");
        }

        return errors;
    }

    private static Map<String, String> validateEndpointDetail(EndpointConfig.EndpointDetail detail, String basePrefix) {
        Map<String, String> errors = new HashMap<>();

        if (!BasicUtil.validString(detail.getUrl())) {
            errors.put(keyConcatenate(basePrefix, "url"), "URL must not be blank");
        }

        if (detail.getMethod() == null) {
            errors.put(keyConcatenate(basePrefix, "method"), "Method must be specified (GET or POST)");
        }

        List<EndpointConfig.EndpointHeader> headers = detail.getHeaders();
        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                EndpointConfig.EndpointHeader header = headers.get(i);
                if (header.getName() == null || header.getName().isBlank()) {
                    errors.put(keyConcatenate(basePrefix, "headers[" + i + "].name"),
                            "Header name must not be blank");
                }
            }
        }

        return errors;
    }

}
