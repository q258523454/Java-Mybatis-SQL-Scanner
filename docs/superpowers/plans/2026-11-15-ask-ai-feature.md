# Ask AI 功能实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 subagent-driven-development（推荐）或 executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在 SQL 详情预览面板新增 "Ask AI" 按钮，将 SQL 上下文发给 DeepSeek 等 OpenAI 兼容模型做流式分析，并新增 Model Settings 设置页（含 Test Connection）。

**架构：** 预览面板新增按钮 → 组装固定提示词模板 → 后台线程经 JDK `HttpClient` 发起 SSE 流式请求 → 增量渲染到新 AI 对话面板（替换右侧窗口）。设置页重构为父节点 + Rules/Model 两个子页；模型配置由新的 applicationService 持久化（API Key 明文）。

**技术栈：** Java 21、IntelliJ Platform（编译目标 2025.1）、JDK `java.net.http.HttpClient`、平台自带 Gson、IntelliJ GUI Designer（.form）。

**规格：** `docs/superpowers/specs/2026-11-15-ask-ai-feature-design.md`（执行者两份都读）

## 全局约束

- 文档、注释、提交说明一律中文。
- **本项目不写单元测试**（用户明确要求人工验证）；每个任务的验证 = `compileJava`/`buildPlugin` 编译通过 + 人工验证点。
- 构建命令：`GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew buildPlugin`（默认 `~/.gradle` 几乎为空，勿改）。
- 不引入任何 Gradle 依赖：HTTP 用 JDK HttpClient，JSON 用 IDE 平台自带 Gson（`com.google.gson`，平台 classpath 已含）。若 Gson 编译不可见，仅可加 `compileOnly` 依赖，打包不携带。
- 不改任何原有扫描/预览/规则逻辑；`RuleResultViewAction`、`ConsoleToolRunner`、`TreeConsoleExecutor` 等文件**零改动**。
- API Key 明文存储于 `AiModelSettings.xml`（用户拍板）；设置页输入用 `JPasswordField` 密文，眼睛按钮切换明文。
- 网络请求一律后台线程，UI 更新一律 EDT（`invokeLater`）。
- 按钮/标题等 UI 文案英文（与现有 "Open File" 风格一致）；AI 提示词模板文案随 `LanguageUtil.isChinese()` 中英切换。
- 每个任务 commit 时**只 `git add` 本任务涉及的文件**；工作区此前未提交的 P0 修复文件（如 `MapperXmlHelper.java`、`RunAction.java` 等）不属于本计划，禁止一并提交。

## 审查重点（Review Focus）

本计划无自动化测试，以下场景必须在对应任务的人工验证中逐条验证：

1. **apiKey 为空时点 Ask AI** → 弹友好提示 + 提供「配置」按钮直达 Model Settings 页；不 NPE、不弹原始异常。
2. **流式响应中途断网** → 已接收内容保留，状态显示"失败/已停止"，无崩溃、无 EDT 异常。
3. **代理返回非 SSE 响应**（完整 JSON）→ 走非流式兜底，回答正常显示。
4. **API Key 错误（401）** → 对话窗口显示 `error.message` 友好文案 + IDEA 通知；不弹堆栈。
5. **生成中点 Stop** → 输出立即停止、状态"已停止"、后台线程正常退出（无泄漏）；再次点击其他树节点功能正常。

---

## 任务 1：设置结构重构 + 模型配置持久化

**文件：**
- 创建：`src/main/java/com/zhang/scanner/settings/ai/ParentSettingsConfig.java`
- 创建：`src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsPersistent.java`
- 创建：`src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsConfig.java`
- 修改：`src/main/java/com/zhang/scanner/settings/MySettingsConfig.java:21-23`（仅 `getDisplayName()` 返回值）
- 修改：`src/main/resources/META-INF/plugin.xml:90-96`（设置注册段）

- [ ] **步骤 1：创建 `ParentSettingsConfig`**

`public class ParentSettingsConfig implements SearchableConfigurable.Parent`：
- `getId()` 返回 `"zhang.scanner.settings.parent"`
- `getDisplayName()` 返回 `"Java Mybatis SQL Scanner Rules"`
- 无子逻辑（纯分组节点，不需要 createComponent）。

