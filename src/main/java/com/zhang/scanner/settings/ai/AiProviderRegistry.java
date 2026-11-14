package com.zhang.scanner.settings.ai;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 供应商预设注册表(7 家主流供应商 + 自定义)
 * 设置页选择供应商后按预设自动填充官方 API URL 与默认模型, 用户可二次编辑
 * 数据基于 2026-11 各供应商官方文档查证
 */
public final class AiProviderRegistry {

    /**
     * API 协议类型
     */
    public enum Protocol {
        /**
         * OpenAI 兼容协议(chat/completions + Authorization Bearer)
         */
        OPENAI_COMPAT,
        /**
         * Anthropic Messages 协议(x-api-key + anthropic-version + /v1/messages)
         */
        ANTHROPIC
    }

    /**
     * thinking(思考模式)参数策略: 各家接口差异很大, 按供应商分别处理
     */
    public enum ThinkingMode {
        /**
         * 发 {"type":"disabled"} 关闭思考(DeepSeek 现状, 加快响应)
         */
        DISABLED,
        /**
         * 发 {"type":"enabled","reasoning_effort":"low"}(GLM-5.x 强制思考且无法关闭, 用最低思考强度)
         */
        ENABLED_LOW,
        /**
         * 发 {"enable_thinking": false}(Qwen 混合思考模型默认开启思考, 显式关闭显著降低延迟与费用)
         */
        QWEN_OFF,
        /**
         * 发 {"reasoning_effort": "low"}(GPT-6 系列默认 medium 会思考, 显式降低推理力度加速首字;
         * 官方支持 low, 但 none/minimal 会 400)
         */
        GPT_LOW,
        /**
         * 不发 thinking 字段(Claude 自适应思考保持默认, 思考过程由界面展示; Doubao/Kimi 保持默认行为)
         */
        NONE
    }

    /**
     * 供应商预设
     */
    public static final class Preset {
        public final String id;
        public final String displayNameEn;
        public final String displayNameZh;
        public final String defaultApiUrl;
        public final String defaultModel;
        public final Icon icon;
        public final List<String> recommendedModels;
        public final Protocol protocol;
        public final ThinkingMode thinkingMode;

        public Preset(String id, String displayNameEn, String displayNameZh,
                      String defaultApiUrl, String defaultModel, Icon icon,
                      Protocol protocol, ThinkingMode thinkingMode,
                      String... recommendedModels) {
            this.id = id;
            this.displayNameEn = displayNameEn;
            this.displayNameZh = displayNameZh;
            this.defaultApiUrl = defaultApiUrl;
            this.defaultModel = defaultModel;
            this.icon = icon;
            this.protocol = protocol;
            this.thinkingMode = thinkingMode;
            this.recommendedModels = Collections.unmodifiableList(Arrays.asList(recommendedModels));
        }
    }

    /**
     * 全部预设(设置页下拉框顺序); 图标为各家官方 logo(32x32 PNG, 位于 resources/icons/ai/)
     */
    public static final List<Preset> PRESETS = Arrays.asList(
            new Preset("deepseek", "DeepSeek", "DeepSeek",
                    "https://api.deepseek.com/v1/chat/completions", "deepseek-v4-pro",
                    icon("deepseek.png"),
                    Protocol.OPENAI_COMPAT, ThinkingMode.DISABLED,
                    "deepseek-v4-pro", "deepseek-flash"),
            new Preset("qwen", "Qwen", "通义千问 Qwen",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen3.7-plus",
                    icon("qwen.png"),
                    Protocol.OPENAI_COMPAT, ThinkingMode.QWEN_OFF,
                    "qwen3.7-plus", "qwen3.8-max", "qwen3.8-flash"),
            new Preset("doubao", "Doubao", "豆包 Doubao",
                    "https://ark.cn-beijing.volces.com/api/v3/chat/completions", "doubao-seed-2-1-pro-260915",
                    icon("doubao.png"),
                    Protocol.OPENAI_COMPAT, ThinkingMode.NONE,
                    "doubao-seed-2-1-pro-260915"),
            new Preset("glm", "GLM", "智谱 GLM",
                    "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-5.3",
                    icon("glm.png"),
                    Protocol.OPENAI_COMPAT, ThinkingMode.ENABLED_LOW,
                    "glm-5.3", "glm-5.3-flash"),
            new Preset("kimi", "Kimi", "Kimi",
                    "https://api.moonshot.cn/v1/chat/completions", "kimi-k3.1-pro",
                    icon("kimi.png"),
                    Protocol.OPENAI_COMPAT, ThinkingMode.NONE,
                    "kimi-k3.1-pro", "kimi-k3"),
            new Preset("gpt", "GPT", "OpenAI GPT",
                    "https://api.openai.com/v1/chat/completions", "gpt-6-astra",
                    icon("openai.png"),
                    Protocol.OPENAI_COMPAT, ThinkingMode.GPT_LOW,
                    "gpt-6-astra", "gpt-6.1-sol", "gpt-6-luna"),
            new Preset("claude", "Claude", "Anthropic Claude",
                    "https://api.anthropic.com/v1/messages", "claude-sonnet-5-5",
                    icon("anthropic.png"),
                    Protocol.ANTHROPIC, ThinkingMode.NONE,
                    "claude-sonnet-5-5", "claude-opus-5-5", "claude-haiku-5-5"),
            new Preset("custom", "Custom", "自定义",
                    "", "",
                    AllIcons.General.Settings,
                    Protocol.OPENAI_COMPAT, ThinkingMode.NONE)
    );

    /**
     * 加载 icons/ai/ 下的官方 logo(32x32 PNG)
     */
    private static Icon icon(String fileName) {
        return IconLoader.getIcon("/icons/ai/" + fileName, AiProviderRegistry.class);
    }

    /**
     * 按 id 查找预设; 找不到或 id 为空时返回自定义预设
     */
    public static Preset byId(String id) {
        if (null != id) {
            for (Preset preset : PRESETS) {
                if (preset.id.equals(id)) {
                    return preset;
                }
            }
        }
        return customPreset();
    }

    /**
     * 自定义预设(列表最后一项)
     */
    public static Preset customPreset() {
        return PRESETS.get(PRESETS.size() - 1);
    }

    private AiProviderRegistry() {
    }
}
