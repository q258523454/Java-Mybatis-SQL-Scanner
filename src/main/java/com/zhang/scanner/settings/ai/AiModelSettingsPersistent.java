package com.zhang.scanner.settings.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.zhang.scanner.utils.LanguageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 模型配置持久化
 * 多供应商结构: activeProvider + providers(每家独立一套 URL/Key/Model)
 * 旧版本只有 apiUrl/apiKey/model 三字段, 加载后自动迁移为 DeepSeek 槽
 * 注意: API Key 以 AES-256-GCM 密文(ENC1: 前缀)持久化于 AiModelSettings.xml,
 * 内存中为明文; 旧版本明文配置在读取时兼容, 下次保存时自动转为密文(见 AiKeyCipher)
 */
@State(
        name = "AiModelSettingsPersistent",
        storages = @Storage("AiModelSettings.xml")
)
public class AiModelSettingsPersistent implements PersistentStateComponent<AiModelSettingsPersistent> {

    /**
     * 旧版本字段: API 完整端点(OpenAI 兼容), 仅用于旧配置迁移, 新版本请使用 providers
     * 保持序列化(不能 @Transient: 会导致旧 XML 字段无法读入), 迁移后由设置页置空
     */
    public String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    /**
     * 旧版本字段: API Key(落盘为 ENC1: 密文), 仅用于旧配置迁移, 新版本请使用 providers
     */
    public String apiKey = "";

    /**
     * 旧版本字段: 模型名称, 仅用于旧配置迁移, 新版本请使用 providers
     */
    public String model = "deepseek-v4-pro";

    /**
     * 当前激活供应商 id(deepseek/qwen/doubao/glm/kimi/gpt/claude/custom)
     */
    public String activeProvider = "deepseek";

    /**
     * 各供应商独立配置(每家一套 URL/Key/Model)
     */
    public List<AiProviderConfig> providers = new ArrayList<>();

    /**
     * AI 回答语言: auto(跟随 IDE) / zh(中文) / en(英文)
     * 仅控制与 AI 对话的语言, 与 UI 国际化无关
     */
    public String aiAnswerLanguage = "auto";

    /**
     * 回答区字号合法范围: 8-24(px)
     */
    public static final int MIN_ANSWER_FONT_SIZE = 8;
    public static final int MAX_ANSWER_FONT_SIZE = 24;

    /**
     * AI 回答区文字字号(px, 8-24, A-/A+ 按钮调节并持久化)
     * 未显式设置时该值只作合法占位(实际显示跟随 IDE Editor Font); 用户点过 A-/A+ 后写入 8-24
     */
    public int answerFontSize = MIN_ANSWER_FONT_SIZE;

    /**
     * 用户是否显式设置过回答区字号
     * 旧配置没有该字段, 反序列化为 false, 旧字号(14/10 等)一律忽略
     */
    public boolean answerFontSizeExplicit = false;

    /**
     * 是否存在无法解密的已加密 Key(密文损坏或密钥不匹配)
     * loadState 时计算, 不持久化(@Transient); 供设置页 apply() 检测,
     * 避免用户在不知情时静默清空磁盘上仍存在的密文
     */
    @Transient
    public boolean hasUndecryptableApiKey = false;

    public static AiModelSettingsPersistent getInstance() {
        return ApplicationManager.getApplication().getService(AiModelSettingsPersistent.class);
    }

    /**
     * 当前激活供应商配置; 不存在时返回 null
     */
    @Nullable
    public AiProviderConfig getActiveConfig() {
        if (null == providers) {
            return null;
        }
        for (AiProviderConfig config : providers) {
            if (config.id.equals(activeProvider)) {
                return config;
            }
        }
        return null;
    }

    /**
     * 按 id 取配置; 不存在时返回以预设默认值构造的临时配置(不加入列表, 不落盘)
     * 供读路径(Ask AI 组装请求)使用, 避免未确认的槽位被写入持久化
     */
    public AiProviderConfig getConfigOrDefault(String id) {
        if (null != providers) {
            for (AiProviderConfig config : providers) {
                if (config.id.equals(id)) {
                    return config;
                }
            }
        }
        AiProviderRegistry.Preset preset = AiProviderRegistry.byId(id);
        AiProviderConfig config = new AiProviderConfig();
        config.id = preset.id;
        config.apiUrl = preset.defaultApiUrl;
        config.model = preset.defaultModel;
        return config;
    }

