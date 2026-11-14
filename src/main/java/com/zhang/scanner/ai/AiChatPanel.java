package com.zhang.scanner.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.zhang.scanner.settings.ai.AiModelSettingsPersistent;
import com.zhang.scanner.settings.ai.AiProviderConfig;
import com.zhang.scanner.settings.ai.AiProviderRegistry;
import com.zhang.scanner.utils.LanguageUtil;
import com.zhang.scanner.utils.MyLogUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;

/**
 * AI 对话窗口(单轮, 流式输出)
 * 布局: 顶部标题(Show Context 弹框预览提示词 / Copy Text 复制提示词) /
 * 中部流式回答区 / 底部状态栏(Stop/Back)
 */
public class AiChatPanel {

    private final Project project;
    private final String sqlNodeId;
    private final String sqlType;
    private final String rawXml;
    private final String sqlText;
    private final String parseResult;
    private final Runnable onBack;

    /**
     * 实际回答语言是否为中文(构造时按偏好+IDE 语言解析, 供缓存命中比较)
     */
    private final boolean aiAnswerChinese;

    /**
     * 当前激活供应商配置快照(构造时读取, 供缓存命中比较与请求组装)
     */
    private final String providerId;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final AiProviderRegistry.Protocol protocol;
    private final AiProviderRegistry.ThinkingMode thinkingMode;

    private JPanel mainPanel;
    private JEditorPane answerPane;
    private JScrollPane answerScrollPane;
    private JLabel statusLabel;
    private JButton stopButton;
    private JButton copyTextButton;

    /**
     * 思考过程面板: 流式显示思考内容(固定高度), 正式回答开始后自动折叠, 点击标题展开/收起
     */
    private JPanel thinkingPanel;
    private JLabel thinkingTitleLabel;
    private JTextArea thinkingArea;
    private JScrollPane thinkingScroll;

    /**
     * 思考内容累积缓冲(后台线程写, EDT 读)
     */
    private final StringBuilder thinkingBuffer = new StringBuilder();

    /**
     * 是否已产生思考内容(EDT)
     */
    private volatile boolean thinkingStarted = false;

    /**
     * 思考面板当前是否折叠(仅 EDT 读写)
     */
    private boolean thinkingCollapsed = false;

    /**
     * 完整提示词(system + user 全文, 供弹框预览与复制)
     */
    private String fullPrompt = "";

    /**
     * 累积的回答文本(后台线程写, EDT 读)
     */
    private final StringBuilder answerBuffer = new StringBuilder();

    /**
     * 上次刷新时间(节流用, 仅后台线程读写)
     */
    private volatile long lastFlush = 0;

    /**
     * 回答区文本字号(px, 通过 HTMLEditorKit 的 CSS 规则生效)
     * 构造时从持久化读取, A-/A+ 调整时写回持久化
     */
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final int MIN_FONT_SIZE = 10;
    private static final int MAX_FONT_SIZE = 24;
    private static final int FONT_SIZE_STEP = 2;
    private volatile int answerFontSize = DEFAULT_FONT_SIZE;

    /**
     * 思考内容累计字符数(后台线程累加, EDT 读, 用于状态栏显示思考进度)
     */
    private volatile int thinkingChars = 0;

    /**
     * 上次思考进度刷新时间(节流用, 仅后台线程读写)
     */
    private volatile long lastThinkingFlush = 0;

    /**
     * 状态栏当前是否显示思考中(仅 EDT 读写)
     */
    private volatile boolean thinkingShown = false;

    private volatile boolean started = false;
    private volatile boolean stoppedByUser = false;
    private volatile boolean completed = false;
    private volatile boolean failed = false;
    private AiApiClient.StreamRequest streamRequest;

