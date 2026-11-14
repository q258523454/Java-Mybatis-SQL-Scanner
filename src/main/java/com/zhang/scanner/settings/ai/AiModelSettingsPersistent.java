package com.zhang.scanner.settings.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 模型配置持久化
 * 注意: API Key 按用户要求明文存储于 AiModelSettings.xml
 */
@State(
        name = "AiModelSettingsPersistent",
        storages = @Storage("AiModelSettings.xml")
)
public class AiModelSettingsPersistent implements PersistentStateComponent<AiModelSettingsPersistent> {

    /**
     * API 完整端点(OpenAI 兼容), 因为要序列化,必须是 public
     */
    public String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    /**
     * API Key(明文持久化)
     */
    public String apiKey = "";

    /**
     * 模型名称
     */
    public String model = "deepseek-v4-pro";

    public static AiModelSettingsPersistent getInstance() {
        return ApplicationManager.getApplication().getService(AiModelSettingsPersistent.class);
    }

    @Nullable
    @Override
    public AiModelSettingsPersistent getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AiModelSettingsPersistent state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
