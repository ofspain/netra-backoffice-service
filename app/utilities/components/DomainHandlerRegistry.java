package utilities.components;

import com.netra.commons.contracts.Domain;
import com.netra.commons.enums.DomainType;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class DomainHandlerRegistry {

    private final Map<DomainType, DomainHandler<? extends Domain>> handlers = new HashMap<>();

    @Inject
    public DomainHandlerRegistry(FinancialInstitutionHandler financialInstitutionHandler) {
        handlers.put(DomainType.FINANCIAL_INSTITUTION, financialInstitutionHandler);
        // add more as needed...
    }

    @SuppressWarnings("unchecked")
    public <T extends Domain> DomainHandler<T> getHandler(DomainType domainType) {
        DomainHandler<?> handler = handlers.get(domainType);
        if (handler == null) {
            throw new RuntimeException("No handler registered for domain type: " + domainType);
        }
        return (DomainHandler<T>) handler;
    }
}

