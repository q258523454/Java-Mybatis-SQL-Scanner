# AGENTS.md

## 项目简介

MyBatis SQL Scanner AI 是一个 IntelliJ IDEA 插件（插件 ID：`com.plugin.sql`），用于扫描 MyBatis/iBatis mapper XML，基于 Druid 解析 SQL，按 20 条规则输出 Blocked / Critical / Major 三级问题，提供结果树、表操作统计、代码预览与跳转、规则开关等能力。

此外内置 **Ask AI** 智能分析：把扫描出的 SQL（原始 XML / 格式化 SQL / SQL Type / 报错建议）发给大模型流式分析，支持 7 家供应商（DeepSeek、Qwen、Doubao、GLM、Kimi、GPT、Claude）+ 自定义，每家独立保存 URL/Key/Model，思考过程实时展示，详见 `docs/ask-ai-prompt.md`。

- 支持数据库：MySQL、Oracle、Gauss、DB2、PostgreSQL
- 插件壳代码开源；扫描核心为闭源二进制 jar：`src/main/resources/libs/sql_scanner-5.0.3-RELEASE.jar`
- 技术栈：Java 21，Gradle 9.4.1，IntelliJ Platform Gradle Plugin 2.13.1，编译目标 IDE 2026.1

## 常用命令

```bash
./gradlew buildPlugin     # 构建插件 jar
./gradlew runIde          # 沙箱运行调试
./gradlew test            # 运行测试（当前无测试用例）
./gradlew verifyPlugin    # 兼容性校验（当前未配置）
```

构建产物：`build/libs/`（jar）、`build/distributions/`（zip）。

## 目录结构

```
src/main/java/com/zhang/scanner/
├── action/            # 入口动作：扫描、数据库类型、预览、树节点右键
├── ai/                # Ask AI：HTTP 客户端、提示词模板、Markdown 渲染、对话面板
├── ConsoleToolRunner  # 面板编排：结果树 + 统计表 + 工具栏
├── executor/          # 全局 Executor（plugin.xml 注册）
├── listener/          # 树节点点击/右键事件分发
├── pojo/              # 数据模型（含 tree/ 四级树节点）
├── settings/          # 规则开关/等级设置与持久化
│   └── ai/            # 供应商预设注册表、多供应商配置持久化、Model Settings 设置页
├── ui/                # 预览面板、规则设置表、操作统计表
├── utils/             # PSI、XML、图标、语言等工具
└── enums/             # 数据库类型枚举
src/main/resources/
├── META-INF/plugin.xml # 插件描述、扩展点、动作注册
├── icons/ai/           # 7 家供应商官方 logo（32x32 PNG，Custom 用 IDEA 内置齿轮）
└── libs/               # 闭源扫描核心 jar
```

## 核心入口

| 入口 | 说明 |
|---|---|
| `RunAction` | 右键目录/文件执行扫描（ProjectViewPopupMenu） |
| `ShowRunAction` | Tools 菜单打开扫描面板 |
| `ConsoleToolRunner` | 创建 Run 面板：树、表统计、工具栏 |
| `TreeConsoleExecutor` | 应用级 Executor，当前持有 Project/Tree/Splitter/dbType |
| `MySettingsConfig` | 设置页：规则开关与等级 |
| `AiModelSettingsConfig` | Model Settings 设置页：Provider 下拉（官方 URL/模型自动填充）、URL/Key/Model、Test Connection |
| `AiChatPanel` | AI 对话窗口：流式回答、思考过程面板（自动折叠）、Stop/Back、字号调节 |
| `AiProviderRegistry` | 8 家供应商预设（API URL、默认模型、协议类型、thinking 策略、图标） |
| `AiModelSettingsPersistent` | 多供应商配置持久化（activeProvider + providers 列表，旧配置自动迁移） |

## 开发约定

- 文档、注释、提交说明一律使用中文
- 打包排除 `img/`、`libs/`、`libsource/`、`icons/mybatis/` 等资源，见 `build.gradle` 的 `sourceSets`；`icons/ai/` 正常打包
- 扫描核心 jar 为闭源依赖，禁止反编译后改包；升级时需同步 `build.gradle`、`plugin.xml` 的版本号
- 修改规则说明需同步 `plugin.xml` 的 `change-notes` 与 README
- AI 相关约定：
  - API Key 按用户要求**明文持久化**于 `AiModelSettings.xml`，设置页密码框 + 眼睛切换明文
  - AI 回答语言（AI Language 按钮）与 UI 国际化相互独立；UI 文案用 `LanguageUtil.zhOrEn(en, zh)` 双语
  - 供应商预设数据（官方 URL/默认模型/thinking 策略）变更需对照各家官方文档核实
  - Ask AI 不修改扫描/预览/规则逻辑；网络请求一律后台线程，禁止阻塞 EDT

## 已知关键问题（修复优先级）

- P0：规则设置无法持久化（重启后恢复默认）；扫描同步跑在 EDT；`TreeConsoleExecutor` 全局保存 Project/Tree 状态导致多项目串台
- P1：mapper 含 `<cache/>` 时预览 NPE；预览面板为 static 单例；`RunAction` 保存 `AnActionEvent`

详见 `architecture.md` 的「已知架构缺陷」。

## 验证要求

- 提交前必须运行 `./gradlew buildPlugin` 并确认通过
- 涉及设置持久化、树结构、统计表的改动需补充单元测试（当前测试为空）
- AI 功能改动以构建通过 + 用户人工验证清单验收（参考 `docs/ask-ai-prompt.md` 的功能描述逐项核对）
