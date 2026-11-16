# 架构文档

> 本文档描述 MyBatis SQL Scanner AI 插件的分层架构、核心流程与关键设计。扫描核心为闭源 jar，仅从公开 API 与字节码层面说明契约。

## 1. 总体架构

采用分层架构，自上而下：入口层 → 编排层 → 展示层 / 数据模型层 → 基础设施层，规则引擎与 SQL 解析下沉到扫描核心 jar。

```
┌─────────────────────────────────────────────────────────────┐
│ 入口层 action/        RunAction、ShowRunAction、SqlTypeAction │
├─────────────────────────────────────────────────────────────┤
│ 编排层                ConsoleToolRunner（面板编排）           │
│                       TreeConsoleExecutor（全局执行器）       │
├──────────────────┬──────────────────────────────────────────┤
│ 展示层            │  数据模型层                                │
│ ui/  toolbar/     │  pojo/  pojo/tree/  enums/               │
│ listener/         │  （四级树节点、MapperFileInfo）            │
├──────────────────┴──────────────────────────────────────────┤
│ AI 层  ai/（HTTP 客户端、提示词、渲染、对话面板）              │
│        settings/ai/（供应商预设、多供应商配置持久化、设置页）    │
├─────────────────────────────────────────────────────────────┤
│ 基础设施层  utils/（PSI、XML、图标、语言） settings/（配置持久化）│
├─────────────────────────────────────────────────────────────┤
│ 扫描核心  resources/libs/sql_scanner-5.0.3-RELEASE.jar       │
│  XmlBatisSqlParser（解析） ResultUtil（规则聚合） RuleCodeEnum │
└─────────────────────────────────────────────────────────────┘
```

## 2. 分层职责

| 层 | 组件 | 职责 |
|---|---|---|
| 入口层 | `action/` | 响应 IDE 事件，触发扫描/预览/数据库切换 |
| 编排层 | `ConsoleToolRunner`、`executor/TreeConsoleExecutor` | 组装面板，桥接扫描结果与 UI |
| 展示层 | `ui/`、`toolbar/`、`listener/` | 树、表、预览面板、工具栏渲染与交互 |
| 数据模型层 | `pojo/`、`pojo/tree/`、`enums/` | 扫描结果对象与四级树节点结构 |
| AI 层 | `ai/`、`settings/ai/` | 供应商预设与配置持久化、双协议 HTTP/SSE 客户端、提示词组装、Markdown 渲染、AI 对话面板 |
| 基础设施层 | `utils/`、`settings/` | PSI/XML 定位、图标、国际化、配置持久化 |
| 扫描核心 | `libs/sql_scanner-5.0.3-RELEASE.jar` | XML 解析、动态 SQL 拼装、Druid 方言解析、20 条规则检查 |

## 3. 入口层

| 类 | 触发方式 | 职责 |
|---|---|---|
| `RunAction` | 右键目录/文件 | 获取选中路径与数据库类型，调用核心解析，打开结果面板 |
| `ShowRunAction` | Tools 菜单 | 打开空白扫描面板（不执行扫描） |
| `SqlTypeAction` / `SqlTypeActionGroup` | 面板工具栏 | 切换数据库类型，写入 `TreeConsoleExecutor.dbType` |
| `CopyTreeSelectedAction` / `DeleteTreeSelectedAction` | 树节点右键 | 复制节点文本；删除为 TODO 空实现 |
| `preview/CodePreviewAction`、`preview/RuleResultViewAction` | 树节点点击（程序化触发） | 代码预览 / 规则结果展示 |

入口层通过 `DataKeyConst` + `SimpleDataContext` 传递 `ClickActionEventObject`（Project、Executor、MapperFileInfo）。

## 4. 编排层

### 4.1 ConsoleToolRunner

- `run()`：创建 `RunnerLayoutUi`，组装左右分屏（结果树 + 预览），添加「MyBatis SQL Scanner AI」「Table Operation」两个 Content，注册工具栏，通过 `RunContentManager` 展示
- `createSimpleTree()`：调用 `ResultUtil.doRuleAll()` 聚合结果，构建四级树（等级 → 规则 → mapper → 明细）
- 生命周期：实现 `Disposable`，但未注册到 Disposer 树，实际不会被释放

