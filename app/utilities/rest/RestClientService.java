package utilities.rest;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Singleton
public class RestClientService {

    private final WSClient wsClient;
    private final RestClientConfig config;

    @Inject
    public RestClientService(WSClient wsClient, RestClientConfig config) {
        this.wsClient = wsClient;
        this.config = config;
    }

    public CompletionStage<RestResponse> execute(RestRequest request) {
        return executeWithRetry(request, 0);
    }

    private CompletionStage<RestResponse> executeWithRetry(RestRequest request, int attempt) {
        WSRequest wsRequest = buildRequest(request);

        // 👇 Log curl representation (optional, or use your logger)
        String curlCmd = generateCurlCommand(request);
        System.out.println("[REST DEBUG] Attempt " + (attempt) + ": " + curlCmd);


        return wsRequest.execute(request.getMethod().name())
                .thenApply(this::convertResponse)
                .exceptionally(ex -> new RestResponse(0, "Request failed: " + ex.getMessage(), null, null))
                .thenCompose(response -> {
                    if (shouldRetry(response, attempt, request.isRetryOnFailure())) {
                        return executeWithRetry(request, attempt + 1);
                    }
                    return CompletableFuture.completedFuture(response);
                });
    }

    private WSRequest buildRequest(RestRequest request) {
        WSRequest wsRequest = wsClient.url(request.getUrl());

        // Set timeout
        int timeout = request.getTimeout() > 0 ? request.getTimeout() : config.getDefaultTimeout();
        wsRequest.setRequestTimeout(timeout);

        // Add headers
        if (request.getHeaders() != null) {
            request.getHeaders().forEach(wsRequest::addHeader);
        }

        // Add query parameters
        if (request.getQueryParams() != null) {
            request.getQueryParams().forEach(wsRequest::addQueryParameter);
        }

        // Set body for appropriate methods
        if (request.getBody() != null &&
                (request.getMethod() == RestRequest.HttpMethod.POST ||
                        request.getMethod() == RestRequest.HttpMethod.PUT ||
                        request.getMethod() == RestRequest.HttpMethod.PATCH)) {

            if (request.getBody() instanceof String) {
                wsRequest.setBody((String) request.getBody());
            } else if (request.getBody() instanceof JsonNode) {
                wsRequest.setBody((JsonNode) request.getBody());
            } else {
                wsRequest.setBody(Json.toJson(request.getBody()));
            }
        }

        return wsRequest;
    }

    private RestResponse convertResponse(WSResponse wsResponse) {
        String body = wsResponse.getBody();
        JsonNode jsonBody = null;

        try {
            jsonBody = Json.parse(body);
        } catch (Exception e) {
            // Not JSON, keep body as string
        }

        return new RestResponse(
                wsResponse.getStatus(),
                wsResponse.getStatusText(),
                body,
                jsonBody
        );
    }

    private boolean shouldRetry(RestResponse response, int attempt, boolean retryEnabled) {
        return retryEnabled &&
                attempt < config.getMaxRetries() &&
                (response.getStatusCode() >= 500 || response.getStatusCode() == 0);
    }

    // Convenience methods
    public CompletionStage<RestResponse> get(String url) {
        return execute(new RestRequest.Builder(url).method(RestRequest.HttpMethod.GET).build());
    }

    public CompletionStage<RestResponse> post(String url, Object body) {
        return execute(new RestRequest.Builder(url).method(RestRequest.HttpMethod.POST).body(body).build());
    }

    public CompletionStage<RestResponse> put(String url, Object body) {
        return execute(new RestRequest.Builder(url).method(RestRequest.HttpMethod.PUT).body(body).build());
    }

    public CompletionStage<RestResponse> delete(String url) {
        return execute(new RestRequest.Builder(url).method(RestRequest.HttpMethod.DELETE).build());
    }


    private String generateCurlCommand(RestRequest request) {
        StringBuilder curl = new StringBuilder("curl -X ");
        curl.append(request.getMethod().name()).append(" '").append(request.getUrl());

        // Add query params
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            String queryString = request.getQueryParams().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .map(q -> "?" + q)
                    .orElse("");
            curl.append(queryString);
        }

        curl.append("'");

        // Add headers
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                curl.append(" -H '").append(header.getKey()).append(": ").append(header.getValue()).append("'");
            }
        }

        // Add body (if applicable)
        if (request.getBody() != null &&
                (request.getMethod() == RestRequest.HttpMethod.POST ||
                        request.getMethod() == RestRequest.HttpMethod.PUT ||
                        request.getMethod() == RestRequest.HttpMethod.PATCH)) {

            String bodyString;
            if (request.getBody() instanceof String) {
                bodyString = (String) request.getBody();
            } else if (request.getBody() instanceof JsonNode) {
                bodyString = request.getBody().toString();
            } else {
                bodyString = Json.toJson(request.getBody()).toString();
            }

            // Escape single quotes in body
            bodyString = bodyString.replace("'", "'\"'\"'");
            curl.append(" -d '").append(bodyString).append("'");
        }

        // Add timeout info (optional)
        int timeout = request.getTimeout() > 0 ? request.getTimeout() : config.getDefaultTimeout();
        curl.append(" --max-time ").append(timeout / 1000); // convert ms to seconds

        return curl.toString();
    }

}