- [ ] **步骤 2：创建 `AiModelSettingsPersistent`**

`@State(name = "AiModelSettingsPersistent", storages = @Storage("AiModelSettings.xml"))`，实现 `PersistentStateComponent<AiModelSettingsPersistent>`：
- `public String apiUrl = "https://api.deepseek.com/v1/chat/completions";`
- `public String apiKey = "";`
- `public String model = "deepseek-v4-pro";`
- `public static AiModelSettingsPersistent getInstance()`（`ApplicationManager.getApplication().getService(...)`）
- `getState()/loadState()` 用 `XmlSerializerUtil.copyBean`（照抄 `MySettingsPersistent` 模式）。

- [ ] **步骤 3：创建 `AiModelSettingsConfig`（Model Settings 表单）**

`public class AiModelSettingsConfig implements Configurable`，参照 `MySettingsConfig` 的结构（`createComponent()/isModified()/apply()/reset()`）：
- `getDisplayName()` 返回 `"Model Settings"`；
- 表单字段：`JTextField apiUrlField`、`JPasswordField apiKeyField`、`JComboBox<String> modelCombo`（可编辑，items：`deepseek-v4-pro`、`deepseek-flash`）、`JButton eyeButton`（图标 `AllIcons.Actions.Preview`，tooltip "Show/Hide API Key"，点击在 `echoChar((char)0)` 与 `'•'` 间切换）；
- 布局：`JBUI.Panels.simplePanel` + 竖向两列（label 左、输入右），整体留白适度；label 文案："API URL"、"API Key"、"Model"；
- `isModified()`：任一字段与持久化值不同；`apply()`：三字段写回 `AiModelSettingsPersistent`；`reset()`：从持久化读取到表单。

- [ ] **步骤 4：修改 `MySettingsConfig.getDisplayName()`**

返回值由 `"Java Mybatis SQL Scanner Rules"` 改为 `"Rules Settings"`。**其余代码零改动。**

- [ ] **步骤 5：修改 `plugin.xml` 设置注册段**

替换第 91-96 行为：
```xml
<applicationConfigurable parentId="configurable.group.other"
                         instance="com.zhang.scanner.settings.ai.ParentSettingsConfig"
                         id="zhang.scanner.settings.parent"
                         displayName="Java Mybatis SQL Scanner Rules"/>
<applicationConfigurable parentId="zhang.scanner.settings.parent"
                         instance="com.zhang.scanner.settings.MySettingsConfig"
                         id="MySettingsConfig"
                         displayName="Rules Settings"/>
<applicationConfigurable parentId="zhang.scanner.settings.parent"
                         instance="com.zhang.scanner.settings.ai.AiModelSettingsConfig"
                         id="zhang.scanner.settings.ai.model"
                         displayName="Model Settings"/>
<applicationService serviceImplementation="com.zhang.scanner.settings.ai.AiModelSettingsPersistent"/>
```
同时在 `change-notes` 中追加本次功能说明（英文，格式照旧）。

- [ ] **步骤 6：编译验证**

运行：`GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew buildPlugin --console=plain`
预期：BUILD SUCCESSFUL。

- [ ] **步骤 7：人工验证点**

- 设置树：`Other Settings > Java Mybatis SQL Scanner Rules` 下出现 `Rules Settings`、`Model Settings` 两个子页；
- Rules Settings 页面与之前完全一致（表格、开关、Apply/Reset）；
- Model Settings：API Key 输入为密文圆点，点眼睛切换明文/密文；
- 填好三字段 Apply 后重启 IDEA：配置保留；`~/Library/Application Support/JetBrains/IntelliJIdea2026.1/options/AiModelSettings.xml` 中出现明文 apiKey。

- [ ] **步骤 8：Commit**

```bash
git add src/main/java/com/zhang/scanner/settings/ai/ParentSettingsConfig.java \
        src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsPersistent.java \
        src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsConfig.java \
        src/main/java/com/zhang/scanner/settings/MySettingsConfig.java \
        src/main/resources/META-INF/plugin.xml
git commit -m "feat: 设置页重构为父节点+Rules/Model 两个子页, 新增模型配置持久化"
```

---

## 任务 2：Test Connection 测试按钮