### 4.2 TreeConsoleExecutor

- 在 `plugin.xml` 注册为 `executor` 扩展，应用级单例
- 当前额外持有可变状态：`Project`、`Tree`、`Splitter`、`dbType`
- 各组件通过 `MyExecutorUtil.getRunExecutorInstance()` 按 ID 获取并读写该状态

## 5. 展示层

| 组件 | 职责 |
|---|---|
| `SqlScanResultPanel`（GUI Designer form） | SQL ID、数据库类型、格式化 SQL、规则建议、跳转按钮 |
| `MyOperationTable` | 表操作统计（Insert/Delete/Select/Update/Merge/Alter/Create/Drop） |
| `MySetttingTable` | 规则设置表：Active 开关、Degree 图标、Code、Description |
| `toolbar/` | Rerun、展开/收起全部、数据库类型下拉 |
| `TreeListener` | 单击 C3 触发代码预览，单击/双击 C4 触发结果预览（双击同时跳转），右键弹菜单 |

注意：`CodePreviewAction.usagePreviewPanel` 与 `RuleResultViewAction.sqlScanResultPanel` 为 **static 单例**，跨项目/多次扫描共享同一 Swing 组件。

## 6. AI 模块（Ask AI）

Ask AI 是预览面板（`SqlScanResultPanel`）上的一个独立能力：把当前 SQL 的四段上下文（原始 XML 片段、格式化 SQL、SQL Type、报错建议）组装为固定提示词模板，调用用户配置的大模型流式分析，回答渲染在替换预览区的对话面板中。单轮对话、无历史消息。

### 6.1 组件

| 组件 | 职责 |
|---|---|
| `settings/ai/AiProviderRegistry` | 8 家供应商预设静态表：官方 API URL、默认/推荐模型、协议类型（OPENAI_COMPAT / ANTHROPIC）、thinking 策略、官方图标（`resources/icons/ai/`） |
| `settings/ai/AiProviderConfig` | 单家配置 POJO（id/apiUrl/apiKey/model），XmlSerializer 序列化单元；`apiKey` 落盘为 `ENC1:` AES-256-GCM 密文，内存为明文 |
| `settings/ai/AiKeyCipher` | API Key 加解密工具（AES-256-GCM，纯 JDK）：旧明文无前缀原样透传，解密失败返回空串；密钥为代码内置常量，发布后不可更换 |
| `settings/ai/AiModelSettingsPersistent` | 多供应商持久化：`activeProvider` + `providers` 列表 + AI 回答语言 + 回答区字号；旧版本单套三字段在 `loadState` 时自动迁移为 DeepSeek 槽 |
| `settings/ai/AiModelSettingsConfig` | Model Settings 设置页：Provider 下拉（带图标）、URL/Key/Model（可编辑，切换供应商自动填充）、Test Connection；编辑期内存草稿暂存，OK/Apply 才落盘 |
| `ai/AiApiClient` | JDK HttpClient + Gson 的双协议客户端：OpenAI 兼容（Bearer）与 Anthropic（x-api-key + anthropic-version）；流式 SSE 解析（含 reasoning/thinking 增量）；thinking 参数按供应商策略（DISABLED / ENABLED_LOW / QWEN_OFF / GPT_LOW / NONE） |
| `ai/AiPromptTemplate` | 中英提示词模板：四段上下文 + 三段式输出模板（SQL 语句/潜在风险/修改建议），开头注明"由 MyBatis XML 解析而来"；语言由 AI 回答语言偏好决定，与 UI 国际化无关 |
| `ai/MarkdownRenderer` | 轻量 Markdown→HTML（针对 JEditorPane HTML 3.2 子集：有序列表保留原文编号的块级行、表格、代码块） |
| `ai/AiChatPanel` | 对话面板：流式回答渲染（节流 + 用户不在底部时跳过重渲染）、思考过程面板（斜体小字、固定高度、正式回答后自动折叠、点标题展开）、Stop/Back、Show Prompt 弹框（Copy 反馈）、Copy Answer、A-/A+ 字号调节 |

