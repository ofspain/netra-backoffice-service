package utilities.rest;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

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

    // ✅ Generic JSON body deserializer
    public <T> T getBodyAs(TypeReference<T> typeRef) {
        try {
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("Response body is empty");
            }
            return Json.mapper().readValue(body, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body", e);
        }
    }

    // Convenience overload for simple classes
    public <T> T getBodyDataAs(Class<T> clazz, String dataRootPath) {
        try {
            JsonNode root = this.jsonBody;
            if (root == null || !root.has(dataRootPath)) {
                throw new IllegalStateException("No 'data' field in response");
            }
            return Json.mapper().treeToValue(root.get(dataRootPath), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response 'data' field", e);
        }
    }
}