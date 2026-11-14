package com.zhang.scanner.ui;


import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PsiNavigateUtil;
import com.zhang.scanner.ai.AiChatPanel;
import com.zhang.scanner.executor.TreeConsoleExecutor;
import com.zhang.scanner.settings.ai.AiModelSettingsConfig;
import com.zhang.scanner.settings.ai.AiModelSettingsPersistent;
import com.zhang.scanner.settings.ai.AiProviderConfig;
import com.zhang.scanner.utils.LanguageUtil;
import com.zhang.scanner.utils.MyExecutorUtil;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 分屏、Tree模型、代码预览
 * 参考  {@link com.intellij.ide.todo.TodoPanel}
 */
public class SqlScanResultPanel {

    private JPanel mainPanel;

    private JTextField sqlNodeIdOrg;
    private JLabel sqlNodeIdLabel;
    private JLabel sqlTypeLabel;
    private JLabel operationLabel;
    private JButton openFileButton;
    private JButton askAiButton;
    private JButton aiLanguageButton;
    private JEditorPane sqlText;
    private JEditorPane sqlScanResult;
    private JLabel sqlLabel;
    private JLabel parseLabel;
    private JTextField sqlType;

    /**
     * 用来直接跳转到指定的 xml tag 处
     * 如果找不到对应的 sql id ,则定位到 mapper 根节点
     */
    private XmlTag xmlTag;

    /**
     * 上次 Ask AI 对话面板(缓存: Back 后再次点击同一 SQL 节点时复用, 不重复调用模型)
     */
    private AiChatPanel cachedChatPanel;

    /**
     * 缓存面板对应的 SQL 节点 id(与缓存面板配套)
     */
    private String cachedSqlNodeId;



    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JTextField getSqlNodeIdOrg() {
        return sqlNodeIdOrg;
    }

    public JTextField getSqlType() {
        return sqlType;
    }

    public JEditorPane getSqlText() {
        return sqlText;
    }

    public JEditorPane getSqlScanResult() {
        return sqlScanResult;
    }

    public XmlTag getXmlTag() {
        return xmlTag;
    }

    public void setXmlTag(XmlTag xmlTag) {
        this.xmlTag = xmlTag;
    }

