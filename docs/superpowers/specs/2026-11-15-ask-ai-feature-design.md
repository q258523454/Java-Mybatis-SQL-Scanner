# Ask AI 功能设计文档

日期：2026-11-15
项目：Java Mybatis SQL Scanner（IntelliJ IDEA 插件，插件 ID `com.plugin.sql`）

## 一、意图与原因

插件扫描 MyBatis mapper XML 后，在 SQL 详情预览面板展示 SQL ID、SQL Type、格式化 SQL、报错/建议。用户希望把这三部分内容交给大模型（以 DeepSeek 为例）做一次智能分析，帮助判断 SQL 是否有问题、如何优化。

## 二、目标

1. 在预览面板的 **Open File 按钮右侧**新增 **Ask AI** 按钮。
2. 点击 Ask AI 后，将 格式化 SQL + SQL Type + 报错/建议（另附 SQL ID 便于定位）按固定提示词模板组装，调用配置好的模型。
3. 右侧窗口整体替换为 **AI 对话窗口**，流式输出回答；**单轮对话**（不支持追问、无历史消息）。
4. 设置页重构为父节点 + 两个子页：
   - `Other Settings > Java Mybatis SQL Scanner Rules > Rules Settings`（原规则页，仅改归属与显示名）
   - `Other Settings > Java Mybatis SQL Scanner Rules > Model Settings`（新增：API URL / API Key / model / **Test Connection 测试按钮**）
5. 模型配置以 DeepSeek 为默认：API URL 默认 `https://api.deepseek.com/v1/chat/completions`，model 默认 `deepseek-v4-pro`（官方当前模型名，可编辑下拉，兼容自定义模型名）。
6. Test Connection：点击后在 Model Settings 页内验证 模型配置是否可用（连通性 + API Key 有效性），后台执行不阻塞 UI，结果显示在按钮旁。

## 三、非目标 / 反目标

- **不做**多轮对话、历史消息、会话保存。
- **不做**思考过程（reasoning）展示——默认关闭 DeepSeek thinking 模式，只显示最终回答。
- **不做**温度等高级参数暴露。
- **不修改**任何原有扫描/预览/规则设置的逻辑（P0-4B 静态单例等已知缺陷不在本次范围内）。
- **反目标**：不允许扫描流程或预览流程因新增功能而行为变化；不允许在 EDT 上发起网络请求或阻塞 UI。

## 四、约束

- JDK 21；编译目标 IDE 2025.1（since-build=251）；Gradle 9.4.1 + IntelliJ Platform Gradle Plugin 2.13.1。
- 文档、注释、提交说明一律中文。
- 不引入国内镜像，不改用户网络配置；构建命令沿用 `GRADLE_USER_HOME=/Users/mac/Documents/gradle ./gradlew buildPlugin`。
- 新增依赖必须可打包进插件或由 IDE 平台自带；优先使用平台自带库，避免版本冲突。
- API Key 以明文持久化（用户明确要求），输入框隐藏显示（密码框），眼睛按钮切换明文。
- 单元测试不写（用户人工验证），但需给出人工验证清单。
- 插件壳代码开源；扫描核心 jar 闭源，不可改。

## 五、用户已拍板的决策

1. API Key：**手工明文输入**，设置页用密码框隐藏，眼睛按钮切换明文/密文；明文持久化到配置文件。
2. 其余按 2026-11-15 对话中"建议的默认方案"执行（用户未否决）：
   - 单轮、无历史；生成期间禁用 Ask AI；
   - 对话窗口含 停止生成、返回预览 按钮；
   - 提示词模板固定，语言跟随 `LanguageUtil.isChinese()`；
   - thinking 模式关闭；
   - 上下文含 SQL ID、SQL Type、格式化 SQL、报错/建议 四段。

## 六、技术决策（建造者可自行实现，以下为设计者的决定）

### 6.1 HTTP 与流式解析

- 使用 **JDK 自带 `java.net.http.HttpClient`**（无新增网络依赖）。
- 请求：`POST {apiUrl}`，头 `Authorization: Bearer {apiKey}`、`Content-Type: application/json`。
- 请求体：
  ```json
  {
    "model": "{model}",
    "messages": [
      {"role": "system", "content": "{系统提示}"},
      {"role": "user", "content": "{用户上下文}"}
    ],
    "stream": true,
    "thinking": {"type": "disabled"}
  }
  ```
  （`thinking` 字段为 DeepSeek 扩展，其他 OpenAI 兼容服务会忽略未知字段，无副作用。）
- SSE 解析：`BodyHandlers.ofInputStream()` + `BufferedReader` 按行读；`data: {...}` 行解析 JSON，取 `choices[0].delta.content` 追加；`data: [DONE]` 或 `finish_reason != null` 结束。
- 兜底：若响应 Content-Type 不是 `text/event-stream`（或首个非空字符是 `{`），按**非流式**解析一次完整 JSON，取 `choices[0].message.content`。保证兼容不支持流的代理。
- JSON 解析使用 **IDE 平台自带 Gson**（`com.google.gson`，平台 classpath 已包含，无需 Gradle 依赖、无打包冲突）。若编译期找不到，再在 build.gradle 添加 `compileOnly` 依赖（打包仍用平台运行时）。
- 超时：连接超时 10s；流式读取期间不设整体超时（防止长回答被切断），用户可用「停止」按钮中断（关闭 InputStream）。
- 错误处理：4xx/5xx/网络异常 → 解析错误响应体 `error.message`（若有），在对话窗口内以友好文案显示，并提示去 Model Settings 检查配置；同时弹一条 IDEA 通知（沿用现有 `MyLogUtil`）。**不写日志文件**（用户选定方案 A）。

