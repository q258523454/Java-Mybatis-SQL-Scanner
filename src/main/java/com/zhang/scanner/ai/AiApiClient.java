package com.zhang.scanner.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek / OpenAI 兼容 API 客户端
 * HTTP 使用 JDK 自带 HttpClient, JSON 使用 IDE 平台自带 Gson
 */
public final class AiApiClient {

    /**
     * 连接超时
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private AiApiClient() {
    }

    /**
     * 连接测试结果
     */
    public static class TestResult {
        public final boolean ok;
        public final String message;

        public TestResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    /**
     * 测试连接是否可用(同步阻塞, 调用方必须在后台线程执行)
     * 优先 GET {base}/models; 该端点不可用(404/405)时降级为一次非流式极小 chat 请求
     */
    public static TestResult testConnection(String apiUrl, String apiKey, String model) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(deriveModelsUri(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return new TestResult(true, buildModelsMessage(parseModelIds(response.body())));
            }
            if (response.statusCode() == 404 || response.statusCode() == 405) {
                // 该代理不支持 /models 端点, 降级为极小 chat 请求验证
                return testWithMiniChat(client, apiUrl, apiKey, model);
            }
            // 401/403/5xx 等错误
            return new TestResult(false, extractErrorMessage(response.statusCode(), response.body()));
        } catch (Exception ex) {
            return new TestResult(false, "Connection failed: " + ex.getMessage());
        }
    }

    /**
     * 由完整 chat 端点推导 /models 端点:
     * https://host/v1/chat/completions -> https://host/v1/models
     */
    private static URI deriveModelsUri(String apiUrl) throws Exception {
        URI uri = URI.create(apiUrl);
        String path = uri.getPath();
        int idx = path.indexOf("/v1");
        String basePath;
        if (idx >= 0) {
            // 取到 /v1 为止(含)
            basePath = path.substring(0, idx + 3);
        } else {
            basePath = "/";
        }
        String modelsPath = basePath.endsWith("/") ? basePath + "models" : basePath + "/models";
        return new URI(uri.getScheme(), uri.getAuthority(), modelsPath, null, null);
    }

    /**
     * 解析 GET /models 响应中的模型 id 列表
     */
    private static List<String> parseModelIds(String body) {
        List<String> ids = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");
            if (null != data) {
                for (JsonElement el : data) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("id")) {
                        ids.add(obj.get("id").getAsString());
                    }
                }
            }
        } catch (Exception ignored) {
            // 响应体格式异常, 视为无模型列表
        }
        return ids;
    }

    /**
     * 拼接可用模型列表文案(最多显示 3 个)
     */
    private static String buildModelsMessage(List<String> modelIds) {
        if (modelIds.isEmpty()) {
            return "Connection successful";
        }
        StringBuilder sb = new StringBuilder("Connection successful. Models: ");
        for (int i = 0; i < modelIds.size(); i++) {
            if (i >= 3) {
                sb.append(", ...");
                break;
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(modelIds.get(i));
        }
        return sb.toString();
    }

    /**
     * 降级验证: 非流式极小 chat 请求
     */
    private static TestResult testWithMiniChat(HttpClient client, String apiUrl, String apiKey, String model) {
        try {
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", "ping");
            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.add("messages", messages);
            body.addProperty("stream", false);
            body.addProperty("max_tokens", 1);

            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return new TestResult(true, "Connection successful");
            }
            return new TestResult(false, extractErrorMessage(response.statusCode(), response.body()));
        } catch (Exception ex) {
            return new TestResult(false, "Connection failed: " + ex.getMessage());
        }
    }

    /**
     * 从错误响应体中提取 error.message
     */
    private static String extractErrorMessage(int statusCode, String body) {
        String detail = "";
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                if (error.has("message")) {
                    detail = error.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            // 响应体非 JSON 或结构不同
        }
        if (detail.isEmpty()) {
            return "HTTP " + statusCode;
        }
        return "HTTP " + statusCode + ": " + detail;
    }
}