**文件：**
- 创建：`src/main/java/com/zhang/scanner/ai/AiApiClient.java`（本任务只实现 `testConnection`）
- 修改：`src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsConfig.java`（加按钮与结果展示）

- [ ] **步骤 1：在 `AiApiClient` 实现 `testConnection`**

`public static TestResult testConnection(String apiUrl, String apiKey, String model)`（同步阻塞，调用方负责后台线程）：
- `public static class TestResult { public final boolean ok; public final String message; }`
- 流程：由 apiUrl 推导 base（`URI` 的 scheme://host[:port] + `/v1` 路径前缀）→ `GET {base}/models`，头 `Authorization: Bearer {apiKey}`（用 `HttpClient`，连接超时 10s）→
  - 200：解析 `data[].id`，message = `"Connection successful. Models: a, b, c"`（最多列 3 个，超出加 `...`）；
  - 404/405：降级 `POST {apiUrl}` 非流式极小请求（`messages` 只含一条 "ping"、`max_tokens=1`）→ 200 则 ok；
  - 401/403：message 含状态码与 `error.message`（Gson 解析响应体）；网络异常：message 为连接失败原因。

- [ ] **步骤 2：在 `AiModelSettingsConfig` 加 Test Connection 按钮与结果标签**

- 表单底部一行：`JButton testButton`（text "Test Connection"）+ `JLabel testResultLabel`（初始空）；
- 点击：用**表单当前值**（未 Apply 也可）→ `ApplicationManager.getApplication().executeOnPooledThread` 执行 `testConnection` → 回 EDT：成功 `✓ ` + 绿色 message；失败 `✗ ` + 红色 message（`JBColor` 或 `UIUtil` 标准错误色）；执行期间 testButton 禁用、text 为 "Testing..."。

- [ ] **步骤 3：编译验证**

运行：`GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew buildPlugin --console=plain`
预期：BUILD SUCCESSFUL。

- [ ] **步骤 4：人工验证点**

- 正确 Key → 绿色 ✓ 显示可用模型列表；
- 错误 Key → 红色 ✗ 显示 401 错误原因；
- 断网 → 红色 ✗ 连接失败；测试期间 UI 不卡顿、按钮禁用。

- [ ] **步骤 5：Commit**

```bash
git add src/main/java/com/zhang/scanner/ai/AiApiClient.java \
        src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsConfig.java
git commit -m "feat: Model Settings 新增 Test Connection 测试按钮"
```

---

## 任务 3：提示词模板 + 流式 API 客户端

**文件：**
- 创建：`src/main/java/com/zhang/scanner/ai/AiPromptTemplate.java`
- 修改：`src/main/java/com/zhang/scanner/ai/AiApiClient.java`（加 `streamChat`）

- [ ] **步骤 1：创建 `AiPromptTemplate`**

`public final class AiPromptTemplate`，两个静态方法（文案钉死，不可自行发挥）：

`public static String buildSystemPrompt(boolean chinese)`：
- 中文：`"你是一名资深数据库专家与 SQL 优化顾问，擅长 MySQL、Oracle、DB2、PostgreSQL 等主流数据库的性能分析与 SQL 调优。请根据用户提供的 SQL 上下文，指出 SQL 存在的问题、风险与可优化之处，并给出具体、可执行的优化建议。回答请使用中文。"`
- 英文：语义一致的英文版（建造者撰写，需与中文版信息量相当）。

`public static String buildUserPrompt(String sqlNodeId, String sqlType, String sqlText, String parseResult)`，模板（`\n` 换行）：
```
请分析以下 MyBatis 映射文件中的 SQL：

【SQL ID】
{sqlNodeId}

【SQL Type】
{sqlType}

【格式化后的 SQL】
```sql
{sqlText}
```

【报错信息或建议】
{parseResult 或 "（无）"}

请从正确性、性能、索引使用、潜在风险等角度分析这条 SQL 是否存在问题或需要优化的地方。
```

- [ ] **步骤 2：在 `AiApiClient` 实现流式聊天**

