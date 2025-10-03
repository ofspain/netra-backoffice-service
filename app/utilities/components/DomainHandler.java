package utilities.components;

import com.netra.commons.contracts.Domain;
import play.mvc.Result;

import java.time.LocalDateTime;

public interface DomainHandler<T extends Domain> {
    T resolve(Long domainId);
    Result redirectToView(T domain);

    default LocalDateTime getCreatedAt(T domain) { return null; }
    default LocalDateTime getUpdatedAt(T domain) { return null; }
    default String getDomainCode(T domain) { return domain.getDomainCode(); }
}