### 6.2 线程模型

- 点击 Ask AI 在 EDT 上发生（按钮监听器）：只做 校验配置 + 创建对话面板 + 替换 splitter 第二组件。
- 网络请求在**后台线程**执行（`ApplicationManager.getApplication().executeOnPooledThread` 或新线程）。
- 流式增量回 **EDT** 用 `invokeLater` 刷新 UI；为避免刷新过于频繁，累积增量，约每 100ms 或每 512 字符批量刷新一次（建造者可自定合并策略）。
- 停止按钮：关闭 InputStream 使后台读循环退出；后台线程结束前再 invokeLater 更新状态为"已停止"。
- 生成期间禁用 Ask AI 按钮与停止以外的重入（对话面板打开后用户无法再次点击旧面板按钮，因为右侧已替换为对话窗口；但需防重入：若对话面板已存在，直接返回）。

### 6.3 对话窗口（AiChatPanel）布局

自上而下：
1. **标题栏**：标题 "AI SQL Analysis" + 当前模型名小标签（浅灰）。
2. **上下文折叠区**：默认折叠（JBCollapsiblePanel 或等价实现），展示四段：SQL ID、SQL Type、格式化 SQL、报错/建议；SQL 用等宽字体。
3. **回答区**：`JEditorPane`（HTML）+ `JBScrollPane`，占满剩余空间；流式追加。
4. **底部状态栏**：状态文本（生成中…/已完成/已停止/失败原因）+ 「Stop」按钮 + 「Back」按钮。

外观要求：使用 `JBColor`/`JBUI`/`UIUtil` 适配暗色主题；等宽字体显示 SQL 与代码；整体简洁、留白适度、不挤。

### 6.4 Markdown 渲染

回答区用轻量 Markdown → HTML 转换（自写 `MarkdownRenderer`，不引第三方）：
- ``` 代码块 → `<pre>`（等宽、浅底）
- 行内 `code` → `<code>`
- `#`~`###` 标题 → `<h3>` 等
- `**粗体**` → `<b>`
- `- ` 列表 → `<ul>/<li>`（连续行合并）
- 其余纯文本按 HTML 转义
流式期间边累积边渲染（按批渲染），代码块未闭合时按未完成状态渲染，最终刷新一次收尾。

### 6.5 设置结构

- 新增父节点 configurable `ParentSettingsConfig`：displayName "Java Mybatis SQL Scanner Rules"，`parentId="configurable.group.other"`，实现 `Configurable.Composite`，返回两个子页。
- 原 `MySettingsConfig`：displayName 改为 **"Rules Settings"**，`parentId` 改为父节点 id；**其余逻辑不动**。
- 新增 `AiModelSettingsConfig`（applicationConfigurable，parentId=父节点 id，displayName "Model Settings"）：
  - API URL：`JTextField`，默认 `https://api.deepseek.com/v1/chat/completions`
  - API Key：`JPasswordField` + 眼睛按钮（`AllIcons.Actions.Preview` 或等价图标）切换 `echoChar` 明文/密文
  - Model：可编辑 `JComboBox`，候选 `deepseek-v4-pro`、`deepseek-flash`，允许手输任意模型名
  - **Test Connection 按钮**：用当前表单中（未 Apply 也可）的 URL/Key 发起验证；后台线程执行，按钮期间禁用并显示 "Testing..."，完成后在按钮旁显示结果图标与文字（成功：绿色 ✓ + 可用模型列表摘要；失败：红色 ✗ + 错误原因）；验证方式优先 `GET {apiBaseUrl}/models`（OpenAI 兼容标准端点，从 API URL 推导 base：取 URL 的 scheme+host+`/v1` 部分），若该端点 404/405 则降级为一次非流式极小 chat 请求（`max_tokens=1`）验证连通性与 Key 有效性
  - 实现 `isModified()/apply()/reset()`（`apply()` 写入持久化组件）
- 新增持久化组件 `AiModelSettingsPersistent`（`@State(name="AiModelSettingsPersistent", storages=@Storage("AiModelSettings.xml"))`，application service），字段：`apiUrl`、`apiKey`、`model`（均 public，参考 `MySettingsPersistent` 模式）。API Key 按用户要求明文存储于此文件。

### 6.6 Ask AI 按钮与数据流

