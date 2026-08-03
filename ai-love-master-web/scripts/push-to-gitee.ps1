# Push frontend to Gitee (adds frontend/ folder)
# Usage: .\push-to-gitee.ps1 -GiteeUrl "https://gitee.com/username/repo.git"

param(
    [Parameter(Mandatory=$true)]
    [string]$GiteeUrl
)

$ErrorActionPreference = "Stop"
$frontendSrc = "$PSScriptRoot\.."
$tempDir = "$env:TEMP\ai-love-master-push-" + (Get-Random)

Write-Host "1. Cloning Gitee repo..." -ForegroundColor Cyan
git clone $GiteeUrl $tempDir 2>$null
if (-not $?) { Write-Host "Clone failed. Check URL and network." -ForegroundColor Red; exit 1 }

Push-Location $tempDir

Write-Host "2. Copying frontend to frontend/ ..." -ForegroundColor Cyan
New-Item -ItemType Directory -Path "frontend" -Force | Out-Null
$exclude = @('node_modules', 'dist', '.git', '.vscode')
Get-ChildItem $frontendSrc -Force | Where-Object { $exclude -notcontains $_.Name } | ForEach-Object {
    Copy-Item $_.FullName -Destination "frontend\$($_.Name)" -Recurse -Force
}

Write-Host "3. Commit and push..." -ForegroundColor Cyan
git add .
git status
git commit -m "feat: add Vue frontend (AI love master, super agent, cyberpunk homepage)"
$branch = (git branch --show-current)
git push origin $branch

Pop-Location
Remove-Item -Recurse -Force $tempDir -ErrorAction SilentlyContinue

Write-Host "`nDone! Frontend pushed to Gitee." -ForegroundColor Green
