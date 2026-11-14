package com.zhang.scanner.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.zhang.scanner.settings.ai.AiModelSettingsPersistent;
import com.zhang.scanner.utils.LanguageUtil;
import com.zhang.scanner.utils.MyLogUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * AI 对话窗口(单轮, 流式输出)
 * 布局: 顶部标题+上下文折叠区 / 中部流式回答区 / 底部状态栏(Stop/Back)
 */
public class AiChatPanel {

    private final Project project;
    private final String sqlNodeId;
    private final String sqlType;
    private final String sqlText;
    private final String parseResult;
    private final Runnable onBack;

    private JPanel mainPanel;
    private JPanel contextPanel;
    private JEditorPane answerPane;
    private JLabel statusLabel;
    private JButton stopButton;

    /**
     * 累积的回答文本(后台线程写, EDT 读)
     */
    private final StringBuilder answerBuffer = new StringBuilder();

    /**
     * 上次刷新时间(节流用, 仅后台线程读写)
     */
    private volatile long lastFlush = 0;

    private volatile boolean started = false;
    private volatile boolean stoppedByUser = false;
    private AiApiClient.StreamRequest streamRequest;

    public AiChatPanel(@NotNull Project project,
                       @NotNull String sqlNodeId,
                       @NotNull String sqlType,
                       @NotNull String sqlText,
                       @NotNull String parseResult,
                       @NotNull Runnable onBack) {
        this.project = project;
        this.sqlNodeId = sqlNodeId;
        this.sqlType = sqlType;
        this.sqlText = sqlText;
        this.parseResult = parseResult;
        this.onBack = onBack;
        buildUi();
    }

    @NotNull
    public JComponent getComponent() {
        return mainPanel;
    }

    /**
     * 发起请求(幂等, 已启动则忽略)
     */
    public void start() {
        if (started) {
            return;
        }
        started = true;

        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        if (isBlank(settings.apiUrl) || isBlank(settings.apiKey)) {
            statusLabel.setForeground(JBColor.RED);
            statusLabel.setText("未配置 API URL / API Key, 请在 Model Settings 中配置");
            return;
        }

        statusLabel.setForeground(JBColor.GRAY);
        statusLabel.setText("生成中…");
        stopButton.setEnabled(true);

        String systemPrompt = AiPromptTemplate.buildSystemPrompt(LanguageUtil.isChinese());
        String userPrompt = AiPromptTemplate.buildUserPrompt(sqlNodeId, sqlType, sqlText, parseResult);

        streamRequest = AiApiClient.startStream(settings.apiUrl, settings.apiKey, settings.model,
                systemPrompt, userPrompt, new AiApiClient.StreamCallback() {
                    @Override
                    public void onDelta(String text) {
                        // 后台线程: 累积 + 节流刷新
                        synchronized (answerBuffer) {
                            answerBuffer.append(text);
                        }
                        long now = System.currentTimeMillis();
                        if (now - lastFlush > 100) {
                            lastFlush = now;
                            ApplicationManager.getApplication().invokeLater(AiChatPanel.this::flushAnswer);
                        }
                    }

                    @Override
                    public void onDone() {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            flushAnswer();
                            stopButton.setEnabled(false);
                            statusLabel.setForeground(JBColor.GRAY);
                            statusLabel.setText(stoppedByUser ? "已停止" : "已完成");
                        });
                    }

                    @Override
                    public void onError(String friendlyMessage) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            flushAnswer();
                            stopButton.setEnabled(false);
                            statusLabel.setForeground(JBColor.RED);
                            statusLabel.setText("失败: " + friendlyMessage);
                            MyLogUtil.error("Ask AI 请求失败: " + friendlyMessage);
                        });
                    }
                });
    }

    /**
     * 构建界面
     */
    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout());

        // ===== 顶部: 标题 + 模型名 + 上下文折叠按钮 =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(JBUI.Borders.empty(8, 10, 4, 10));

        JLabel titleLabel = new JLabel("AI SQL Analysis");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        JLabel modelLabel = new JLabel(AiModelSettingsPersistent.getInstance().model);
        modelLabel.setForeground(JBColor.GRAY);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.add(titleLabel);
        titleRow.add(modelLabel);

        JButton toggleContextButton = new JButton("Show Context");
        toggleContextButton.addActionListener(e -> {
            boolean visible = !contextPanel.isVisible();
            contextPanel.setVisible(visible);
            toggleContextButton.setText(visible ? "Hide Context" : "Show Context");
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        topPanel.add(titleRow, BorderLayout.WEST);
        topPanel.add(toggleContextButton, BorderLayout.EAST);

        // 上下文折叠区(默认隐藏)
        contextPanel = buildContextPanel();
        contextPanel.setVisible(false);
        topPanel.add(contextPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // ===== 中部: 流式回答区 =====
        answerPane = new JEditorPane("text/html", "");
        answerPane.setEditable(false);
        answerPane.setBackground(UIUtil.getEditorPaneBackground());
        JScrollPane scrollPane = new JScrollPane(answerPane);
        scrollPane.setBorder(JBUI.Borders.empty());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ===== 底部: 状态栏 =====
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(JBUI.Borders.empty(0, 4, 0, 0));

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> {
            stoppedByUser = true;
            if (null != streamRequest) {
                streamRequest.cancel();
            }
            statusLabel.setForeground(JBColor.GRAY);
            statusLabel.setText("正在停止…");
        });

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            // 生成中返回: 先停止请求, 避免后台线程继续输出
            if (null != streamRequest) {
                stoppedByUser = true;
                streamRequest.cancel();
            }
            onBack.run();
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(JBUI.Borders.empty(4, 10, 8, 10));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(stopButton);
        buttonRow.add(backButton);
        bottomPanel.add(buttonRow, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 上下文折叠面板: 四段上下文
     */
    private JPanel buildContextPanel() {
        JPanel panel = JBUI.Panels.simplePanel();
        panel.setBorder(JBUI.Borders.empty(6, 0, 2, 0));
        panel.setLayout(new BorderLayout());

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<b>SQL ID</b><br/>").append(escapeHtml(sqlNodeId)).append("<br/><br/>");
        sb.append("<b>SQL Type</b><br/>").append(escapeHtml(sqlType)).append("<br/><br/>");
        sb.append("<b>SQL</b><br/>").append(escapeHtml(sqlText)).append("<br/><br/>");
        sb.append("<b>Error/Suggestion</b><br/>").append(escapeHtml(isBlank(parseResult) ? "（无）" : parseResult));
        sb.append("</html>");
        JEditorPane contextPane = new JEditorPane("text/html", sb.toString());
        contextPane.setEditable(false);
        contextPane.setBackground(UIUtil.getEditorPaneBackground());
        panel.add(contextPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 刷新回答区(EDT)
     */
    private void flushAnswer() {
        String text;
        synchronized (answerBuffer) {
            if (answerBuffer.length() == 0) {
                return;
            }
            text = answerBuffer.toString();
        }
        String html = "<html><body>" + MarkdownRenderer.toHtml(text) + "</body></html>";
        answerPane.setText(html);
        answerPane.setCaretPosition(answerPane.getDocument().getLength());
    }

    private static boolean isBlank(String s) {
        return null == s || s.trim().isEmpty();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
    }
}
