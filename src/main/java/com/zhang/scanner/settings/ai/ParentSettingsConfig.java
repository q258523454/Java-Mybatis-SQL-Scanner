package com.zhang.scanner.settings.ai;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.zhang.scanner.utils.LanguageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * 设置页父节点:
 * Other Settings > MyBatis SQL Scanner AI
 * 下挂 Rules Settings 与 Model Settings 两个子页(子页在 plugin.xml 中通过 parentId 注册)
 * <p>
 * 点击父节点时显示说明面板(避免平台对无内容节点显示占位文案).
 */
public class ParentSettingsConfig implements SearchableConfigurable {

    public static final String ID = "zhang.scanner.settings.parent";

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "MyBatis SQL Scanner AI";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(16, 16, 16, 16));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JBLabel title = new JBLabel("MyBatis SQL Scanner AI");
        Font titleFont = title.getFont();
        title.setFont(titleFont.deriveFont(Font.BOLD, titleFont.getSize() + 2f));

        JBLabel hint = new JBLabel(LanguageUtil.zhOrEn(
                "Please select a sub-page in the tree on the left:",
                "请在左侧树中选择要配置的子页面："));
        hint.setForeground(JBColor.GRAY);

        JBLabel rulesDesc = new JBLabel(LanguageUtil.zhOrEn(
                "• Rules Settings - Scan rule switches and rule levels",
                "• Rules Settings - 扫描规则开关与规则等级配置"));
        JBLabel modelDesc = new JBLabel(LanguageUtil.zhOrEn(
                "• Model Settings - AI model configuration (API URL / API Key / Model)",
                "• Model Settings - AI 模型配置(API URL / API Key / Model)"));

        content.add(title);
        content.add(Box.createVerticalStrut(12));
        content.add(hint);
        content.add(Box.createVerticalStrut(12));
        content.add(rulesDesc);
        content.add(Box.createVerticalStrut(6));
        content.add(modelDesc);
        content.add(Box.createVerticalGlue());

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 父节点自身无配置项
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * 父节点自身无配置项
     */
    @Override
    public void apply() {
        // 无需处理
    }

    /**
     * 父节点自身无配置项
     */
    @Override
    public void reset() {
        // 无需处理
    }

    /**
     * 父节点自身无配置项
     */
    @Override
    public void disposeUIResources() {
        // 无需处理
    }
}
