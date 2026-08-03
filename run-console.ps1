# 先设终端为 UTF-8，再启动控制台交互模式（中文回复可正常显示）
# 用法: .\run-console.ps1
chcp 65001 | Out-Null
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev,console"
