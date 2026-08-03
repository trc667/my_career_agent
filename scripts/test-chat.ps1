# 调用职规大师 API 并把回复写入 UTF-8 文件，避免控制台乱码
# 用法: .\scripts\test-chat.ps1
# 或: .\scripts\test-chat.ps1 "你的问题"

param(
    [string]$Message = "怎样和喜欢的人找话题？"
)

$uri = "http://localhost:8080/api/chat"
$body = @{ message = $Message } | ConvertTo-Json -Compress

try {
    $response = Invoke-RestMethod -Uri $uri -Method POST -ContentType "application/json; charset=utf-8" -Body $body
    $outPath = Join-Path $PSScriptRoot ".." "reply.txt"
    $outPath = [System.IO.Path]::GetFullPath($outPath)
    [System.IO.File]::WriteAllText($outPath, $response.reply, [System.Text.Encoding]::UTF8)
    Write-Host "AI 回复已写入: $outPath"
    Write-Host "正在用默认程序打开..."
    Start-Process $outPath
} catch {
    Write-Host "请求失败: $_" -ForegroundColor Red
}
