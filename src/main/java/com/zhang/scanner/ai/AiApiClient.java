package com.zhang.scanner.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    // ==================== 流式聊天 ====================

    /**
     * 流式聊天回调
     * 注意: 三个回调都在后台线程触发, 调用方需自行 invokeLater 回 EDT
     */
    public interface StreamCallback {
        /**
         * 增量文本
         */
        void onDelta(String text);

        /**
         * 正常结束(含用户主动取消)
         */
        void onDone();

        /**
         * 失败(友好文案)
         */
        void onError(String friendlyMessage);
    }

    /**
     * 流式请求句柄, 用于取消
     */
    public static final class StreamRequest {

        private volatile boolean cancelled = false;
        private volatile InputStream stream;
        private volatile CompletableFuture<HttpResponse<InputStream>> future;

        /**
         * 停止生成: 取消发送中的请求并关闭底层输入流
         */
        public void cancel() {
            cancelled = true;
            InputStream s = stream;
            if (null != s) {
                try {
                    s.close();
                } catch (Exception ignored) {
                    // 忽略关闭异常
                }
            }
            CompletableFuture<HttpResponse<InputStream>> f = future;
            if (null != f) {
                f.cancel(true);
            }
        }

        private boolean isCancelled() {
            return cancelled;
        }

        private void setStream(InputStream stream) {
            this.stream = stream;
        }

        private void setFuture(CompletableFuture<HttpResponse<InputStream>> future) {
            this.future = future;
        }
    }

    /**
     * 发起流式聊天请求(立即返回, 内部起后台线程)
     *
     * @param apiUrl       完整 chat 端点
     * @param apiKey       API Key
     * @param model        模型名
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param callback     回调(后台线程触发)
     */
    public static StreamRequest startStream(String apiUrl, String apiKey, String model,
                                            String systemPrompt, String userPrompt, StreamCallback callback) {
        StreamRequest request = new StreamRequest();
        Thread thread = new Thread(() -> runStream(apiUrl, apiKey, model, systemPrompt, userPrompt, callback, request),
                "AskAI-Stream");
        thread.setDaemon(true);
        thread.start();
        return request;
    }

    /**
     * 后台线程: 发送请求并解析响应
     */
    private static void runStream(String apiUrl, String apiKey, String model,
                                  String systemPrompt, String userPrompt,
                                  StreamCallback callback, StreamRequest request) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    // 请求超时只约束到响应头到达(JDK 语义), 流式 body 读取不受限;
                    // 防止故障代理接受连接后永不返回响应头导致线程永久挂起
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(buildChatBody(model, systemPrompt, userPrompt)))
                    .build();

            // sendAsync: 用户 Stop 时 future.cancel(true) 可立即中断 send 阻塞
            CompletableFuture<HttpResponse<InputStream>> future =
                    client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            request.setFuture(future);
            HttpResponse<InputStream> response = future.get();

            if (response.statusCode() != 200) {
                callback.onError(readError(response));
                return;
            }

            InputStream input = response.body();
            request.setStream(input);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                parseSseOrFallback(reader, callback);
                callback.onDone();
            }
        } catch (Exception ex) {
            if (request.isCancelled()) {
                // 用户主动停止, 视为正常结束
                callback.onDone();
            } else {
                callback.onError("Request failed: " + ex.getMessage());
            }
        }
    }

    /**
     * 组装 chat 请求体(stream=true, 关闭思考模式)
     */
    private static String buildChatBody(String model, String systemPrompt, String userPrompt) {
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", "disabled");

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("stream", true);
        body.add("thinking", thinking);
        return body.toString();
    }

    /**
     * 统一解析 SSE 流式响应; 若响应实为完整 JSON(代理不支持流式), 自动按非流式兜底
     */
    private static void parseSseOrFallback(BufferedReader reader, StreamCallback callback) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                String data = trimmed.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                String delta = extractStreamDelta(data);
                if (null != delta && !delta.isEmpty()) {
                    callback.onDelta(delta);
                }
                continue;
            }
            if (trimmed.startsWith("{")) {
                // 非 SSE 完整 JSON: 读入首行与后续所有行(兼容 pretty-printed 多行 JSON)后整体解析
                StringBuilder jsonBody = new StringBuilder(trimmed);
                String rest;
                while ((rest = reader.readLine()) != null) {
                    jsonBody.append('\n').append(rest);
                }
                String content = extractNonStreamContent(jsonBody.toString());
                if (!content.isEmpty()) {
                    callback.onDelta(content);
                }
                return;
            }
            // 其余行(如 "event:" 等)忽略
        }
    }

    /**
     * 从流式 chunk 中提取 delta.content
     */
    private static String extractStreamDelta(String data) {
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (null == choices || choices.isEmpty()) {
                return null;
            }
            JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
            if (null == delta || !delta.has("content")) {
                return null;
            }
            JsonElement content = delta.get("content");
            return content.isJsonNull() ? null : content.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 从非流式完整响应中提取 message.content
     */
    private static String extractNonStreamContent(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (null == choices || choices.isEmpty()) {
                return "";
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (null == message || !message.has("content")) {
                return "";
            }
            JsonElement content = message.get("content");
            return content.isJsonNull() ? "" : content.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * 读取非 200 响应的错误信息
     */
    private static String readError(HttpResponse<InputStream> response) {
        try (InputStream in = response.body()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return extractErrorMessage(response.statusCode(), body);
        } catch (Exception e) {
            return "HTTP " + response.statusCode();
        }
    }
}