### 6.2 数据流

```
SqlScanResultPanel Ask AI 点击
  → 读 xmlTag.getText()（ReadAction 内，原始 XML）
  → new AiChatPanel(取 AiModelSettingsPersistent.getActiveConfig() + 预设的协议/thinking)
  → AiApiClient.startStream(...) 后台线程 HTTP 流式请求
  → 回调(后台线程): onDelta/onThinking/onDone/onError → invokeLater 回 EDT 渲染
```

### 6.3 关键设计

- **缓存复用**：`cachedChatPanel` 按 SQL 节点 + provider/URL/model + 实际回答语言判断 `isReusable()`——已完成直接复用（省 Token）；仍在生成中复用原面板继续看（不重复调用）；停止/失败重新生成
- **思考体验**：思考型模型（GLM/Kimi/Claude 强制或默认思考）流式输出 reasoning/thinking 增量，思考面板实时滚动显示；Qwen 显式关思考（`enable_thinking: false`）、GPT 降推理力度（`reasoning_effort: low`）、DeepSeek 关思考（`thinking.type: disabled`）
- **Test Connection**：用当前 model 发一次极小真实请求；Key 有效但模型无效时报错并列出可用模型
- **迁移兼容**：旧 XML 无 `providers` 节点时自动生成 DeepSeek 槽；`getConfigOrDefault` 读路径不污染持久化

## 7. 数据模型层

- `pojo/MapperFileInfo`：包装核心的 `XmlPluginRuleResult`
- `pojo/tree/`：`RootTreeNode` → `C1DegreeTreeNode`（等级）→ `C2RuleTreeNode`（规则）→ `C3MapperFileTreeNode`（mapper）→ `C4DetailTreeNode`（明细），基于 `StructureTreeModel` + `AsyncTreeModel`
- `pojo/DataKeyConst`、`ClickActionEventObject`：节点点击上下文传递
- `enums/SqlTypeEnum`：五种数据库类型

## 8. 基础设施层

| 组件 | 职责 |
|---|---|
| `MapperXmlHelper` | 按 mapper 名/路径定位 `XmlFile`，按 `sqlNodeId` 定位 `XmlTag` |
| `MyPsiUtil` | PSI 文件/类定位辅助（多为死代码） |
| `MyIconUtil` | 图标统一加载 |
| `LanguageUtil` | 判断 IDE 语言，规则描述中英切换 |
| `MyLogUtil` | 通知封装（group id 为空，未注册） |
| `settings/MySettingsPersistent` | `@State` 应用级配置持久化 |
| `settings/MySettingsConfig` | 设置页 `Configurable` 实现 |
| `settings/ai/AiModelSettingsPersistent` | Ask AI 多供应商配置持久化（见 6.1） |

### 8.1 设置持久化链路（当前存在缺陷）

`MySettingsConfig.reset()` 读取 `MySettingsPersistent.ruleSelectedList`；`apply()` 回写。

`MySettingRow` 仅暴露 `getRuleCodeEnum()`（无 setter），`id` 为 final。`XmlSerializer` 只序列化可写属性，实际落盘内容仅含 `active` 与 `ruleIcon`，**丢失 `ruleCodeEnum`**。重启后行对象 `ruleCodeEnum == null`，`reset()` 判定非法并重建默认配置，规则开关与等级全部丢失。

## 9. 扫描核心 jar 契约

包结构：`com.zhang.zmain.*`（入口/规则）、`com.zhang.sqlscaner.*`（XML 解析、SQL 解析、节点模型）。

