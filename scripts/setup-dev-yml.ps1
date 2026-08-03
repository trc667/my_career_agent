# 首次克隆：从 application-dev.yml.example 生成 application-dev.yml（后者在 .gitignore，不会入库）
# 用法: powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev-yml.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$example = Join-Path $repoRoot "src/main/resources/application-dev.yml.example"
$target = Join-Path $repoRoot "src/main/resources/application-dev.yml"

if (-not (Test-Path $example)) {
    Write-Error "找不到示例文件: $example"
    exit 1
}
if (Test-Path $target) {
    Write-Host "已存在: $target"
    Write-Host "如需从示例重新生成，请先删除该文件再运行本脚本。"
    exit 0
}
Copy-Item -Path $example -Destination $target
Write-Host "已生成: $target"
Write-Host "请编辑该文件，将 REPLACE_WITH_* 替换为真实 Key，或改用环境变量 DASHSCOPE_API_KEY / AMAP_MCP_KEY。"
