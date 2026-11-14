# AGENTS.md

## 项目简介

Java Mybatis SQL Scanner 是一个 IntelliJ IDEA 插件（插件 ID：`com.plugin.sql`），用于扫描 MyBatis/iBatis mapper XML，基于 Druid 解析 SQL，按 20 条规则输出 Blocked / Critical / Major 三级问题，提供结果树、表操作统计、代码预览与跳转、规则开关等能力。

- 支持数据库：MySQL、Oracle、Gauss、DB2、PostgreSQL
- 插件壳代码开源；扫描核心为闭源二进制 jar：`src/main/resources/libs/sql_scanner-5.0.2-RELEASE.jar`
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
├── ConsoleToolRunner  # 面板编排：结果树 + 统计表 + 工具栏
├── executor/          # 全局 Executor（plugin.xml 注册）
├── listener/          # 树节点点击/右键事件分发
├── pojo/              # 数据模型（含 tree/ 四级树节点）
├── settings/          # 规则开关/等级设置与持久化
├── ui/                # 预览面板、规则设置表、操作统计表
├── utils/             # PSI、XML、图标、语言等工具
└── enums/             # 数据库类型枚举
src/main/resources/
├── META-INF/plugin.xml # 插件描述、扩展点、动作注册
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

## 开发约定

- 文档、注释、提交说明一律使用中文
- 打包排除 `img/`、`libs/`、`libsource/`、`icons/mybatis/` 等资源，见 `build.gradle` 的 `sourceSets`
- 扫描核心 jar 为闭源依赖，禁止反编译后改包；升级时需同步 `build.gradle`、`plugin.xml` 的版本号
- 修改规则说明需同步 `plugin.xml` 的 `change-notes` 与 README

## 已知关键问题（修复优先级）

- P0：规则设置无法持久化（重启后恢复默认）；扫描同步跑在 EDT；`TreeConsoleExecutor` 全局保存 Project/Tree 状态导致多项目串台
- P1：mapper 含 `<cache/>` 时预览 NPE；预览面板为 static 单例；`RunAction` 保存 `AnActionEvent`

详见 `architecture.md` 的「已知架构缺陷」。

## 验证要求

- 提交前必须运行 `./gradlew buildPlugin` 并确认通过
- 涉及设置持久化、树结构、统计表的改动需补充单元测试（当前测试为空）