```java
public interface StreamCallback {
    void onDelta(String text);      // 增量文本
    void onDone();                  // 正常结束
    void onError(String friendlyMessage); // 失败(友好文案)
}
public static StreamRequest startStream(String apiUrl, String apiKey, String model,
                                        String systemPrompt, String userPrompt, StreamCallback callback);
public static final class StreamRequest { public void cancel(); } // 关闭输入流中断
```
- 请求体 JSON（Gson）：`model`、`messages`（system + user）、`stream: true`、`thinking: {"type": "disabled"}`；
- 实现要点（后台线程、阻塞式读取）：
  - `BodyHandlers.ofInputStream()` + `BufferedReader` 按行读；`data: ` 前缀行 → 若为 `[DONE]` 结束；否则 Gson 解析取 `choices[0].delta.content`（可能 null，跳过）；
  - 首字节为 `{` 或 Content-Type 非 `text/event-stream` → 非流式兜底：读完整 JSON 取 `choices[0].message.content`；
  - 非 200：读响应体 `error.message`（无则用状态码文案），回调 `onError`；
  - `cancel()`：关闭底层 InputStream，使读循环退出后回调 `onDone()`（调用方据此显示"已停止"）；
- **回调线程约定**：三个回调都在后台线程触发，由调用方负责 `invokeLater` 回 EDT；连接超时 10s，流式读取不设整体超时。

- [ ] **步骤 3：编译验证**

运行：`GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew compileJava --console=plain`
预期：BUILD SUCCESSFUL。（本任务行为在任务 5 全链路中人工验证。）

- [ ] **步骤 4：Commit**

```bash
git add src/main/java/com/zhang/scanner/ai/AiPromptTemplate.java \
        src/main/java/com/zhang/scanner/ai/AiApiClient.java
git commit -m "feat: 新增 AI 提示词模板与 DeepSeek SSE 流式客户端"
```

---

## 任务 4：Markdown 渲染器 + AI 对话面板

**文件：**
- 创建：`src/main/java/com/zhang/scanner/ai/MarkdownRenderer.java`
- 创建：`src/main/java/com/zhang/scanner/ai/AiChatPanel.java`

- [ ] **步骤 1：创建 `MarkdownRenderer`**

`public final class MarkdownRenderer { public static String toHtml(String markdown); }`
- 轻量自写转换（不引第三方）：``` 代码块 → `<pre>`（等宽、`JBColor` 浅底）；行内 `code` → `<code>`；`#`~`###` 标题 → `<h3>` 等；`**粗体**` → `<b>`；`- ` 连续行 → `<ul>/<li>`；其余纯文本按 HTML 转义（`&<>` 等）；
- 未闭合代码块视为进行中（渲染为打开的 `<pre>` 即可，最终刷新收尾）。

- [ ] **步骤 2：创建 `AiChatPanel`**

```java
public class AiChatPanel {
    public AiChatPanel(@NotNull Project project, @NotNull String sqlNodeId, @NotNull String sqlType,
                       @NotNull String sqlText, @NotNull String parseResult, @NotNull Runnable onBack);
    public @NotNull JComponent getComponent();
    public void start(); // 发起请求(幂等, 已启动则忽略)
}
```
- 布局（`JBUI`/`JBColor` 适配暗色主题，简洁留白）：
  1. 标题栏：`JLabel` "AI SQL Analysis" + 模型名小标签（浅灰）；
  2. 上下文折叠区：默认折叠（可用 `JBCollapsiblePanel` 或等价手写折叠），四段展示 SQL ID / SQL Type / 格式化 SQL（等宽字体）/ 报错建议；
  3. 回答区：`JEditorPane`（contentType text/html，不可编辑）+ `JBScrollPane`；
  4. 底部状态栏：状态 `JLabel`（"生成中…"/"已完成"/"已停止"/错误文案）+「Stop」+「Back」按钮。
- 行为：
  - `start()`：校验配置（apiKey 空 → `onError` 提示去 Model Settings 配置）→ 状态"生成中…"、Stop 可用 → `ApplicationManager.executeOnPooledThread` 调 `AiApiClient.startStream`（模板用 `AiPromptTemplate`，语言按 `LanguageUtil.isChinese()`）；
  - 三个回调回 EDT：`onDelta` 累积到 `StringBuilder`，约每 100ms 或每 512 字符批量 `invokeLater` 渲染（`MarkdownRenderer.toHtml` 写入回答区，滚动到底）；`onDone` 最终刷新 + 状态"已完成"（cancel 触发的显示"已停止"）；`onError` 状态区显示错误文案 + `MyLogUtil` 弹 IDEA 通知；
  - Stop → `StreamRequest.cancel()`；Back → 调 `onBack` 回调；
  - 构造器不发起请求；面板非静态单例，每次新建。