- `SqlScanResultPanel.form` 的 Operation 行（第 4 行）Open File 按钮右侧新增 `JButton`（binding `askAiButton`，text "Ask AI"）。
- `SqlScanResultPanel.java` 新增监听器（原有 Open File 监听器不动）：
  1. 读面板自身字段：`sqlNodeIdOrg.getText()`、`sqlType.getText()`、`sqlText.getText()`、`sqlScanResult.getText()`；
  2. 从 `MyExecutorUtil.getRunExecutorInstance(TreeConsoleExecutor.PLUGIN_ID)` 取 `TreeConsoleExecutor`，得 `splitterPanel` 与 `project`（与 `RunAction` 既有用法一致，不改原逻辑）；
  3. 读 `AiModelSettingsPersistent` 校验 apiKey/apiUrl 非空，否则弹框提示并给「配置」按钮直达 Model Settings 页（`ShowSettingsUtil.showSettingsDialog(project, AiModelSettingsConfig.class)`）；
  4. 创建 `AiChatPanel`（传入四段上下文、模型配置、project），`splitterPanel.setSecondComponent(chatPanel.getComponent())` 替换右侧；
  5. 由 `AiChatPanel` 内部触发发送（或在替换后立即触发）。
- **返回预览**：`AiChatPanel` 持有预览面板引用，Back 按钮 `splitterPanel.setSecondComponent(预览面板)`；另外用户点击结果树节点时，`RuleResultViewAction` 原有逻辑会把预览面板重新设为第二组件（自然恢复，不改原代码）。
- 静态单例：`SqlScanResultPanel` 目前是 static 单例（P0-4B 已知缺陷，不在本次范围）。新对话面板不做 static 单例，每次点击新建（生命周期短，无状态串台风险）。

### 6.7 提示词模板（固定）

- system（角色定位，语言跟随 `LanguageUtil.isChinese()`）：
  - 中文：`你是一名资深数据库专家与 SQL 优化顾问，擅长 MySQL/Oracle/DB2/PostgreSQL 等主流数据库的性能分析与 SQL 调优。请根据用户提供的 SQL 上下文，指出存在的问题、风险与可优化之处，并给出具体可执行的建议。回答请使用中文。`
  - 英文：对应英文版（建造者自行撰写，语义一致）。
- user 模板（固定结构）：
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
  {parseResult}

  请从正确性、性能、索引使用、潜在风险等角度分析这条 SQL 是否存在问题或需要优化的地方。
  ```
  当报错/建议为空时显示"（无）"。

## 七、文件清单

### 新增

| 文件 | 说明 |
|---|---|
| `src/main/java/com/zhang/scanner/ai/AiChatPanel.java` | AI 对话窗口面板（布局、流式渲染、停止/返回） |
| `src/main/java/com/zhang/scanner/ai/AiApiClient.java` | HTTP 请求 + SSE 解析 + 非流式兜底 |
| `src/main/java/com/zhang/scanner/ai/AiPromptTemplate.java` | 提示词模板组装 |
| `src/main/java/com/zhang/scanner/ai/MarkdownRenderer.java` | 轻量 Markdown → HTML |
| `src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsPersistent.java` | 模型配置持久化（明文，含 apiKey） |
| `src/main/java/com/zhang/scanner/settings/ai/AiModelSettingsConfig.java` | Model Settings 设置页 |
| `src/main/java/com/zhang/scanner/settings/ai/ParentSettingsConfig.java` | 设置父节点（Composite） |

### 修改（仅新增/归属调整，不改原逻辑）

| 文件 | 改动 |
|---|---|
| `src/main/java/com/zhang/scanner/ui/SqlScanResultPanel.form` | Operation 行新增 Ask AI 按钮 |
| `src/main/java/com/zhang/scanner/ui/SqlScanResultPanel.java` | 新增按钮字段与监听器（原 Open File 监听器不动） |
| `src/main/resources/META-INF/plugin.xml` | 注册父节点与 Model Settings；原 MySettingsConfig 改 parentId |
| `src/main/java/com/zhang/scanner/settings/MySettingsConfig.java` | 仅 displayName 改为 "Rules Settings" |
| `build.gradle` | 原则上不改；若 Gson 编译不可见则加 `compileOnly` 依赖 |

## 八、验证（人工）

1. 设置页：`Other Settings > Java Mybatis SQL Scanner Rules` 下出现 `Rules Settings` 与 `Model Settings` 两个子页；Rules 页原功能不变、持久化正常。
2. Model Settings：填 API URL / API Key / model；API Key 显示为密文，点眼睛可看明文；重启 IDEA 后配置保留（明文在 `AiModelSettings.xml` 中）。
3. **Test Connection**：配置正确 → 绿色 ✓ 显示可用；API Key 错误 → 红色 ✗ 显示错误原因；测试期间 UI 不卡顿。
4. 未配置 API Key 时点 Ask AI → 弹提示并可跳转设置页。
5. 配置正确后点 Ask AI → 右侧变为对话窗口，回答流式逐字出现，最终状态"已完成"。
6. 生成中点 Stop → 输出停止、状态"已停止"，无异常日志。
7. 点 Back / 点击其他结果树节点 → 预览面板恢复，原 Open File 跳转等功能正常。
8. 断网或错误 API Key → 对话窗口显示友好错误文案 + IDEA 通知，不弹原始异常。
9. `./gradlew buildPlugin` 通过；插件安装后暗色主题下界面美观可读。
