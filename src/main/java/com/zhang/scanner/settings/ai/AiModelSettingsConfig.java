package com.zhang.scanner.settings.ai;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.zhang.scanner.ai.AiApiClient;
import com.zhang.scanner.utils.LanguageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model Settings 设置页: Provider / API URL / API Key / Model
 * Provider 下拉框(7 家供应商 + 自定义)切换时自动填充官方默认 URL 与模型;
 * 每家独立保存一套配置, 切换回来继续可用
 * 编辑期间用内存草稿(drafts)暂存, 点 OK/Apply 才落盘持久化
 * API Key 输入为密文, 眼睛按钮切换明文
 */
public class AiModelSettingsConfig implements SearchableConfigurable {

    /**
     * 设置页 id(供 ShowSettingsUtil.showSettingsDialog(project, id) 直达)
     */
    public static final String ID = "zhang.scanner.settings.ai.model";

    private JComboBox<AiProviderRegistry.Preset> providerCombo;
    private JTextField apiUrlField;
    private JPasswordField apiKeyField;
    private JComboBox<String> modelCombo;
    private JButton eyeButton;
    private JButton testButton;
    private JLabel testResultLabel;

    /**
     * 编辑草稿(供应商 id → 配置), 仅内存; apply() 时落盘, reset() 时清空
     */
    private Map<String, AiProviderConfig> drafts;

    /**
     * 当前输入框内容归属的供应商 id
     */
    private String currentDraftProviderId;

    /**
     * reset() 期间抑制切换动作把 UI 旧值写回草稿
     */
    private boolean suppressSwitchSave = false;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Model Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        drafts = new HashMap<>();

        apiUrlField = new JTextField();
        apiKeyField = new JPasswordField();
        modelCombo = new JComboBox<>();
        modelCombo.setEditable(true);

        // Model 下拉可编辑: 常驻提示 + tooltip 让用户知道选中推荐模型后仍可直接输入自定义模型名
        String modelTip = LanguageUtil.zhOrEn(
                "Select a recommended model, or type a custom model name.",
                "可选择推荐模型，也可直接输入自定义模型名。");
        modelCombo.setToolTipText(modelTip);
        ((JComponent) modelCombo.getEditor().getEditorComponent()).setToolTipText(modelTip);

        JBLabel modelHint = new JBLabel(LanguageUtil.zhOrEn(
                "Choose a recommended model, or type a custom model name.",
                "可选择推荐模型，也可直接输入自定义模型名。"));
        modelHint.setForeground(JBColor.GRAY);

        JPanel modelPanel = new JPanel(new BorderLayout(0, 2));
        modelPanel.add(modelCombo, BorderLayout.NORTH);
        modelPanel.add(modelHint, BorderLayout.SOUTH);