| 契约 | 说明 |
|---|---|
| `XmlBatisSqlParser.parse(String path, LogBean logBean, String dbType)` | 返回 `List<BaseResult>`；path 为空返回 null；逐文件捕获异常仅记日志 |
| `com.zhang.zmain.enums.RuleCodeEnum` | 20 条规则，静态可变字段 `active`、`degreeEnum`，中英描述 |
| `com.zhang.zmain.enums.DegreeEnum` | BLOCKED / CRITICAL / MAJOR |
| `ResultUtil.doRuleAll(List<BaseResult>)` | 聚合为 `Map<RuleCodeEnum, List<XmlPluginRuleResult>>`，规则级 `isActive()` 过滤 |
| `ResultUtil.getTableOptCount(List<BaseResult>)` | 表级 8 类操作计数 |
| `XmlPluginRuleResult` | mapperName、sqlNodeId、sqlNodeIdOrg、sqlText、druidFormatSql、parseResult、dbType、filePath |
| `FileUtil.getFileList` | 递归遍历；目录名包含 `.idea`/`target` 跳过；文件名包含 `.xml` 收集 |

SQL 解析经 `DruidVisitorUtil` 按 dbType（equalsIgnoreCase）选择 Druid 方言 parser，依次尝试 mysql/oracle/gauss/postgresql/db2。XML 读取使用 dom4j `SAXReader`，已设置空 `EntityResolver` 阻断外部实体。

## 10. 线程模型

| 环节 | 当前实现 | 问题 |
|---|---|---|
| 扫描 | EDT 同步调用 `XmlBatisSqlParser.parse()` | 无进度、不可取消，大项目卡死 UI |
| 面板构建 | EDT 执行 `doRuleAll()`、`getTableOptCount()`、建树 | 数据量大时同样卡顿 |
| 预览定位 | 后台 `runReadAction` 后 EDT 阻塞 `CompletableFuture.get()` | 阻塞 UI，无 Dumb 模式处理，无缓存 |
| Ask AI 请求 | `AiApiClient` 后台线程（`executeOnPooledThread` / 独立线程）流式请求，回调 `invokeLater` 回 EDT | 正常；Stop 通过关闭输入流取消；面板 Back 后回调仍指向旧组件（对象级安全，无 EDT 阻塞） |
| Test Connection | `executeOnPooledThread` 后台执行，结果 `invokeLater + ModalityState.any()` 回 EDT（设置对话框模态下必需） | 正常 |

## 11. 构建与打包

- 依赖：`druid:1.2.8`、`dom4j:2.1.4`、`fileTree(libs/*.jar)`
- `sourceSets` 排除 `img/`、`libs/`、`libsource/`、`icons/mybatis/`、`scanner.vsdx`；`icons/ai/`（供应商官方 logo PNG）正常打包
- `buildPlugin.buildSearchableOptions.enabled = false`
- 最终 zip 除核心依赖外含旧传递依赖（jaxb-api 2.2.12、stax-api、xpp3、xsdlib、pull-parser、relaxngDatatype、jaxen），存在与 IDE 类冲突风险
- `gradle-wrapper.properties` 指向本机绝对路径，不可移植
- `plugin.xml` `since-build="243.21565.193"` 与编译目标 2026.1 不一致，未配置 `verifyPlugin`

## 12. 已知架构缺陷与演进方向

| 级别 | 缺陷 | 演进方向 |
|---|---|---|
| P0 | 规则设置持久化失效 | 引入显式 DTO（ruleCode/active/degree），`reset()`/`apply()` 显式映射，恢复 `isModified()` |
| P0 | 扫描同步跑在 EDT | `Task.Backgroundable` + `ProgressIndicator`，后台解析、UI 线程建树 |
| P0 | Executor 全局持有 Project/Tree/Splitter/dbType | 状态下沉至 project service / Content，Executor 仅保留身份信息 |
| P1 | `<cache/>` 等无 id 标签预览 NPE | `findXmlElement` 判空 |
| P1 | 预览面板 static 单例跨项目共享（含 AI 对话面板 `cachedChatPanel` 同生命周期） | 每项目/每 Content 独立实例 |
| P1 | `RunAction` 保存 `AnActionEvent` | 改存 Project + 路径 |
| P1 | 树模型 parent 未注册、不释放 | 绑定 project/Content 并注册 Disposer |
| P2 | 统计表 setter/clone/排序缺陷、Delete 空实现、空 notification group、重复 Content ID | 逐一修复（详见审查记录） |
