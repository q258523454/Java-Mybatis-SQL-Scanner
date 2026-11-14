package com.zhang.scanner.settings.ai;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.zhang.scanner.settings.MySettingsConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * 设置页父节点(纯分组节点):
 * Other Settings > Java Mybatis SQL Scanner Rules
 * 下挂 Rules Settings 与 Model Settings 两个子页
 */
public class ParentSettingsConfig implements SearchableConfigurable.Parent {

    @NotNull
    @Override
    public String getId() {
        return "zhang.scanner.settings.parent";
    }

    @Override
    public String getDisplayName() {
        return "Java Mybatis SQL Scanner Rules";
    }

    /**
     * 子设置页: Rules Settings + Model Settings
     */
    @Override
    public Configurable @NotNull [] getConfigurables() {
        return new Configurable[]{new MySettingsConfig(), new AiModelSettingsConfig()};
    }

    /**
     * 纯分组节点, 无任何配置项
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return null;
    }

    /**
     * 纯分组节点, 无任何配置项
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * 纯分组节点, 无任何配置项
     */
    @Override
    public void apply() {
        // 无需处理
    }

    /**
     * 纯分组节点, 无任何配置项
     */
    @Override
    public void reset() {
        // 无需处理
    }

    /**
     * 纯分组节点, 无任何配置项
     */
    @Override
    public void disposeUIResources() {
        // 无需处理
    }
}