        // Provider 下拉框: 选中项显示名随 UI 语言
        providerCombo = new JComboBox<>(AiProviderRegistry.PRESETS.toArray(
                new AiProviderRegistry.Preset[0]));
        providerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof AiProviderRegistry.Preset) {
                    AiProviderRegistry.Preset preset = (AiProviderRegistry.Preset) value;
                    label.setText(LanguageUtil.zhOrEn(preset.displayNameEn, preset.displayNameZh));
                    // 供应商官方 logo(icons/ai/ 下的 32x32 PNG; 自定义项为齿轮图标)
                    label.setIcon(preset.icon);
                    label.setIconTextGap(6);
                }
                return label;
            }
        });
        // 先初始化选中项, 再加切换监听(避免初始化触发一次无意义的切换)
        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        AiProviderRegistry.Preset initPreset = AiProviderRegistry.byId(settings.activeProvider);
        providerCombo.setSelectedItem(initPreset);
        currentDraftProviderId = initPreset.id;
        loadDraftIntoFields(getDraft(initPreset.id), initPreset);
        providerCombo.addActionListener(e -> switchProvider());

        eyeButton = new JButton(AllIcons.Actions.Preview);
        eyeButton.setToolTipText(LanguageUtil.zhOrEn("Show/Hide API Key", "显示/隐藏 API Key"));
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

        // Test Connection 行: 按钮 + 结果标签
        testButton = new JButton(LanguageUtil.zhOrEn("Test Connection", "测试连接"));
        testResultLabel = new JLabel(" ");
        JPanel testRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        testRow.add(testButton);
        testRow.add(testResultLabel);
        testButton.addActionListener(e -> {
            String url = apiUrlField.getText().trim();
            String key = new String(apiKeyField.getPassword());
            String model = getModelText();
            if (url.isEmpty() || key.isEmpty()) {
                testResultLabel.setForeground(JBColor.RED);
                testResultLabel.setText(LanguageUtil.zhOrEn(
                        "✗ Please fill in API URL and API Key first",
                        "✗ 请先填写 API URL 与 API Key"));
                return;
            }
            // 协议与 thinking 策略由当前选中供应商预设决定(EDT 内取值, 后台线程不可碰 UI)
            AiProviderRegistry.Preset preset =
                    (AiProviderRegistry.Preset) providerCombo.getSelectedItem();
            AiProviderRegistry.Protocol protocol = null == preset
                    ? AiProviderRegistry.Protocol.OPENAI_COMPAT : preset.protocol;
            AiProviderRegistry.ThinkingMode thinkingMode = null == preset
                    ? AiProviderRegistry.ThinkingMode.NONE : preset.thinkingMode;
            // 测试请求在后台线程执行, UI 不阻塞
            testButton.setEnabled(false);
            testButton.setText(LanguageUtil.zhOrEn("Testing...", "测试中…"));
            testResultLabel.setForeground(JBColor.foreground());
            testResultLabel.setText(LanguageUtil.zhOrEn("Testing connection...", "正在测试连接…"));
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                AiApiClient.TestResult result =
                        AiApiClient.testConnection(url, key, model, protocol, thinkingMode);
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 设置对话框是模态对话框, 后台线程的 invokeLater 默认是非模态状态会被 EDT 阻塞;
                    // 必须用 ModalityState.any() 才能在模态对话框打开期间更新 UI
                    // 测试期间用户可能已关闭设置对话框(disposeUIResources 置空字段), 需判空
                    if (null == testButton || null == testResultLabel) {
                        return;
                    }
                    testButton.setEnabled(true);
                    testButton.setText(LanguageUtil.zhOrEn("Test Connection", "测试连接"));
                    testResultLabel.setForeground(result.ok ? JBColor.GREEN : JBColor.RED);
                    testResultLabel.setText((result.ok ? "✓ " : "✗ ") + result.message);
                }, ModalityState.any());
            });
        });

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(LanguageUtil.zhOrEn("Provider", "服务商"), providerCombo)
                .addLabeledComponent(LanguageUtil.zhOrEn("API URL", "API 地址"), apiUrlField)
                .addLabeledComponent(LanguageUtil.zhOrEn("API Key", "API 密钥"), apiKeyPanel)
                .addLabeledComponent(LanguageUtil.zhOrEn("Model", "模型"), modelPanel)
                .addComponent(testRow)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    /**
     * 切换供应商: 先把当前输入框内容暂存到原供应商草稿, 再加载新供应商草稿
     */
    private void switchProvider() {
        AiProviderRegistry.Preset preset =
                (AiProviderRegistry.Preset) providerCombo.getSelectedItem();
        if (null == preset || preset.id.equals(currentDraftProviderId)) {
            return;
        }
        if (!suppressSwitchSave) {
            saveFieldsIntoDraft(currentDraftProviderId);
        }
        currentDraftProviderId = preset.id;
        loadDraftIntoFields(getDraft(preset.id), preset);
    }

    /**
     * 取某供应商的编辑草稿; 无草稿时以持久化值(无槽位则用预设默认 URL/模型)新建
     * 注意: 只读持久化, 不写回(点 Cancel 不落盘)
     */
    private AiProviderConfig getDraft(String id) {
        AiProviderConfig draft = drafts.get(id);
        if (null != draft) {
            return draft;
        }
        AiProviderConfig persisted = findPersisted(AiModelSettingsPersistent.getInstance(), id);
        AiProviderConfig d = new AiProviderConfig();
        d.id = id;
        if (null != persisted) {
            d.apiUrl = null == persisted.apiUrl ? "" : persisted.apiUrl;
            d.apiKey = null == persisted.apiKey ? "" : persisted.apiKey;
            d.model = null == persisted.model ? "" : persisted.model;
        } else {
            AiProviderRegistry.Preset preset = AiProviderRegistry.byId(id);
            d.apiUrl = preset.defaultApiUrl;
            d.model = preset.defaultModel;
        }
        drafts.put(id, d);
        return d;
    }

    /**
     * 把草稿加载到输入控件, 并刷新模型下拉(推荐模型 + 当前值)
     */
    private void loadDraftIntoFields(AiProviderConfig cfg, AiProviderRegistry.Preset preset) {
        apiUrlField.setText(cfg.apiUrl);
        apiKeyField.setText(cfg.apiKey);
        modelCombo.removeAllItems();
        for (String recommended : preset.recommendedModels) {
            modelCombo.addItem(recommended);
        }
        if (!cfg.model.isEmpty() && !preset.recommendedModels.contains(cfg.model)) {
            modelCombo.addItem(cfg.model);
        }
        modelCombo.setSelectedItem(cfg.model);
    }

    /**
     * 把输入控件当前值写回某供应商草稿
     */
    private void saveFieldsIntoDraft(String id) {
        if (null == id) {
            return;
        }
        AiProviderConfig cfg = drafts.get(id);
        if (null == cfg) {
            cfg = new AiProviderConfig();
            cfg.id = id;
            drafts.put(id, cfg);
        }
        cfg.apiUrl = apiUrlField.getText().trim();
        cfg.apiKey = new String(apiKeyField.getPassword());
        cfg.model = getModelText();
    }

    /**
     * 按 id 在持久化列表查找槽位
     */
    private static AiProviderConfig findPersisted(AiModelSettingsPersistent settings, String id) {
        for (AiProviderConfig c : settings.providers) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 检查是否被编辑,用来触发 apply(), reset() 事件.
     * 先把当前输入暂存, 再与持久化逐槽比较
     * 注: 持久化无槽且草稿为纯预设默认值(仅浏览过未编辑)不算修改
     */
    @Override
    public boolean isModified() {
        saveFieldsIntoDraft(currentDraftProviderId);
        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        if (!settings.activeProvider.equals(currentDraftProviderId)) {
            return true;
        }
        for (AiProviderConfig draft : drafts.values()) {
            AiProviderConfig persisted = findPersisted(settings, draft.id);
            if (null == persisted) {
                // 无持久化槽: 草稿等于预设默认(URL/模型默认且 Key 空)视为未编辑
                AiProviderRegistry.Preset preset = AiProviderRegistry.byId(draft.id);
                boolean isDefault = preset.defaultApiUrl.equals(draft.apiUrl)
                        && preset.defaultModel.equals(draft.model)
                        && draft.apiKey.isEmpty();
                if (!isDefault) {
                    return true;
                }
                continue;
            }
            if (!Objects.equals(draft.apiUrl, persisted.apiUrl)
                    || !Objects.equals(draft.apiKey, persisted.apiKey)
                    || !Objects.equals(draft.model, persisted.model)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 编辑后,点击'OK'或者'apply',保存idea窗口上的值
     * 全部草稿落盘到对应供应商槽位, 旧单套字段置空
     */
    @Override
    public void apply() {
        saveFieldsIntoDraft(currentDraftProviderId);
        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        AiProviderRegistry.Preset preset =
                (AiProviderRegistry.Preset) providerCombo.getSelectedItem();
        settings.activeProvider = null == preset ? currentDraftProviderId : preset.id;
        // 防护: 存在无法解密的已加密 Key 且保存会把它清空时, 先向用户确认(见 confirmBeforeClearingUndecryptableKey)
        boolean keepBrokenKey = confirmBeforeClearingUndecryptableKey(settings);
        for (AiProviderConfig draft : drafts.values()) {
            AiProviderConfig persisted = settings.getOrCreateConfig(draft.id);
            persisted.apiUrl = draft.apiUrl;
            boolean skipKeyWrite = keepBrokenKey && (draft.apiKey == null || draft.apiKey.isEmpty());
            if (!skipKeyWrite) {
                persisted.apiKey = draft.apiKey;
            }
            persisted.model = draft.model;
        }
        // 旧版本字段已迁移, 置空避免 XML 冗余
        settings.apiUrl = "";
        settings.apiKey = "";
        settings.model = "";
    }

    /**
     * loadState 标记存在无法解密的已加密 Key(密文损坏/密钥不匹配)时弹确认框;
     * 用户选择"保留原值"则跳过空 Key 的写入, 磁盘上仍存的密文不被静默清空
     *
     * @return true 表示跳过空 Key 写入(保留原值)
     */
    private boolean confirmBeforeClearingUndecryptableKey(AiModelSettingsPersistent settings) {
        if (!settings.hasUndecryptableApiKey) {
            return false;
        }
        int choice = Messages.showYesNoDialog(
                LanguageUtil.zhOrEn(
                        "A previously saved API Key cannot be decrypted (corrupted config or incompatible version). "
                                + "Saving now will clear it and you will need to re-enter the key. Continue?",
                        "存在无法解密的已保存 API Key(配置损坏或版本不兼容)。"
                                + "继续保存将清空该 Key, 之后需要重新填写。是否继续?"),
                LanguageUtil.zhOrEn("API Key", "API Key 提示"),
                LanguageUtil.zhOrEn("Continue", "继续"),
                LanguageUtil.zhOrEn("Keep Old Value", "保留原值"),
                Messages.getQuestionIcon());
        return choice == Messages.NO;
    }

    /**
     * 初始化重置；或者编辑后,点击'Reset'重置
     * 清空草稿, 从持久化重新加载激活供应商
     */
    @Override
    public void reset() {
        drafts.clear();
        AiProviderRegistry.Preset preset =
                AiProviderRegistry.byId(AiModelSettingsPersistent.getInstance().activeProvider);
        suppressSwitchSave = true;
        try {
            providerCombo.setSelectedItem(preset);
        } finally {
            suppressSwitchSave = false;
        }
        currentDraftProviderId = preset.id;
        loadDraftIntoFields(getDraft(preset.id), preset);
    }

    /**
     * 关闭Settings
     */
    @Override
    public void disposeUIResources() {
        providerCombo = null;
        apiUrlField = null;
        apiKeyField = null;
        modelCombo = null;
        eyeButton = null;
        testButton = null;
        testResultLabel = null;
        drafts = null;
        currentDraftProviderId = null;
    }

    /**
     * 读取 model 下拉框当前文本(可编辑下拉, 兼容自定义模型名)
     */
    private String getModelText() {
        Object item = modelCombo.getEditor().getItem();
        return null == item ? "" : item.toString().trim();
    }
}
