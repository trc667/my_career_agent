<#
.SYNOPSIS
Stop all services started by start-all.ps1: backend (8080) + frontend (5175) + cpolar tunnels.

.USAGE
.\scripts\stop-all.ps1

.DESCRIPTION
1. Stop all java processes (Spring Boot backend).
2. Stop the node process listening on 5175 (Vite dev server).
3. Stop cpolar.
Then verify all ports are released.
#>

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
& chcp.com 65001 | Out-Null

Write-Host "Stopping backend (java)..."
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host "Stopping frontend (Vite on 5175)..."
$fe = Get-NetTCPConnection -LocalPort 5175 -State Listen -ErrorAction SilentlyContinue
if ($fe) {
    Get-Process -Id ($fe.OwningProcess | Select-Object -Unique) -ErrorAction SilentlyContinue | Stop-Process -Force
}

Write-Host "Stopping cpolar..."
Get-Process -Name cpolar -ErrorAction SilentlyContinue | Stop-Process -Force

Start-Sleep -Seconds 2

$be = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
$fe2 = Get-NetTCPConnection -LocalPort 5175 -State Listen -ErrorAction SilentlyContinue
$cp = Get-Process -Name cpolar -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "backend 8080: $(if($be){'STILL UP'}else{'stopped'}) | frontend 5175: $(if($fe2){'STILL UP'}else{'stopped'}) | cpolar: $(if($cp){'STILL UP'}else{'stopped'})"
