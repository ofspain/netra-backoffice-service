package utilities.dto;

import java.time.LocalDateTime;


public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private LocalDateTime issuedAt;
    private String refreshToken;

    // --- No-args constructor (required for Jackson deserialization) ---
    public AuthResponse() {
    }

    // --- All-args constructor (for manual creation) ---
    public AuthResponse(String accessToken,
                        String tokenType,
                        long expiresIn,
                        LocalDateTime issuedAt,
                        String refreshToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.issuedAt = issuedAt;
        this.refreshToken = refreshToken;
    }

    // --- Getters & Setters ---
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // --- Optional: for logging/debugging ---
    @Override
    public String toString() {
        return "AuthResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", issuedAt=" + issuedAt +
                ", refreshToken='" + refreshToken + '\'' +
                '}';
    }
}
