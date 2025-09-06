package security;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utilities.rest.RestClientService;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@UtilityClass
public class PublicKeyExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyExtractor.class);


    /**
     * Fetches RSA public key from upstream authentication server
     */
    protected RSAPublicKey fetchPublicKeyFromUpstream(RestClientService restClientService, String publicKeyUrl) {
        try {

            LOGGER.info("Fetching public key from: {}", publicKeyUrl);

            // Use the REST client to fetch the public key
            var response = restClientService.get(publicKeyUrl)
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS); // 10 second timeout for key fetching

            if (!response.isSuccess()) {
                throw new IllegalStateException("Failed to fetch public key. Status: " +
                        response.getStatusCode() + " - " + response.getStatusText());
            }

            String publicKeyPem = extractPublicKeyFromResponse(response.getBody());
            return loadRSAPublicKeyFromString(publicKeyPem);

        } catch (TimeoutException e) {
            LOGGER.error("Timeout while fetching public key from upstream server", e);
            throw new IllegalStateException("Timeout fetching public key from authentication server", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while fetching public key", e);
            throw new IllegalStateException("Interrupted while fetching public key", e);
        } catch (ExecutionException e) {
            LOGGER.error("Execution error while fetching public key", e);
            throw new IllegalStateException("Failed to fetch public key from authentication server", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while fetching public key", e);
            throw new IllegalStateException("Failed to initialize security configuration", e);
        }
    }

    /**
     * Extracts public key from various response formats
     */
    private String extractPublicKeyFromResponse(String responseBody) {
        // Try to parse as JSON first
        try {
            // If the response is JSON, look for common key fields
            if (responseBody.trim().startsWith("{")) {
                // Simple JSON parsing - you might want to use Jackson for more robust parsing
                if (responseBody.contains("\"publicKey\"")) {
                    return responseBody.split("\"publicKey\"\\s*:")[1]
                            .split("\"")[1]
                            .replace("\\n", "\n")
                            .replace("\\r", "");
                } else if (responseBody.contains("\"key\"")) {
                    return responseBody.split("\"key\"\\s*:")[1]
                            .split("\"")[1]
                            .replace("\\n", "\n")
                            .replace("\\r", "");
                }
            }

            // If not JSON or key not found in JSON, assume it's the raw PEM
            return responseBody.trim();

        } catch (Exception e) {
            LOGGER.warn("Failed to parse public key response as JSON, treating as raw PEM", e);
            return responseBody.trim();
        }
    }

    /**
     * Loads RSA public key from PEM string
     */
    private RSAPublicKey loadRSAPublicKeyFromString(String publicKeyPem) {
        try {
            // Clean up the PEM string
            String cleanPem = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] encoded = Base64.getDecoder().decode(cleanPem);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            LOGGER.error("Failed to load RSA public key from PEM string", e);
            throw new IllegalStateException("Invalid RSA public key format", e);
        }
    }
}


