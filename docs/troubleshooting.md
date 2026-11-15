# 故障排查（Troubleshooting）

## 下载依赖报 `Connection refused` / `Can't connect to SOCKS proxy`

浏览器能打开但 Gradle 不行，通常是 Gradle daemon 残留了旧代理端口。Gradle daemon 是长生命周期 JVM，代理关闭或换端口后不会自动更新 `socksProxyHost`。先停 daemon：

```bash
GRADLE_USER_HOME="$HOME/Documents/gradle" ./gradlew --stop
```

再执行 `File → Reload All Gradle Projects`。

> 本机 IDEA 的 Gradle User Home 是 `~/Documents/gradle`，不是 `~/.gradle`；直接 `./gradlew --stop` 停不掉。
> 若使用默认 Gradle User Home，去掉 `GRADLE_USER_HOME=...`。
> 仍失败再检查 IDEA `HTTP Proxy`、`JAVA_TOOL_OPTIONS`、`gradle.properties`。
