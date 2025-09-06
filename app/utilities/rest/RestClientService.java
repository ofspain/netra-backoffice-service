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
}
