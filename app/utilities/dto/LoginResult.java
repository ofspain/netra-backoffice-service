package utilities.dto;
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
        String domainType,
        String identityUuid,
        List<String> roles,
        LocalDateTime lastLogin,
        LocalDateTime lastPasswordChange
) {}