    public SqlScanResultPanel() {
        // 界面文案国际化: 中文环境显示中文, 其他语言显示英文
        sqlNodeIdLabel.setText(LanguageUtil.zhOrEn("SQL ID:", "SQL ID："));
        sqlTypeLabel.setText(LanguageUtil.zhOrEn("SQL Type:", "SQl Type："));
        sqlLabel.setText(LanguageUtil.zhOrEn("Format SQL:", "Format SQL："));
        parseLabel.setText(LanguageUtil.zhOrEn("Error/Suggestion:", "Error/Suggestion："));
        operationLabel.setText(LanguageUtil.zhOrEn("Operation:", "Operation："));

        // 按钮文案国际化("Ask AI" 为功能专名保留英文)
        openFileButton.setText(LanguageUtil.zhOrEn("Open File", "打开文件"));
        openFileButton.setToolTipText(LanguageUtil.zhOrEn("Navigate To Line(XML)", "跳转到 XML 对应行"));
        askAiButton.setToolTipText(LanguageUtil.zhOrEn("Analyze SQL with AI", "用 AI 分析 SQL"));

        // AI Language 按钮: 选择 AI 回答语言(与 UI 国际化无关, 仅控制与 AI 对话的语言)
        aiLanguageButton.setText(LanguageUtil.zhOrEn("AI Language", "AI语言"));
        aiLanguageButton.setToolTipText(LanguageUtil.zhOrEn(
                "Choose AI answer language", "选择 AI 回答语言"));
        aiLanguageButton.addActionListener(e -> showAiLanguageMenu());

        openFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (null != xmlTag) {
                    // 按钮回调运行在 EDT 上; navigate 会打开文件编辑器(写操作),
                    // 禁止用 ReadAction 包裹, 否则触发 WriteIntentReadAction can not be called from ReadAction
                    PsiNavigateUtil.navigate(xmlTag);
                }
            }
        });

        // Ask AI: 将当前 SQL 上下文发给 AI 分析, 右侧窗口替换为 AI 对话窗口
        askAiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 与 RunAction 既有用法一致: 从应用级执行器取 splitter 与 project
                TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);
                if (null == executor) {
                    return;
                }
                Project project = executor.getProject();
                Splitter splitterPanel = executor.getSplitterPanel();

                // 当前激活供应商配置: 无槽位时用预设默认值(只读, 不落盘)
                AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
                AiProviderConfig active = settings.getActiveConfig();
                if (null == active) {
                    active = settings.getConfigOrDefault(settings.activeProvider);
                }
                if (isBlank(active.apiUrl) || isBlank(active.apiKey)) {
                    // 未配置模型参数: 提示并可直达 Model Settings 设置页
                    int choice = Messages.showDialog(project,
                            LanguageUtil.zhOrEn(
                                    "Please configure API URL and API Key in Model Settings first",
                                    "请先在 Model Settings 中配置 API URL 与 API Key"),
                            "Ask AI",
                            new String[]{
                                    LanguageUtil.zhOrEn("Configure", "配置"),
                                    LanguageUtil.zhOrEn("Cancel", "取消")
                            }, 0, Messages.getWarningIcon());
                    if (choice == 0) {
                        // 按 class 直达(类已在 plugin.xml 注册)
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, AiModelSettingsConfig.class);
                    }
                    return;
                }

                String sqlNodeId = sqlNodeIdOrg.getText();

                // 缓存命中: 同一 SQL 节点、同一供应商配置(provider/URL/模型)、实际回答语言相同
                // 且面板可复用(已完成省 Token 直接复用; 仍在生成中则切回来继续看, 不重复调用;
                // 停止/失败后不可复用, 重新点击重新生成)
                // 注: 比较的是解析后的实际语言(如 en→auto 且 IDE 为英文时不重新调用)
                if (null != cachedChatPanel && cachedChatPanel.isReusable()
                        && java.util.Objects.equals(cachedSqlNodeId, sqlNodeId)
                        && cachedChatPanel.isAnswerChinese() == settings.isAiAnswerChinese()
                        && java.util.Objects.equals(cachedChatPanel.getProviderId(), active.id)
                        && java.util.Objects.equals(cachedChatPanel.getApiUrl(), active.apiUrl)
                        && java.util.Objects.equals(cachedChatPanel.getModel(), active.model)) {
                    splitterPanel.setSecondComponent(cachedChatPanel.getComponent());
                    return;
                }

                // 组装四段上下文并替换右侧窗口
                // 原始 XML 语句(解析前): 从 xmlTag 读取, PSI 访问需持有读锁
                String rawXml = "";
                if (null != xmlTag) {
                    rawXml = ReadAction.compute(() -> xmlTag.getText());
                }
                AiChatPanel chatPanel = new AiChatPanel(project,
                        sqlNodeId,
                        sqlType.getText(),
                        rawXml,
                        sqlText.getText(),
                        sqlScanResult.getText(),
                        () -> splitterPanel.setSecondComponent(mainPanel));
                cachedChatPanel = chatPanel;
                cachedSqlNodeId = sqlNodeId;
                splitterPanel.setSecondComponent(chatPanel.getComponent());
                chatPanel.start();
            }
        });
    }

    private static boolean isBlank(String s) {
        return null == s || s.trim().isEmpty();
    }

    /**
     * 弹出 AI 回答语言选择菜单(仅控制与 AI 对话的语言, 与 UI 国际化无关)
     */
    private void showAiLanguageMenu() {
        JPopupMenu menu = new JPopupMenu();

        // 菜单选项文本跟随 UI 语言(与国际化一致)
        JMenuItem autoItem = new JMenuItem(languageMenuItemText("auto",
                LanguageUtil.zhOrEn("Follow IDE (Auto)", "跟随 IDE（自动）")));
        autoItem.addActionListener(e -> setAiAnswerLanguage("auto"));
        menu.add(autoItem);

        JMenuItem zhItem = new JMenuItem(languageMenuItemText("zh",
                LanguageUtil.zhOrEn("Chinese", "中文")));
        zhItem.addActionListener(e -> setAiAnswerLanguage("zh"));
        menu.add(zhItem);

        JMenuItem enItem = new JMenuItem(languageMenuItemText("en",
                LanguageUtil.zhOrEn("English", "英文")));
        enItem.addActionListener(e -> setAiAnswerLanguage("en"));
        menu.add(enItem);

        menu.show(aiLanguageButton, 0, aiLanguageButton.getHeight());
    }

    /**
     * 菜单项文本: 当前选项加 ✓ 前缀
     */
    private static String languageMenuItemText(String key, String text) {
        return key.equals(AiModelSettingsPersistent.getInstance().aiAnswerLanguage)
                ? "✓ " + text : "    " + text;
    }

    /**
     * 写入 AI 回答语言偏好(持久化组件, 重启保留)
     */
    private static void setAiAnswerLanguage(String lang) {
        AiModelSettingsPersistent.getInstance().aiAnswerLanguage = lang;
    }
}
