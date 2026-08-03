# 本地开发：控制台改为 UTF-8 后再启动，减轻 Windows 下 MCP stdio 中文乱码
# 用法: .\scripts\run-spring-boot-utf8.ps1
# 可选参数会原样传给 Maven，例如: .\scripts\run-spring-boot-utf8.ps1 "-DskipTests"

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
& chcp.com 65001 | Out-Null

$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repoRoot

$extra = $args
& mvn @("spring-boot:run", "-Dspring-boot.run.profiles=dev") @extra
