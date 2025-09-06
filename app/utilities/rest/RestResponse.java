package utilities.rest;


import com.fasterxml.jackson.databind.JsonNode;

public class RestResponse {
    private int statusCode;
    private String statusText;
    private String body;
    private JsonNode jsonBody;
    private boolean success;

    public RestResponse(int statusCode, String statusText, String body, JsonNode jsonBody) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.body = body;
        this.jsonBody = jsonBody;
        this.success = statusCode >= 200 && statusCode < 300;
    }

    // Getters
    public int getStatusCode() { return statusCode; }
    public String getStatusText() { return statusText; }
    public String getBody() { return body; }
    public JsonNode getJsonBody() { return jsonBody; }
    public boolean isSuccess() { return success; }

    public <T> T getBodyAs(Class<T> clazz) {
        // This would use Jackson to deserialize the body
        // Implementation depends on your JSON library
        return null;
    }
}