    /**
     * 按 id 取配置; 不存在时以预设默认值(官方 URL + 默认模型)创建并加入列表后返回
     * 仅供设置页 apply() 落盘使用; 读路径请用 getConfigOrDefault 避免污染持久化
     */
    public AiProviderConfig getOrCreateConfig(String id) {
        if (null == providers) {
            providers = new ArrayList<>();
        }
        for (AiProviderConfig config : providers) {
            if (config.id.equals(id)) {
                return config;
            }
        }
        AiProviderRegistry.Preset preset = AiProviderRegistry.byId(id);
        AiProviderConfig config = new AiProviderConfig();
        config.id = preset.id;
        config.apiUrl = preset.defaultApiUrl;
        config.model = preset.defaultModel;
        providers.add(config);
        return config;
    }

    /**
     * 旧版本单套配置迁移为 DeepSeek 槽:
     * providers 为空时始终用旧三字段创建(旧字段本身有代码默认值, 等价于全新安装的默认槽)
     */
    private void migrateLegacyConfig() {
        if (null != providers && !providers.isEmpty()) {
            return;
        }
        providers = new ArrayList<>();
        AiProviderConfig config = new AiProviderConfig();
        config.id = "deepseek";
        config.apiUrl = (null == apiUrl || apiUrl.trim().isEmpty())
                ? "https://api.deepseek.com/v1/chat/completions" : apiUrl;
        config.apiKey = null == apiKey ? "" : apiKey;
        config.model = (null == model || model.trim().isEmpty()) ? "deepseek-v4-pro" : model;
        providers.add(config);
    }

    /**
     * 解析 AI 回答语言偏好为实际回答语言是否为中文:
     * zh=中文, en=英文, auto=跟随 IDE 语言(中文环境为中文, 其他为英文)
     */
    public boolean isAiAnswerChinese() {
        if ("zh".equals(aiAnswerLanguage)) {
            return true;
        }
        if ("en".equals(aiAnswerLanguage)) {
            return false;
        }
        return LanguageUtil.isChinese();
    }

    @Nullable
    @Override
    public AiModelSettingsPersistent getState() {
        // 落盘前返回加密副本, 绝不可修改 this: 内存中必须保持明文, 否则运行中的 Ask AI 会拿到密文
        AiModelSettingsPersistent copy = new AiModelSettingsPersistent();
        XmlSerializerUtil.copyBean(this, copy);
        copy.providers = new ArrayList<>();
        if (providers != null) {
            for (AiProviderConfig config : providers) {
                AiProviderConfig cc = new AiProviderConfig();
                cc.id = config.id;
                cc.apiUrl = config.apiUrl;
                cc.apiKey = AiKeyCipher.encrypt(config.apiKey);
                cc.model = config.model;
                copy.providers.add(cc);
            }
        }
        // 旧版单字段同样加密, 避免 XML 残留明文
        copy.apiKey = AiKeyCipher.encrypt(apiKey);
        // 运行期标记不持久化, 防御 copyBean 复制了 this 的值
        copy.hasUndecryptableApiKey = false;
        return copy;
    }

    @Override
    public void loadState(@NotNull AiModelSettingsPersistent state) {
        XmlSerializerUtil.copyBean(state, this);
        // 记录解密失败的已加密 Key(密文损坏/密钥不匹配), 供设置页保存时提示, 防止静默清空
        boolean hasUndecryptable = AiKeyCipher.isUndecryptable(apiKey);
        // 旧版单字段若为密文先解密, 再迁移到 providers(明文旧值经 decrypt 无前缀原样透传, 天然兼容)
        apiKey = AiKeyCipher.decrypt(apiKey);
        migrateLegacyConfig();
        // 各家供应商 Key 解密(旧明文无前缀原样透传, 重复解密幂等)
        if (providers != null) {
            for (AiProviderConfig config : providers) {
                if (AiKeyCipher.isUndecryptable(config.apiKey)) {
                    hasUndecryptable = true;
                }
                config.apiKey = AiKeyCipher.decrypt(config.apiKey);
            }
        }
        hasUndecryptableApiKey = hasUndecryptable;
        // 旧版本没有 answerFontSizeExplicit 字段(默认 false), 旧的 answerFontSize 一律忽略,
        // 按"未显式设置"处理: 存合法占位值, 实际显示由 AiChatPanel 实时跟随 IDE Editor Font
        if (!answerFontSizeExplicit) {
            answerFontSize = MIN_ANSWER_FONT_SIZE;
        } else {
            // 显式设置过: 夹紧到 8-24, 修掉历史越界值
            answerFontSize = Math.max(MIN_ANSWER_FONT_SIZE, Math.min(MAX_ANSWER_FONT_SIZE, answerFontSize));
        }
    }
}
