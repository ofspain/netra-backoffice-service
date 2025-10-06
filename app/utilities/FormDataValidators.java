package utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.CustomerUser;
import com.netra.commons.models.endpoint.*;
import com.netra.commons.models.FinancialInstitution;
import com.netra.commons.util.BasicUtil;
import play.data.Form;
import services.CustomerUserService;
import services.FinancialInstitutionService;

import java.util.Collections;
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


            FinancialInstitution potentialOld = financialInstService.findMinimalFinancialInstitutionByUniqueKey("code", code);

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

            FinancialInstitution potentialOld = financialInstService.findMinimalFinancialInstitutionByUniqueKey("domain_code", domainCode);

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
    public static Map<String, String> validateEndpointConfig(
            EndpointConfig config,
            String basePrefix,
            Map<FallbackConfig.FallbackType, String> fallbackValues
    ) {
        Map<String, String> errors = new HashMap<>();

        if (config == null) {
            errors.put(basePrefix, "Endpoint configuration must not be null");
            return errors;
        }

        // Domain checks
        if (!BasicUtil.validString(config.getDomainOwnerCode())) {
            errors.put(keyConcatenate(basePrefix, "domainOwnerCode"), "Domain owner code is required for endpoint");
        }
        if (config.getDomainOwnerType() == null) {
            errors.put(keyConcatenate(basePrefix, "domainOwnerType"), "Domain owner type is required for endpoint");
        }

        if (config.getDomainOwnerId() == null) {
            errors.put(keyConcatenate(basePrefix, "domainOwnerId"), "Domain owner id is required for endpoint");
        }

        // --- Network validation ---
        NetworkConfig net = config.getNetwork();
        if (net == null) {
            errors.put(keyConcatenate(basePrefix, "network"), "Network configuration is required");
        } else {
            if (!BasicUtil.validString(net.getBaseUrl())) {
                errors.put(keyConcatenate(basePrefix, "network.baseUrl"), "Base URL is compulsory for endpoint configuration");
            }
            if (net.getTimeoutMillis() <= 0) {
                errors.put(keyConcatenate(basePrefix, "network.timeoutMillis"), "Timeout must be a positive number");
            }
            if (net.isUseProxy()) {
                if (net.getProxy() == null) {
                    errors.put(keyConcatenate(basePrefix, "network.proxy"), "Proxy configuration must be provided when proxy is enabled");
                } else {
                    errors.putAll(validateProxyConfig(net.getProxy(), keyConcatenate(basePrefix, "network.proxy")));
                }
            }
        }

        // --- Endpoints validation ---
        if (config.getEndpoints() == null || config.getEndpoints().isEmpty()) {
            errors.put(keyConcatenate(basePrefix, "endpoints"), "At least one endpoint detail must be provided");
        } else {
            for (EndpointDetail endpointDetail : config.getEndpoints()) {
                String opPrefix = keyConcatenate(basePrefix, "endpoints.");
                errors.putAll(validateEndpointDetail(endpointDetail, opPrefix));
            }
        }

        // --- Resilience config ---
        ResilienceConfig resilience = config.getResilience();
        if (resilience != null && resilience.getFallback() != null) {
            FallbackConfig fb = resilience.getFallback();
            if (fb.getType() != null) {
                switch (fb.getType()) {
                    case STATIC_RESPONSE -> {
                        String val = fallbackValues.get(FallbackConfig.FallbackType.STATIC_RESPONSE);
                        if (!BasicUtil.validString(val)) {
                            errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Static response JSON cannot be empty");
                        } else {
                            try {
                                new ObjectMapper().readTree(val);
                                fb.setValue(val);
                            } catch (Exception e) {
                                errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Static response must be valid JSON");
                            }
                        }
                    }
                    case REDIRECT_ENDPOINT -> {
                        String endpoint = fallbackValues.get(FallbackConfig.FallbackType.REDIRECT_ENDPOINT);
                        if (!BasicUtil.validString(endpoint)) {
                            errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Redirect endpoint URL must be specified");
                        } else {
                            try {
                                new java.net.URL(endpoint);
                                fb.setValue(endpoint);
                            } catch (Exception e) {
                                errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Redirect endpoint must be a valid URL");
                            }
                        }
                    }
                    case EXCEPTION -> {
                        String msg = fallbackValues.get(FallbackConfig.FallbackType.EXCEPTION);
                        if (!BasicUtil.validString(msg)) {
                            errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Exception message is required");
                        } else {
                            fb.setValue(msg);
                        }
                    }
                }
            }
        }

        return errors;
    }

    private static Map<String, String> validateProxyConfig(ProxyConfig proxy, String basePrefix) {
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

    private static Map<String, String> validateEndpointDetail(EndpointDetail detail, String basePrefix) {
        Map<String, String> errors = new HashMap<>();

        if(null == detail.getOperationType()){
            errors.put(keyConcatenate(basePrefix, "operationType"), "Operation type must not be blank");
        }
        if (!BasicUtil.validString(detail.getUrl())) {
            errors.put(keyConcatenate(basePrefix, "url"), "URL must not be blank");
        }
        if (detail.getMethod() == null) {
            errors.put(keyConcatenate(basePrefix, "method"), "HTTP method must be specified (GET, POST, PUT, DELETE)");
        }
        if (detail.getHeaders() != null) {
            for (int i = 0; i < detail.getHeaders().size(); i++) {
                StaticHeader header = detail.getHeaders().get(i);
                if (!BasicUtil.validString(header.getName())) {
                    errors.put(keyConcatenate(basePrefix, "headers[" + i + "].name"), "Header name must not be blank");
                }
            }
        }
        if (detail.getDynamicHeaders() != null) {
            for (int i = 0; i < detail.getDynamicHeaders().size(); i++) {
                DynamicHeader header = detail.getDynamicHeaders().get(i);
                if (!BasicUtil.validString(header.getName())) {
                    errors.put(keyConcatenate(basePrefix, "dynamicHeaders[" + i + "].name"), "Dynamic header name must not be blank");
                }
            }
        }
        return errors;
    }


    public static Form<EndpointConfig> validateEndpointConfig(
            Form<EndpointConfig> configForm,
            Map<String, Object> metadata
    ) {
        Map<String, String> errors = new HashMap<>();

        EndpointConfig config = configForm.get();

        // Domain checks
        if (!BasicUtil.validString(config.getDomainOwnerCode())) {
            configForm = configForm.withError("domainOwnerCode", "Domain owner code is required for endpoint");

        }
        if (config.getDomainOwnerType() == null) {
            configForm = configForm.withError("domainOwnerType", "Domain owner type is required for endpoint");

        }

        if (config.getDomainOwnerId() == null) {
            configForm = configForm.withError("domainOwnerId", "Domain owner id is required for endpoint");

        }

        // --- Network validation ---
//        NetworkConfig net = config.getNetwork();
//        if (net == null) {
//            errors.put(keyConcatenate(basePrefix, "network"), "Network configuration is required");
//        } else {
//            if (!BasicUtil.validString(net.getBaseUrl())) {
//                errors.put(keyConcatenate(basePrefix, "network.baseUrl"), "Base URL is compulsory for endpoint configuration");
//            }
//            if (net.getTimeoutMillis() <= 0) {
//                errors.put(keyConcatenate(basePrefix, "network.timeoutMillis"), "Timeout must be a positive number");
//            }
//            if (net.isUseProxy()) {
//                if (net.getProxy() == null) {
//                    errors.put(keyConcatenate(basePrefix, "network.proxy"), "Proxy configuration must be provided when proxy is enabled");
//                } else {
//                    errors.putAll(validateProxyConfig(net.getProxy(), keyConcatenate(basePrefix, "network.proxy")));
//                }
//            }
//        }
//
//        // --- Endpoints validation ---
//        if (config.getEndpoints() == null || config.getEndpoints().isEmpty()) {
//            errors.put(keyConcatenate(basePrefix, "endpoints"), "At least one endpoint detail must be provided");
//        } else {
//            for (EndpointDetail endpointDetail : config.getEndpoints()) {
//                String opPrefix = keyConcatenate(basePrefix, "endpoints.");
//                errors.putAll(validateEndpointDetail(endpointDetail, opPrefix));
//            }
//        }
//
//        // --- Resilience config ---
//        ResilienceConfig resilience = config.getResilience();
//        if (resilience != null && resilience.getFallback() != null) {
//            FallbackConfig fb = resilience.getFallback();
//            if (fb.getType() != null) {
//                switch (fb.getType()) {
//                    case STATIC_RESPONSE -> {
//                        String val = fallbackValues.get(FallbackConfig.FallbackType.STATIC_RESPONSE);
//                        if (!BasicUtil.validString(val)) {
//                            errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Static response JSON cannot be empty");
//                        } else {
//                            try {
//                                new ObjectMapper().readTree(val);
//                                fb.setValue(val);
//                            } catch (Exception e) {
//                                errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Static response must be valid JSON");
//                            }
//                        }
//                    }
//                    case REDIRECT_ENDPOINT -> {
//                        String endpoint = fallbackValues.get(FallbackConfig.FallbackType.REDIRECT_ENDPOINT);
//                        if (!BasicUtil.validString(endpoint)) {
//                            errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Redirect endpoint URL must be specified");
//                        } else {
//                            try {
//                                new java.net.URL(endpoint);
//                                fb.setValue(endpoint);
//                            } catch (Exception e) {
//                                errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Redirect endpoint must be a valid URL");
//                            }
//                        }
//                    }
//                    case EXCEPTION -> {
//                        String msg = fallbackValues.get(FallbackConfig.FallbackType.EXCEPTION);
//                        if (!BasicUtil.validString(msg)) {
//                            errors.put(keyConcatenate(basePrefix, "resilience.fallback.value"), "Exception message is required");
//                        } else {
//                            fb.setValue(msg);
//                        }
//                    }
//                }
//            }
//        }
//
//        return errors;

        return null;
    }

    public static Form<CustomerUser> validateCustomerUser(Form<CustomerUser> customerUserForm, CustomerUserService customerUserService){
        //todo: do app level validation here
        return customerUserForm;
    }


}
