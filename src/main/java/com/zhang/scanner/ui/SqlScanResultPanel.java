package com.zhang.scanner.ui;


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
import com.zhang.scanner.utils.MyExecutorUtil;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    private JButton openFileButton;
    private JButton askAiButton;
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

                AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
                if (isBlank(settings.apiUrl) || isBlank(settings.apiKey)) {
                    // 未配置模型参数: 提示并可直达设置页
                    int choice = Messages.showDialog(project,
                            "请先在 Model Settings 中配置 API URL 与 API Key",
                            "Ask AI",
                            new String[]{"Configure", "Cancel"}, 0, Messages.getWarningIcon());
                    if (choice == 0) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, AiModelSettingsConfig.ID);
                    }
                    return;
                }

                // 组装四段上下文并替换右侧窗口
                AiChatPanel chatPanel = new AiChatPanel(project,
                        sqlNodeIdOrg.getText(),
                        sqlType.getText(),
                        sqlText.getText(),
                        sqlScanResult.getText(),
                        () -> splitterPanel.setSecondComponent(mainPanel));
                splitterPanel.setSecondComponent(chatPanel.getComponent());
                chatPanel.start();
            }
        });
    }

    private static boolean isBlank(String s) {
        return null == s || s.trim().isEmpty();
    }
}