- [ ] **步骤 3：编译验证**

运行：`GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew compileJava --console=plain`
预期：BUILD SUCCESSFUL。（本任务行为在任务 5 全链路中人工验证。）

- [ ] **步骤 4：Commit**

```bash
git add src/main/java/com/zhang/scanner/ai/MarkdownRenderer.java \
        src/main/java/com/zhang/scanner/ai/AiChatPanel.java
git commit -m "feat: 新增 AI 对话面板与 Markdown 渲染"
```

---

## 任务 5：Ask AI 按钮接入 + 全链路收尾

**文件：**
- 修改：`src/main/java/com/zhang/scanner/ui/SqlScanResultPanel.form`（Operation 行加按钮）
- 修改：`src/main/java/com/zhang/scanner/ui/SqlScanResultPanel.java`（新字段 + 监听器）
- 修改：`README.md`（功能说明，中文）

- [ ] **步骤 1：修改 `SqlScanResultPanel.form`**

- grid `column-count` 由 2 改为 3；
- 第 4 行（Operation）：col 1 = 现有 `openFileButton`（保持原位置参数），col 2 新增 `JButton`，`binding="askAiButton"`，`text="Ask AI"`，`toolTipText="Analyze SQL with AI"`；
- 其余组件位置不动。

- [ ] **步骤 2：修改 `SqlScanResultPanel.java` 新增监听器**

- 新增字段 `private JButton askAiButton;`（form binding）；
- 构造器中追加监听器（**原 Open File 监听器一行不动**）：
  1. 读自身字段：`sqlNodeIdOrg.getText()`、`sqlType.getText()`、`sqlText.getText()`、`sqlScanResult.getText()`；
  2. `TreeConsoleExecutor executor = (TreeConsoleExecutor) MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID);`（与 `RunAction` 既有用法一致）取 `project`、`splitterPanel`；
  3. 读 `AiModelSettingsPersistent.getInstance()`：`apiUrl`/`apiKey` 任一为空 → `Messages.showMessageDialog(project, "请先在 Model Settings 配置 API URL 与 API Key", ...)`，第二按钮「Configure」用 `ShowSettingsUtil.getInstance().showSettingsDialog(project, AiModelSettingsConfig.class)` 直达设置页，随后 return；
  4. `AiChatPanel chatPanel = new AiChatPanel(project, 四段上下文, onBack = () -> splitterPanel.setSecondComponent(this.mainPanel));`
  5. `splitterPanel.setSecondComponent(chatPanel.getComponent()); chatPanel.start();`

- [ ] **步骤 3：编译与构建验证**

运行：`GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew buildPlugin --console=plain`
预期：BUILD SUCCESSFUL，产物 `build/distributions/Java-Mybatis-SQL-Scanner-2025.1-PLUS*.zip`。

- [ ] **步骤 4：全链路人工验证（覆盖审查重点 1-5）**

1. 不填 API Key 点 Ask AI → 提示 + Configure 直达 Model Settings，无异常；
2. 正确配置点 Ask AI → 右侧变对话窗口、回答流式出现、"已完成"；
3. 使用不支持流式输出的代理（返回完整 JSON）→ 非流式兜底，回答正常显示；
4. 点其他树节点 → 预览恢复、Open File 正常；
5. 生成中点 Stop → "已停止"、无残留后台线程；
6. 错误 Key → 对话窗口显示 401 错误文案 + IDEA 通知；
7. 断网 → 友好错误文案；暗色主题下界面美观。

- [ ] **步骤 5：Commit**

```bash
git add src/main/java/com/zhang/scanner/ui/SqlScanResultPanel.form \
        src/main/java/com/zhang/scanner/ui/SqlScanResultPanel.java \
        README.md
git commit -m "feat: 预览面板接入 Ask AI 按钮, 完成全链路"
```

- [ ] **步骤 6：全量收尾构建**

运行：`GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew buildPlugin --console=plain`（全量、确认 5 个任务的改动一起编译通过）
预期：BUILD SUCCESSFUL。
