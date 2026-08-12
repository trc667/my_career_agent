<#
.SYNOPSIS
One-command startup: backend (8080) + frontend (5175) + cpolar dual tunnels.

.USAGE
.\scripts\start-all.ps1

.DESCRIPTION
1. Start Spring Boot backend (JDK17, dev profile) in a minimized window if port 8080 is free.
2. Start Vite dev server in a minimized window if port 5175 is free.
3. Start cpolar tunnels (website -> 8080, frontend -> 5175) if not running, log to logs\cpolar.log.
4. Wait for backend health, then print local URL and the fresh cpolar tunnel URLs to share.

.NOTES
- English output only (PowerShell 5.1 reads no-BOM UTF-8 scripts as GBK, Chinese would garble).
- cpolar free plan assigns NEW random domains on every restart; grab the new frontend URL printed at the end.
#>

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
& chcp.com 65001 | Out-Null

$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repoRoot
$webDir = Join-Path $repoRoot "ai-love-master-web"
$logDir = Join-Path $repoRoot "logs"
$cpolarExe = "C:\Users\tan\Desktop\cpolar\cpolar.exe"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# ---------- 1. backend ----------
$be = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($be) {
    Write-Host "[1/3] backend 8080 already running"
} else {
    Write-Host "[1/3] starting backend (Spring Boot, JDK17)..."
    $env:JAVA_HOME = "C:\Users\tan\.jdks\ms-17.0.16"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Start-Process powershell -ArgumentList "-NoProfile","-ExecutionPolicy","Bypass","-File","$repoRoot\scripts\run-spring-boot-utf8.ps1" -WindowStyle Minimized
}

# ---------- 2. frontend ----------
$fe = Get-NetTCPConnection -LocalPort 5175 -State Listen -ErrorAction SilentlyContinue
if ($fe) {
    Write-Host "[2/3] frontend 5175 already running"
} else {
    Write-Host "[2/3] starting frontend (Vite)..."
    Start-Process powershell -ArgumentList "-NoProfile","-Command","Set-Location '$webDir'; npm run dev" -WindowStyle Minimized
}

# ---------- 3. cpolar dual tunnels ----------
$cp = Get-Process -Name cpolar -ErrorAction SilentlyContinue
if ($cp) {
    Write-Host "[3/3] cpolar already running"
} else {
    if (Test-Path $cpolarExe) {
        Write-Host "[3/3] starting cpolar tunnels (frontend 5175 + backend 8080)..."
        $cpLog = Join-Path $logDir "cpolar.log"
        Start-Process $cpolarExe -ArgumentList "start","website","frontend","-log","stdout","-log-level","INFO" `
            -RedirectStandardOutput $cpLog -RedirectStandardError "$cpLog.err" -WindowStyle Hidden
    } else {
        Write-Host "[3/3] WARNING: cpolar.exe not found at $cpolarExe - start it manually"
    }
}

# ---------- 4. wait for backend health ----------
Write-Host ""
Write-Host "Waiting for backend health..."
$ok = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 5
    try {
        $r = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -TimeoutSec 5
        if ($r.data -eq "ok") { $ok = $true; break }
    } catch {}
}
if ($ok) { Write-Host "  backend: OK (localhost:8080)" } else { Write-Host "  backend: NOT ready yet - check the backend console window" }

$fe2 = Get-NetTCPConnection -LocalPort 5175 -State Listen -ErrorAction SilentlyContinue
if ($fe2) { Write-Host "  frontend: OK (http://localhost:5175)" } else { Write-Host "  frontend: NOT ready yet - check the frontend console window" }

# ---------- 5. print cpolar tunnel URLs from log (wait until both tunnels are up) ----------
$cpLog = Join-Path $logDir "cpolar.log"
$seen = @{}
if (Test-Path $cpLog) {
    for ($i = 0; $i -lt 6 -and $seen.Count -lt 2; $i++) {
        $matches = Select-String -Path $cpLog -Pattern "https://[a-z0-9]+\.r[0-9]+(\.vip)?\.cpolar\.(top|cn|com)" -AllMatches
        foreach ($m in $matches) {
            foreach ($hit in $m.Matches) {
                $url = $hit.Value.Trim()
                if (-not $seen.ContainsKey($url)) { $seen[$url] = $true }
            }
        }
        if ($seen.Count -lt 2) { Start-Sleep -Seconds 5 }
    }
    if ($seen.Count -gt 0) {
        Write-Host ""
        Write-Host "cpolar tunnels:"
        foreach ($u in ($seen.Keys | Sort-Object)) { Write-Host "  $u" }
        if ($seen.Count -lt 2) { Write-Host "  (only $($seen.Count) tunnel(s) found - check $cpLog in a few seconds)" }
    } else {
        Write-Host "  tunnel URLs not found yet - check $cpLog in a few seconds"
    }
} else {
    Write-Host "  cpolar log not found - check the cpolar window for tunnel URLs"
}

Write-Host ""
Write-Host "Done. Local: http://localhost:5175  |  Share the FRONTEND tunnel URL above with others."
