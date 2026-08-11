<#
.SYNOPSIS
一键切换前端后端 API 地址（cpolar 隧道 <-> 本地），同步更新 3 处配置。

.USAGE
切换到公网隧道（给外人演示 / 构建部署前）：
  .\scripts\switch-backend.ps1 https://xxxx.r11.cpolar.top

切回本地后端（日常开发调试）：
  .\scripts\switch-backend.ps1 -Local

.DESCRIPTION
同步修改（均按 UTF-8 无 BOM 写入，防止 Vite 误读）：
  1. ai-love-master-web/.env.development  -> VITE_API_BASE_URL（普通请求，dev 用）
  2. ai-love-master-web/.env.production   -> VITE_STREAM_BASE_URL（流式直连隧道，构建用）
  3. ai-love-master-web/public/_redirects -> /api/* 反向代理目标（Netlify 部署用）

切回本地（-Local）只还原开发配置，生产配置保持不动，避免误伤已部署地址。

.NOTES
- 隧道模式下前端页面本身也要公网可达：dev server 演示请再 cpolar 映射 5175；
  构建部署请先跑本脚本再 npm run build。
- dev server 运行时改 .env 会自动重启生效；构建产物需重新 build。
#>
param(
    [Parameter(Position = 0, HelpMessage = "后端地址，如 https://xxxx.r11.cpolar.top")]
    [string]$TunnelUrl,

    [switch]$Local
)

$ErrorActionPreference = 'Stop'
$webDir = Join-Path $PSScriptRoot '..\ai-love-master-web'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

if ($Local) {
    $TunnelUrl = 'http://localhost:8080'
    $mode = '本地开发模式'
} elseif ([string]::IsNullOrWhiteSpace($TunnelUrl)) {
    Write-Host '用法：' -ForegroundColor Cyan
    Write-Host '  切换到隧道： .\scripts\switch-backend.ps1 https://xxxx.r11.cpolar.top'
    Write-Host '  切回本地：   .\scripts\switch-backend.ps1 -Local'
    exit 1
} else {
    $mode = "隧道模式：$TunnelUrl"
}

Write-Host "==> 切换为 $mode" -ForegroundColor Cyan

function Update-File {
    param(
        [string]$Path,
        [string]$Pattern,
        [string]$Replacement
    )
    $name = [System.IO.Path]::GetFileName($Path)
    if (-not (Test-Path $Path)) {
        Write-Host "[SKIP] $name 不存在" -ForegroundColor Yellow
        return
    }
    $content = [System.IO.File]::ReadAllText($Path)
    if ($content -match $Pattern) {
        $new = $content -replace $Pattern, $Replacement
        if ($new -ne $content) {
            [System.IO.File]::WriteAllText($Path, $new, $utf8NoBom)
            $hit = [regex]::Match($content, $Pattern).Value
            Write-Host "[OK] $name" -ForegroundColor Green
            Write-Host "     旧: $hit"
            Write-Host "     新: $($Replacement -replace '^\s*','')"
        } else {
            Write-Host "[SAME] $name 已是最新，跳过" -ForegroundColor DarkGray
        }
    } else {
        Write-Host "[WARN] $name 未匹配到 '$Pattern'，请手动检查" -ForegroundColor Yellow
    }
}

# 1. 开发环境普通请求地址
Update-File -Path (Join-Path $webDir '.env.development') `
    -Pattern '(?m)^VITE_API_BASE_URL=.*$' `
    -Replacement "VITE_API_BASE_URL=$TunnelUrl"

if ($Local) {
    # 切回本地：只动开发配置，生产配置保持上次隧道地址不变
    Write-Host ''
    Write-Host '已还原本地开发配置。生产配置（.env.production / _redirects）未改动；' -ForegroundColor DarkGray
    Write-Host '下次构建部署前请重新执行本脚本切换到最新隧道地址。' -ForegroundColor DarkGray
} else {
    # 2. 生产环境流式直连地址（构建时注入）
    Update-File -Path (Join-Path $webDir '.env.production') `
        -Pattern '(?m)^VITE_STREAM_BASE_URL=.*$' `
        -Replacement "VITE_STREAM_BASE_URL=$TunnelUrl"

    # 3. Netlify 反向代理目标
    Update-File -Path (Join-Path $webDir 'public\_redirects') `
        -Pattern '(?m)^/api/\*\s+https?://\S+/api/:splat' `
        -Replacement "/api/*    $TunnelUrl/api/:splat"

    Write-Host ''
    Write-Host '3 处配置已同步。接下来：' -ForegroundColor Cyan
    Write-Host '  1) dev server 演示（5175）：Vite 检测到 .env 变化会自动重启，刷新即可；' -ForegroundColor White
    Write-Host '     同时记得在 cpolar 里再映射前端 5175 端口，把前端隧道地址发给对方。'
    Write-Host '  2) 构建部署：重新执行 npm run build 后上传 dist/。'
}