    public AiChatPanel(@NotNull Project project,
                       @NotNull String sqlNodeId,
                       @NotNull String sqlType,
                       @NotNull String rawXml,
                       @NotNull String sqlText,
                       @NotNull String parseResult,
                       @NotNull Runnable onBack) {
        this.project = project;
        this.sqlNodeId = sqlNodeId;
        this.sqlType = sqlType;
        this.rawXml = rawXml;
        this.sqlText = sqlText;
        this.parseResult = parseResult;
        this.onBack = onBack;
        this.aiAnswerChinese = AiModelSettingsPersistent.getInstance().isAiAnswerChinese();

        // 当前激活供应商配置: 按 id 取, 无槽位时用预设默认值(只读, 不落盘; DeepSeek 兜底)
        AiModelSettingsPersistent settings = AiModelSettingsPersistent.getInstance();
        AiProviderConfig config = settings.getActiveConfig();
        if (null == config) {
            config = settings.getConfigOrDefault(settings.activeProvider);
        }
        this.providerId = null == config.id ? "custom" : config.id;
        this.apiUrl = null == config.apiUrl ? "" : config.apiUrl;
        this.apiKey = null == config.apiKey ? "" : config.apiKey;
        this.model = null == config.model ? "" : config.model;
        AiProviderRegistry.Preset preset = AiProviderRegistry.byId(config.id);
        this.protocol = preset.protocol;
        this.thinkingMode = preset.thinkingMode;

        // 回答区字号: 从持久化读取(超出范围回退默认), A-/A+ 调整时写回
        int persistedSize = settings.answerFontSize;
        this.answerFontSize = (persistedSize >= MIN_FONT_SIZE && persistedSize <= MAX_FONT_SIZE)
                ? persistedSize : DEFAULT_FONT_SIZE;
        buildUi();
    }

    /**
     * 本面板实际回答语言是否中文(供外部缓存命中比较:
     * 偏好字面值不同但解析后语言相同(如 en→auto 且 IDE 为英文)时仍可复用缓存)
     */
    public boolean isAnswerChinese() {
        return aiAnswerChinese;
    }

    /**
     * 本面板使用的供应商 id(缓存命中比较: 切供应商后不复用旧回答)
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * 本面板使用的 API URL(缓存命中比较: URL 变更后不复用旧回答)
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * 本面板使用的模型名(缓存命中比较: 模型变更后不复用旧回答)
     */
    public String getModel() {
        return model;
    }

    @NotNull
    public JComponent getComponent() {
        return mainPanel;
    }

    /**
     * 是否已正常完成回答(停止/失败返回 false)
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * 面板是否可复用(供缓存命中判断):
     * 已正常完成(直接复用, 省 Token)或仍在生成中(切走再回来继续看流式输出, 不重复调用模型)
     * 用户主动停止或失败后返回 false, 重新点击 Ask AI 应重新生成
     */
    public boolean isReusable() {
        return completed || (started && !stoppedByUser && !failed);
    }

