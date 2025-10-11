package utilities.dto;
import com.netra.commons.enums.DomainType;

import java.time.LocalDateTime;
import java.util.List;

public record LoginResult(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        String username,
        String issuer,
        String domainCode,
        DomainType domainType,
        String identityUuid,
        List<String> roles,
        LocalDateTime lastLogin,
        LocalDateTime lastPasswordChange
) {}

