package com.zhang.scanner.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zhang.scanner.settings.ai.AiProviderRegistry;
import com.zhang.scanner.utils.LanguageUtil;

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
 * AI 模型 API 客户端, 支持两种协议:
 * OpenAI 兼容(chat/completions + Bearer)与 Anthropic Messages(x-api-key + /v1/messages)
 * HTTP 使用 JDK 自带 HttpClient, JSON 使用 IDE 平台自带 Gson
 */
public final class AiApiClient {

    /**
     * 连接超时
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Anthropic 协议版本头(2026-10-21, 官方当前版本)
     */
    private static final String ANTHROPIC_VERSION = "2026-10-21";

    /**
     * Claude 请求 max_tokens(官方必填; 流式分析场景 8192 足够)
     */
    private static final int CLAUDE_MAX_TOKENS = 8192;

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
     * 用当前 model 发送一次非流式极小 chat 请求真实验证 URL/Key/Model 三者;
     * model 不可用但 Key 有效时, 再查询 /models 给出可用模型列表提示
     *
     * @param protocol     协议类型(OpenAI 兼容用 Bearer 鉴权, Anthropic 用 x-api-key)
     * @param thinkingMode thinking 参数策略(极小请求同样按策略组装, 如 GLM-5.x 强制思考)
     */
    public static TestResult testConnection(String apiUrl, String apiKey, String model,
                                            AiProviderRegistry.Protocol protocol,
                                            AiProviderRegistry.ThinkingMode thinkingMode) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            // 1. 真实调用当前 model(极小请求): 模型名无效时此处会失败
            TestResult mini = testWithMiniChat(client, apiUrl, apiKey, model, protocol, thinkingMode);
            if (mini.ok) {
                return new TestResult(true, LanguageUtil.zhOrEn(
                        "Connection successful (model: " + model + ")",
                        "连接成功 (模型: " + model + ")"));
            }
            // 2. 鉴权错误(401/403)直接返回, Key 本身无效
            if (mini.message.startsWith("HTTP 401") || mini.message.startsWith("HTTP 403")) {
                return mini;
            }
            // 3. 其他失败: 查询 /models 区分"Key 有效但模型无效"并给出可用模型列表
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(deriveModelsUri(apiUrl))
                        .timeout(Duration.ofSeconds(15))
                        .GET();
                applyAuth(builder, apiKey, protocol);
                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<String> ids = parseModelIds(response.body());
                    String models = ids.isEmpty() ? "" : " " + LanguageUtil.zhOrEn(
                            "Available models: ", "可用模型: ") + String.join(", ", ids);
                    return new TestResult(false, LanguageUtil.zhOrEn(
                            "API Key is valid, but model \"" + model + "\" is unavailable: " + mini.message,
                            "API Key 有效, 但模型 \"" + model + "\" 不可用: " + mini.message) + models);
                }
            } catch (Exception ignored) {
                // /models 查询失败, 按 mini chat 错误返回
            }
            return mini;
        } catch (Exception ex) {
            return new TestResult(false, "Connection failed: " + ex.getMessage());
        }
    }

    /**
     * 按协议给请求附加鉴权头:
     * OpenAI 兼容 → Authorization: Bearer; Anthropic → x-api-key + anthropic-version
     */
    private static HttpRequest.Builder applyAuth(HttpRequest.Builder builder, String apiKey,
                                                 AiProviderRegistry.Protocol protocol) {
        if (protocol == AiProviderRegistry.Protocol.ANTHROPIC) {
            builder.header("x-api-key", apiKey);
            builder.header("anthropic-version", ANTHROPIC_VERSION);
        } else {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder;
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
     * 降级验证: 非流式极小 chat 请求(按协议与 thinking 策略组装)
     */
    private static TestResult testWithMiniChat(HttpClient client, String apiUrl, String apiKey, String model,
                                               AiProviderRegistry.Protocol protocol,
                                               AiProviderRegistry.ThinkingMode thinkingMode) {
        try {
            String body = buildChatBody(model, "", "ping", protocol, thinkingMode, false, 1);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            applyAuth(builder, apiKey, protocol);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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
         * 增量思考内容(思考型模型流式输出 reasoning_content, 供界面显示思考进度;
         * 非思考型模型不触发)
         */
        default void onThinking(String text) {
        }

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
     * @param protocol     协议类型(OpenAI 兼容 / Anthropic)
     * @param thinkingMode thinking 参数策略(各家差异很大, 由供应商预设决定)
     * @param callback     回调(后台线程触发)
     */
    public static StreamRequest startStream(String apiUrl, String apiKey, String model,
                                            String systemPrompt, String userPrompt,
                                            AiProviderRegistry.Protocol protocol,
                                            AiProviderRegistry.ThinkingMode thinkingMode,
                                            StreamCallback callback) {
        StreamRequest request = new StreamRequest();
        Thread thread = new Thread(() -> runStream(apiUrl, apiKey, model, systemPrompt, userPrompt,
                        protocol, thinkingMode, callback, request),
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
                                  AiProviderRegistry.Protocol protocol,
                                  AiProviderRegistry.ThinkingMode thinkingMode,
                                  StreamCallback callback, StreamRequest request) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            String body = buildChatBody(model, systemPrompt, userPrompt, protocol, thinkingMode, true, null);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    // 请求超时只约束到响应头到达(JDK 语义), 流式 body 读取不受限;
                    // 防止故障代理接受连接后永不返回响应头导致线程永久挂起
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            applyAuth(builder, apiKey, protocol);

            // sendAsync: 用户 Stop 时 future.cancel(true) 可立即中断 send 阻塞
            CompletableFuture<HttpResponse<InputStream>> future =
                    client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            request.setFuture(future);
            HttpResponse<InputStream> response = future.get();

            if (response.statusCode() != 200) {
                callback.onError(readError(response));
                return;
            }

            InputStream input = response.body();
            request.setStream(input);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                if (protocol == AiProviderRegistry.Protocol.ANTHROPIC) {
                    parseAnthropicSse(reader, callback);
                } else {
                    parseSseOrFallback(reader, callback);
                }
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
     * 组装 chat 请求体(按协议)
     *
     * @param stream       是否流式
     * @param maxTokens    最大输出 token(null 时 OpenAI 兼容协议不设置, Anthropic 用 CLAUDE_MAX_TOKENS)
     */
    private static String buildChatBody(String model, String systemPrompt, String userPrompt,
                                        AiProviderRegistry.Protocol protocol,
                                        AiProviderRegistry.ThinkingMode thinkingMode,
                                        boolean stream, Integer maxTokens) {
        if (protocol == AiProviderRegistry.Protocol.ANTHROPIC) {
            // Anthropic Messages: system 为顶层字段, max_tokens 必填
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("max_tokens", null == maxTokens ? CLAUDE_MAX_TOKENS : maxTokens);
            if (null != systemPrompt && !systemPrompt.isEmpty()) {
                body.addProperty("system", systemPrompt);
            }
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userPrompt);
            JsonArray messages = new JsonArray();
            messages.add(userMsg);
            body.add("messages", messages);
            body.addProperty("stream", stream);
            return body.toString();
        }

        // OpenAI 兼容协议
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("stream", stream);
        if (null != maxTokens) {
            body.addProperty("max_tokens", maxTokens);
        }
        // thinking 参数策略: DISABLED=关闭思考, ENABLED_LOW=开启+低强度(GLM-5.x 强制), QWEN_OFF=Qwen 关闭思考, NONE=不发
        if (thinkingMode == AiProviderRegistry.ThinkingMode.DISABLED) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "disabled");
            body.add("thinking", thinking);
        } else if (thinkingMode == AiProviderRegistry.ThinkingMode.ENABLED_LOW) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "enabled");
            thinking.addProperty("reasoning_effort", "low");
            body.add("thinking", thinking);
        } else if (thinkingMode == AiProviderRegistry.ThinkingMode.QWEN_OFF) {
            // Qwen 混合思考模型默认开启思考, 显式关闭显著降低首字延迟与费用
            body.addProperty("enable_thinking", false);
        }
        return body.toString();
    }

    /**
     * 解析 Anthropic Messages 流式响应(SSE):
     * 仅取 content_block_delta 事件的 delta.text; 其余事件(content_block_start/message_delta/message_stop 等)忽略
     */
    private static void parseAnthropicSse(BufferedReader reader, StreamCallback callback) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String data = trimmed.substring(5).trim();
            if ("[DONE]".equals(data)) {
                break;
            }
            try {
                JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                String type = getJsonString(json, "type");
                if ("content_block_delta".equals(type)) {
                    // 文本增量与思考增量都嵌套在 content_block_delta 事件里, 按 delta.type 区分
                    JsonObject delta = json.getAsJsonObject("delta");
                    if (null == delta) {
                        continue;
                    }
                    String deltaType = getJsonString(delta, "type");
                    if ("text_delta".equals(deltaType)) {
                        JsonElement text = delta.get("text");
                        if (null != text && !text.isJsonNull()) {
                            callback.onDelta(text.getAsString());
                        }
                    } else if ("thinking_delta".equals(deltaType)) {
                        // Claude 思考块增量(自适应思考默认开启): 供界面显示思考进度
                        JsonElement thinking = delta.get("thinking");
                        if (null != thinking && !thinking.isJsonNull()
                                && !thinking.getAsString().isEmpty()) {
                            callback.onThinking(thinking.getAsString());
                        }
                    }
                    // signature_delta 等其他 delta 类型忽略
                } else if ("error".equals(type)) {
                    // 流中出现 error 事件(官方允许 200 后携带 error)
                    JsonObject error = json.getAsJsonObject("error");
                    callback.onError("Anthropic error: " + getJsonString(error, "message"));
                    return;
                }
                // message_start/content_block_start/content_block_stop/message_delta/message_stop/ping 忽略
            } catch (Exception ignored) {
                // 非 JSON 或结构不同, 忽略该行
            }
        }
    }

    /**
     * 安全读取 JSON 字符串字段(缺失或 null 时返回空串)
     */
    private static String getJsonString(JsonObject json, String key) {
        JsonElement el = json.get(key);
        return (null != el && !el.isJsonNull()) ? el.getAsString() : "";
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
                handleStreamChunk(data, callback);
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
     * 处理单个流式 chunk: delta.content → onDelta, delta.reasoning_content → onThinking
     * 思考型模型(如 GLM/Kimi)在思考阶段只有 reasoning_content、content 为空,
     * 思考内容通过 onThinking 供界面显示"思考中"进度, 避免用户误以为卡死
     */
    private static void handleStreamChunk(String data, StreamCallback callback) {
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (null == choices || choices.isEmpty()) {
                return;
            }
            JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
            if (null == delta) {
                return;
            }
            JsonElement content = delta.get("content");
            if (null != content && !content.isJsonNull()) {
                String text = content.getAsString();
                if (!text.isEmpty()) {
                    callback.onDelta(text);
                }
            }
            JsonElement reasoning = delta.get("reasoning_content");
            if (null != reasoning && !reasoning.isJsonNull()) {
                String text = reasoning.getAsString();
                if (!text.isEmpty()) {
                    callback.onThinking(text);
                }
            }
        } catch (Exception ignored) {
            // 非 JSON 或结构不同, 忽略该 chunk
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
