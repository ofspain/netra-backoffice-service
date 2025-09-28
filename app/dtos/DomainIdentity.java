package dtos;

import com.netra.commons.util.BasicUtil;

import java.time.LocalDateTime;

public record DomainIdentity(
        String domainOwnerType, Long domainOwnerId,LocalDateTime domainUpdateLastAt,
        String domainOwnerCode, LocalDateTime domainCreatedAt
        ) {

    public String hashIDForURL(){
        return BasicUtil.encodeUrlBoundId(domainOwnerId);
    }

    public String uriFriendlyDomainTypeName(){
        return BasicUtil.encodeURLBoundString(domainOwnerType);
    }
}