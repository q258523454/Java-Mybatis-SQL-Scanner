package com.zhang.scanner.settings.ai;

/**
 * 单个供应商的 AI 模型配置(持久化单元, 每家独立保存一套 URL/Key/Model)
 * 字段 public 与无参构造是 IntelliJ XmlSerializer 序列化要求
 */
public class AiProviderConfig {

    /**
     * 供应商 id(deepseek/qwen/doubao/glm/kimi/gpt/claude/custom)
     */
    public String id = "";

    /**
     * API 完整端点(chat 端点, OpenAI 兼容或 Anthropic Messages)
     */
    public String apiUrl = "";

    /**
     * API Key(落盘为 ENC1: 密文, 内存为明文, 见 AiKeyCipher)
     */
    public String apiKey = "";

    /**
     * 模型名称
     */
    public String model = "";
}