    /**
     * 发起请求(幂等, 已启动则忽略)
     */
    public void start() {
        if (started) {
            return;
        }
        started = true;

        if (isBlank(apiUrl) || isBlank(apiKey)) {
            statusLabel.setForeground(JBColor.RED);
            statusLabel.setText(LanguageUtil.zhOrEn(
                    "Please configure API URL and API Key in Model Settings first",
                    "请先在 Model Settings 中配置 API URL 与 API Key"));
            return;
        }

        statusLabel.setForeground(JBColor.GRAY);
        statusLabel.setText(LanguageUtil.zhOrEn("Generating...", "生成中…"));
        stopButton.setEnabled(true);

        // AI 回答语言: 由用户偏好解析(zh=中文, en=英文, auto=跟随 IDE), 与 UI 国际化无关
        boolean chinese = aiAnswerChinese;

        String systemPrompt = AiPromptTemplate.buildSystemPrompt(chinese);
        String userPrompt = AiPromptTemplate.buildUserPrompt(chinese, sqlType, rawXml, sqlText, parseResult);
        // 保存完整提示词(原封不动: system + user 原文拼接, 供 Show Prompt 弹框预览与复制)
        this.fullPrompt = systemPrompt + "\n\n" + userPrompt;

        // 按当前激活供应商配置与预设(协议/thinking 策略)发起流式请求
        streamRequest = AiApiClient.startStream(apiUrl, apiKey, model,
                systemPrompt, userPrompt, protocol, thinkingMode,
                new AiApiClient.StreamCallback() {
                    @Override
                    public void onDelta(String text) {
                        // 后台线程: 累积 + 节流刷新
                        synchronized (answerBuffer) {
                            answerBuffer.append(text);
                        }
                        long now = System.currentTimeMillis();
                        if (now - lastFlush > 100) {
                            lastFlush = now;
                            ApplicationManager.getApplication().invokeLater(() -> {
                                // 正式内容开始输出: 状态栏从"思考中"切回"生成中"
                                if (thinkingShown) {
                                    thinkingShown = false;
                                    statusLabel.setForeground(JBColor.GRAY);
                                    statusLabel.setText(LanguageUtil.zhOrEn("Generating...", "生成中…"));
                                }
                                // 思考面板自动折叠收起(点标题可展开)
                                if (thinkingStarted && !thinkingCollapsed) {
                                    thinkingCollapsed = true;
                                    thinkingScroll.setVisible(false);
                                    thinkingTitleLabel.setText(LanguageUtil.zhOrEn(
                                            "Thinking (" + thinkingChars + " chars) ▸",
                                            "思考过程 (" + thinkingChars + " 字) ▸"));
                                    thinkingPanel.revalidate();
                                }
                                flushAnswer();
                            });
                        }
                    }

                    @Override
                    public void onThinking(String text) {
                        // 后台线程: 思考内容增量(思考型模型流式输出 reasoning_content)
                        // 思考面板实时流式显示(固定高度自动滚底), 状态栏同步显示进度
                        synchronized (thinkingBuffer) {
                            thinkingBuffer.append(text);
                        }
                        thinkingChars += text.length();
                        long now = System.currentTimeMillis();
                        if (now - lastThinkingFlush > 100) {
                            lastThinkingFlush = now;
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (!thinkingStarted) {
                                    // 首个思考片段: 显示思考面板(展开态)
                                    thinkingStarted = true;
                                    thinkingPanel.setVisible(true);
                                    thinkingCollapsed = false;
                                    thinkingScroll.setVisible(true);
                                    thinkingTitleLabel.setText(LanguageUtil.zhOrEn("Thinking…", "思考中…"));
                                    thinkingPanel.revalidate();
                                }
                                synchronized (thinkingBuffer) {
                                    thinkingArea.setText(thinkingBuffer.toString());
                                }
                                // 自动滚到底部, 始终显示最新思考内容
                                javax.swing.JScrollBar bar = thinkingScroll.getVerticalScrollBar();
                                bar.setValue(bar.getMaximum());
                                thinkingShown = true;
                                statusLabel.setForeground(JBColor.GRAY);
                                statusLabel.setText(LanguageUtil.zhOrEn(
                                        "Thinking… (" + thinkingChars + " chars)",
                                        "思考中… (已思考 " + thinkingChars + " 字)"));
                            });
                        }
                    }

                    @Override
                    public void onDone() {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // 总是渲染: 补上用户向上浏览期间累积的内容(保持其滚动位置)
                            flushAnswer(false);
                            stopButton.setEnabled(false);
                            thinkingShown = false;
                            statusLabel.setForeground(JBColor.GRAY);
                            statusLabel.setText(stoppedByUser
                                    ? LanguageUtil.zhOrEn("Stopped", "已停止")
                                    : LanguageUtil.zhOrEn("Completed", "已完成"));
                            // 仅正常完成才允许缓存复用(停止/失败后重新点击应重新生成)
                            completed = !stoppedByUser;
                        });
                    }

                    @Override
                    public void onError(String friendlyMessage) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            flushAnswer(false);
                            failed = true;
                            stopButton.setEnabled(false);
                            thinkingShown = false;
                            statusLabel.setForeground(JBColor.RED);
                            statusLabel.setText(LanguageUtil.zhOrEn("Failed: ", "失败：") + friendlyMessage);
                            MyLogUtil.error(LanguageUtil.zhOrEn(
                                    "Ask AI request failed: ", "Ask AI 请求失败：") + friendlyMessage);
                        });
                    }
                });
    }

    /**
     * 构建界面
     */
    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout());

        // ===== 顶部: 标题 + 模型名 + Show Context / Copy Text =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(JBUI.Borders.empty(8, 10, 4, 10));

        JLabel titleLabel = new JLabel(LanguageUtil.zhOrEn("AI SQL Analysis", "AI SQL 分析"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        // 模型名来自本次面板的配置快照(旧字段 settings.model 在 apply 后已置空, 不能再读)
        JLabel modelLabel = new JLabel(model);
        modelLabel.setForeground(JBColor.GRAY);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.add(titleLabel);
        titleRow.add(modelLabel);

        // Show Prompt: 弹框展示发给 AI 的完整提示词
        JButton showPromptButton = new JButton(LanguageUtil.zhOrEn("Show Prompt", "显示提示词"));
        showPromptButton.setToolTipText(LanguageUtil.zhOrEn(
                "View the full prompt sent to AI", "查看发给 AI 的完整提示词"));
        showPromptButton.addActionListener(e -> showPromptDialog());

        // Copy Answer: 复制 AI 回答全文到剪贴板
        copyTextButton = new JButton(LanguageUtil.zhOrEn("Copy Answer", "复制回答"));
        copyTextButton.setToolTipText(LanguageUtil.zhOrEn(
                "Copy the full AI answer as plain text", "复制 AI 回答全文(纯文本)"));
        copyTextButton.addActionListener(e -> copyAnswerToClipboard());

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topButtons.setOpaque(false);
        topButtons.add(showPromptButton);
        topButtons.add(copyTextButton);

        topPanel.add(titleRow, BorderLayout.WEST);
        topPanel.add(topButtons, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // ===== 中部: 思考过程面板(上, 固定高度, 回答开始后自动折叠) + 流式回答区(下) =====
        answerPane = new JEditorPane("text/html", "");
        answerPane.setEditable(false);
        answerPane.setBackground(UIUtil.getEditorPaneBackground());
        answerScrollPane = new JScrollPane(answerPane);
        answerScrollPane.setBorder(JBUI.Borders.empty());

        // 思考内容区: 只读多行文本, 斜体灰色, 固定高度约 6 行, 自动滚底显示最新内容
        thinkingArea = new JTextArea();
        thinkingArea.setEditable(false);
        thinkingArea.setLineWrap(true);
        thinkingArea.setWrapStyleWord(true);
        thinkingArea.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, Math.max(10, answerFontSize - 2)));
        thinkingArea.setForeground(JBColor.GRAY);
        thinkingArea.setBackground(UIUtil.getEditorPaneBackground());
        thinkingArea.setBorder(JBUI.Borders.empty(2, 6, 2, 6));
        thinkingScroll = new JScrollPane(thinkingArea);
        thinkingScroll.setBorder(JBUI.Borders.empty());
        thinkingScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        thinkingScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        thinkingScroll.setPreferredSize(new java.awt.Dimension(0, 110));

        // 思考标题行: 点击切换折叠/展开
        thinkingTitleLabel = new JLabel(" ");
        thinkingTitleLabel.setFont(thinkingTitleLabel.getFont().deriveFont(Font.BOLD, 12f));
        thinkingTitleLabel.setForeground(JBColor.GRAY);
        thinkingTitleLabel.setBorder(JBUI.Borders.empty(2, 6, 0, 6));
        thinkingTitleLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        thinkingTitleLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleThinkingCollapsed();
            }
        });

        thinkingPanel = new JPanel(new BorderLayout());
        thinkingPanel.setBackground(UIUtil.getEditorPaneBackground());
        thinkingPanel.setBorder(JBUI.Borders.empty(2, 0, 2, 0));
        thinkingPanel.add(thinkingTitleLabel, BorderLayout.NORTH);
        thinkingPanel.add(thinkingScroll, BorderLayout.CENTER);
        // 无思考内容时整体隐藏
        thinkingPanel.setVisible(false);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(thinkingPanel, BorderLayout.NORTH);
        centerPanel.add(answerScrollPane, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // ===== 底部: 状态栏 =====
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(JBUI.Borders.empty(0, 4, 0, 0));

        stopButton = new JButton(LanguageUtil.zhOrEn("Stop", "停止"));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> {
            stoppedByUser = true;
            if (null != streamRequest) {
                streamRequest.cancel();
            }
            statusLabel.setForeground(JBColor.GRAY);
            statusLabel.setText(LanguageUtil.zhOrEn("Stopping...", "正在停止…"));
        });

        JButton backButton = new JButton(LanguageUtil.zhOrEn("Back", "返回"));
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

        // 文本字号调节(A- / A+)
        JButton decreaseFontButton = new JButton("A-");
        decreaseFontButton.setToolTipText(LanguageUtil.zhOrEn("Decrease text size", "缩小文字"));
        decreaseFontButton.addActionListener(e -> {
            if (answerFontSize > MIN_FONT_SIZE) {
                answerFontSize -= FONT_SIZE_STEP;
                applyAnswerFontSize();
                // 写回持久化, 下次打开对话窗口沿用
                AiModelSettingsPersistent.getInstance().answerFontSize = answerFontSize;
            }
        });
        JButton increaseFontButton = new JButton("A+");
        increaseFontButton.setToolTipText(LanguageUtil.zhOrEn("Increase text size", "放大文字"));
        increaseFontButton.addActionListener(e -> {
            if (answerFontSize < MAX_FONT_SIZE) {
                answerFontSize += FONT_SIZE_STEP;
                applyAnswerFontSize();
                // 写回持久化, 下次打开对话窗口沿用
                AiModelSettingsPersistent.getInstance().answerFontSize = answerFontSize;
            }
        });
        JPanel fontButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        fontButtonRow.setOpaque(false);
        fontButtonRow.add(decreaseFontButton);
        fontButtonRow.add(increaseFontButton);
        bottomPanel.add(fontButtonRow, BorderLayout.WEST);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(stopButton);
        buttonRow.add(backButton);
        bottomPanel.add(buttonRow, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 初始化字体规则(sans-serif + 默认字号), 保证 ASCII 符号用西文字形渲染
        applyAnswerFontSize();
    }

    /**
     * 点击思考标题切换折叠/展开(EDT)
     */
    private void toggleThinkingCollapsed() {
        if (!thinkingStarted) {
            return;
        }
        thinkingCollapsed = !thinkingCollapsed;
        thinkingScroll.setVisible(!thinkingCollapsed);
        if (thinkingCollapsed) {
            thinkingTitleLabel.setText(LanguageUtil.zhOrEn(
                    "Thinking (" + thinkingChars + " chars) ▸",
                    "思考过程 (" + thinkingChars + " 字) ▸"));
        } else {
            thinkingTitleLabel.setText(thinkingShown
                    ? LanguageUtil.zhOrEn("Thinking…", "思考中…")
                    : LanguageUtil.zhOrEn("Thinking ▾", "思考过程 ▾"));
        }
        thinkingPanel.revalidate();
        thinkingPanel.repaint();
    }

    /**
     * 弹框展示发给 AI 的完整提示词(原封不动; IDEA 原生 DialogWrapper 风格, 支持 ESC 关闭)
     * 底部按钮: Copy(复制整个 Prompt) + Close(关闭)
     */
    private void showPromptDialog() {
        DialogWrapper wrapper = new DialogWrapper(true) {
            private DialogWrapperAction copyAction;

            {
                setTitle(LanguageUtil.zhOrEn("Prompt Preview", "提示词预览"));
                setOKButtonText(LanguageUtil.zhOrEn("Close", "关闭"));
                init();
            }

            @Nullable
            @Override
            protected JComponent createCenterPanel() {
                JTextArea textArea = new JTextArea();
                textArea.setText(isBlank(fullPrompt)
                        ? LanguageUtil.zhOrEn("(Prompt not ready)", "（提示词尚未生成）")
                        : fullPrompt);
                textArea.setEditable(false);
                textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new java.awt.Dimension(640, 480));
                return scrollPane;
            }

            @NotNull
            @Override
            protected javax.swing.Action @NotNull [] createActions() {
                // Copy: 复制整个 Prompt(成功后按钮反馈 Copied/已复制, 停留 1.5s 回显); Close: 关闭
                copyAction = new DialogWrapperAction(LanguageUtil.zhOrEn("Copy", "复制")) {
                    @Override
                    protected void doAction(java.awt.event.ActionEvent e) {
                        CopyPasteManager.getInstance().setContents(new StringSelection(fullPrompt));
                        showCopiedFeedback();
                    }
                };
                return new javax.swing.Action[]{copyAction, getOKAction()};
            }

            /**
             * 复制成功反馈: 按钮短暂显示 Copied/已复制, 1.5s 后回显 Copy/复制
             */
            private void showCopiedFeedback() {
                JButton copyButton = getButton(copyAction);
                if (null == copyButton) {
                    return;
                }
                copyButton.setText(LanguageUtil.zhOrEn("Copied", "已复制"));
                Timer timer = new Timer(1500, ev -> copyButton.setText(
                        LanguageUtil.zhOrEn("Copy", "复制")));
                timer.setRepeats(false);
                timer.start();
            }
        };
        wrapper.show();
    }

    /**
     * 复制 AI 回答全文到剪贴板(纯文本)
     */
    private void copyAnswerToClipboard() {
        String answer;
        synchronized (answerBuffer) {
            answer = answerBuffer.toString();
        }
        if (isBlank(answer)) {
            statusLabel.setForeground(JBColor.GRAY);
            statusLabel.setText(LanguageUtil.zhOrEn("No answer to copy yet", "暂无回答内容"));
            return;
        }
        CopyPasteManager.getInstance().setContents(new StringSelection(answer));
        // 按钮短暂反馈
        copyTextButton.setText(LanguageUtil.zhOrEn("Copied ✓", "已复制 ✓"));
        Timer timer = new Timer(1500, ev -> copyTextButton.setText(
                LanguageUtil.zhOrEn("Copy Answer", "复制回答")));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 刷新回答区(EDT, 流式增量用)
     * 滚动策略: 用户停留在底部时才重渲染并跟随最新内容;
     * 用户往上拖动浏览时完全跳过重渲染(内容继续在后台累积),
     * 彻底避免 setText 重建视图打断滚动条拖动
     */
    private void flushAnswer() {
        flushAnswer(true);
    }

    /**
     * 刷新回答区(EDT)
     *
     * @param onlyWhenAtBottom true=仅在底部时渲染(流式增量, 避免打断拖动);
     *                         false=总是渲染(回答完成时补渲染剩余内容, 保持用户滚动位置)
     */
    private void flushAnswer(boolean onlyWhenAtBottom) {
        javax.swing.JScrollBar bar = answerScrollPane.getVerticalScrollBar();
        boolean atBottom = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 8;
        if (onlyWhenAtBottom && !atBottom) {
            // 用户正在向上浏览: 跳过本次渲染, 不打扰其滚动/拖动
            return;
        }

        String text;
        synchronized (answerBuffer) {
            if (answerBuffer.length() == 0) {
                return;
            }
            text = answerBuffer.toString();
        }
        int oldValue = bar.getValue();

        // 显式声明 HTML 内容类型, 保证 Markdown 转换结果被渲染而非按纯文本展示
        answerPane.setContentType("text/html");
        String html = "<html><body>" + MarkdownRenderer.toHtml(text) + "</body></html>";
        answerPane.setText(html);

        if (atBottom) {
            // 用户本来就在底部: 跟随最新内容
            answerPane.setCaretPosition(answerPane.getDocument().getLength());
        } else {
            // 用户正在往上浏览: 恢复原滚动位置
            bar.setValue(oldValue);
        }
    }

    /**
     * 应用字号与字体到回答区(EDT)
     * HTMLEditorKit 支持有限的 CSS 子集, body 规则可全局生效;
     * font-family: sans-serif 保证 ${} # 等 ASCII 字符用西文字形渲染
     * (默认字体回退到 CJK 字形时, 这些符号会显示为全角宽度)
     */
    private void applyAnswerFontSize() {
        HTMLEditorKit kit = (HTMLEditorKit) answerPane.getEditorKit();
        kit.getStyleSheet().addRule("body { font-family: sans-serif; font-size: "
                + answerFontSize + "px; }");
        // 重新加载当前内容使新字号生效, 同时保持滚动位置
        javax.swing.JScrollBar bar = answerScrollPane.getVerticalScrollBar();
        int oldValue = bar.getValue();
        String current = answerPane.getText();
        if (null != current && !current.isEmpty()) {
            answerPane.setText(current);
            bar.setValue(oldValue);
        }
        // 思考区字号随回答区联动(比回答区小 2px, 斜体)
        if (null != thinkingArea) {
            thinkingArea.setFont(new Font(Font.SANS_SERIF, Font.ITALIC,
                    Math.max(10, answerFontSize - 2)));
        }
    }

    private static boolean isBlank(String s) {
        return null == s || s.trim().isEmpty();
    }
}
