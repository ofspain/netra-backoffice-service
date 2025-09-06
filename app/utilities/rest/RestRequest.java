package utilities.rest;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;

public class RestRequest {
    private String url;
    private HttpMethod method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Object body;
    private int timeout;
    private boolean retryOnFailure;

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }

    private RestRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.queryParams = builder.queryParams;
        this.body = builder.body;
        this.timeout = builder.timeout;
        this.retryOnFailure = builder.retryOnFailure;
    }

    // Builder pattern
    public static class Builder {
        private String url;
        private HttpMethod method = HttpMethod.GET;
        private Map<String, String> headers = new HashMap<>();
        private Map<String, String> queryParams = new HashMap<>();
        private Object body;
        private int timeout;
        private boolean retryOnFailure = true;

        public Builder(String url) {
            this.url = url;
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder queryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams.putAll(queryParams);
            return this;
        }

        public Builder body(Object body) {
            this.body = body;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retryOnFailure(boolean retryOnFailure) {
            this.retryOnFailure = retryOnFailure;
            return this;
        }

        public RestRequest build() {
            return new RestRequest(this);
        }
    }

    // Getters
    public String getUrl() { return url; }
    public HttpMethod getMethod() { return method; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public Object getBody() { return body; }
    public int getTimeout() { return timeout; }
    public boolean isRetryOnFailure() { return retryOnFailure; }
}
