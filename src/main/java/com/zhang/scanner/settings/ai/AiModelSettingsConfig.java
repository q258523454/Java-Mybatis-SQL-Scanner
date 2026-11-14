package com.zhang.scanner.settings.ai;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.Objects;

/**
 * Model Settings 设置页: API URL / API Key / Model
 * API Key 输入为密文, 眼睛按钮切换明文
 */
public class AiModelSettingsConfig implements Configurable {

    private JTextField apiUrlField;
    private JPasswordField apiKeyField;
    private JComboBox<String> modelCombo;
    private JButton eyeButton;

    @Override
    public String getDisplayName() {
        return "Model Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        apiUrlField = new JTextField();
        apiKeyField = new JPasswordField();
        modelCombo = new JComboBox<>(new String[]{"deepseek-v4-pro", "deepseek-flash"});
        modelCombo.setEditable(true);

        eyeButton = new JButton(AllIcons.Actions.Preview);
        eyeButton.setToolTipText("Show/Hide API Key");
        eyeButton.addActionListener(e -> {
            if (apiKeyField.echoCharIsSet()) {
                // 显示明文
                apiKeyField.setEchoChar((char) 0);
            } else {
                // 恢复密文
                apiKeyField.setEchoChar('•');
            }
        });

        // API Key 行: 密码框 + 眼睛按钮
        JPanel apiKeyPanel = new JPanel(new BorderLayout());
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);
        apiKeyPanel.add(eyeButton, BorderLayout.EAST);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("API URL", apiUrlField)
                .addLabeledComponent("API Key", apiKeyPanel)
                .addLabeledComponent("Model", modelCombo)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    /**
     * 检查是否被编辑,用来触发 apply(), reset() 事件.
     */
    @Override
    public boolean isModified() {
        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        return !Objects.equals(apiUrlField.getText().trim(), settings.apiUrl)
                || !Objects.equals(new String(apiKeyField.getPassword()), settings.apiKey)
                || !Objects.equals(getModelText(), settings.model);
    }

    /**
     * 编辑后,点击'OK'或者'apply',保存idea窗口上的值
     */
    @Override
    public void apply() {
        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        settings.apiUrl = apiUrlField.getText().trim();
        settings.apiKey = new String(apiKeyField.getPassword());
        settings.model = getModelText();
    }

    /**
     * 初始化重置；或者编辑后,点击'Reset'重置
     */
    @Override
    public void reset() {
        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        apiUrlField.setText(settings.apiUrl);
        apiKeyField.setText(settings.apiKey);
        modelCombo.setSelectedItem(settings.model);
    }

    /**
     * 关闭Settings
     */
    @Override
    public void disposeUIResources() {
        apiUrlField = null;
        apiKeyField = null;
        modelCombo = null;
        eyeButton = null;
    }

    /**
     * 读取 model 下拉框当前文本(可编辑下拉, 兼容自定义模型名)
     */
    private String getModelText() {
        Object item = modelCombo.getEditor().getItem();
        return null == item ? "" : item.toString().trim();
    }
}
