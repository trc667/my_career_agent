# ============================================================
# 数据库备份脚本：MySQL(ai_love_master) + PostgreSQL(vectordb)
# 用法：
#   $env:MYSQL_PASSWORD="你的MySQL密码"; $env:PGPASSWORD="你的PG密码"
#   powershell -File scripts/backup-db.ps1
# 可选参数：
#   -BackupDir 备份目录（默认 backups/db）
#   -KeepCount 保留最近 N 份（默认 7）
# 密码一律从环境变量读取，脚本不含敏感值，可安全提交仓库。
# ============================================================

param(
  [string]$BackupDir = "backups/db",
  [int]$KeepCount = 7
)

$ErrorActionPreference = "Stop"
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$success = $true

function Write-Step($msg) { Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $msg" -ForegroundColor Cyan }
function Write-ErrorMsg($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red }

# ---------- 工具函数：.NET GZip 压缩（Windows 无内置 gzip） ----------
function Compress-GzipFile([string]$src, [string]$dst) {
  $in = [System.IO.File]::OpenRead($src)
  try {
    $out = [System.IO.File]::Create($dst)
    try {
      $gz = New-Object System.IO.Compression.GzipStream($out, [System.IO.Compression.CompressionLevel]::Optimal)
      try { $in.CopyTo($gz) } finally { $gz.Dispose() }
    } finally { $out.Dispose() }
  } finally { $in.Dispose() }
}

# ---------- 1. 目录 ----------
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
Write-Step "备份目录: $((Resolve-Path $BackupDir).Path)"

# ---------- 2. MySQL 备份 ----------
$mysqlHost = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "localhost" }
$mysqlPort = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
$mysqlUser = if ($env:MYSQL_USER) { $env:MYSQL_USER } else { "root" }
$mysqlDb   = if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "ai_love_master" }

if (-not $env:MYSQL_PASSWORD) {
  Write-ErrorMsg "缺少环境变量 MYSQL_PASSWORD，跳过 MySQL 备份"
  $success = $false
} elseif (-not (Get-Command mysqldump -ErrorAction SilentlyContinue)) {
  Write-ErrorMsg "未找到 mysqldump（请将 MySQL bin 加入 PATH），跳过 MySQL 备份"
  $success = $false
} else {
  $mysqlTmp = Join-Path $BackupDir "mysql_${ts}.tmp.sql"
  $mysqlOut = Join-Path $BackupDir "mysql_${ts}.sql.gz"
  Write-Step "MySQL 备份中: $mysqlDb -> $mysqlOut"
  $env:MYSQL_PWD = $env:MYSQL_PASSWORD
  try {
    & mysqldump -h $mysqlHost -P $mysqlPort -u $mysqlUser --single-transaction --routines $mysqlDb 2>$null | Out-File -Encoding utf8 -FilePath $mysqlTmp
    if ((Test-Path $mysqlTmp) -and (Get-Item $mysqlTmp).Length -gt 0) {
      Compress-GzipFile $mysqlTmp $mysqlOut
      Remove-Item $mysqlTmp -Force
      Write-Step "MySQL 备份完成: $([math]::Round((Get-Item $mysqlOut).Length / 1KB, 1)) KB"
    } else {
      Remove-Item $mysqlTmp -Force -ErrorAction SilentlyContinue
      Write-ErrorMsg "MySQL 备份失败（输出为空）"
      $success = $false
    }
  } catch {
    Remove-Item $mysqlTmp -Force -ErrorAction SilentlyContinue
    Write-ErrorMsg "MySQL 备份异常: $($_.Exception.Message)"
    $success = $false
  } finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
  }
}

# ---------- 3. PostgreSQL 备份（自动探测 pg_dump 安装路径） ----------
$pgHost = if ($env:PG_HOST) { $env:PG_HOST } else { "localhost" }
$pgPort = if ($env:PG_PORT) { $env:PG_PORT } else { "5432" }
$pgUser = if ($env:PG_USER) { $env:PG_USER } else { "postgres" }
$pgDb   = if ($env:PG_DATABASE) { $env:PG_DATABASE } else { "vectordb" }

if (-not $env:PGPASSWORD) {
  Write-ErrorMsg "缺少环境变量 PGPASSWORD，跳过 PostgreSQL 备份"
  $success = $false
} else {
  $pgDumpCmd = $null
  $pgDumpFound = Get-Command pg_dump -ErrorAction SilentlyContinue
  if ($pgDumpFound) {
    $pgDumpCmd = $pgDumpFound.Source
  } else {
    # 常见安装路径：C:\Program Files\PostgreSQL\<版本>\bin\pg_dump.exe
    $pgRoot = Get-ChildItem "C:\Program Files\PostgreSQL" -Directory -ErrorAction SilentlyContinue |
      Sort-Object Name -Descending | Select-Object -First 1
    if ($pgRoot) {
      $candidate = Join-Path $pgRoot.FullName "bin\pg_dump.exe"
      if (Test-Path $candidate) { $pgDumpCmd = $candidate }
    }
  }

  if (-not $pgDumpCmd) {
    Write-ErrorMsg "未找到 pg_dump（请将 PostgreSQL bin 加入 PATH），跳过 PostgreSQL 备份"
    $success = $false
  } else {
    $pgTmp = Join-Path $BackupDir "pg_${ts}.tmp.sql"
    $pgOut = Join-Path $BackupDir "pg_${ts}.sql.gz"
    Write-Step "PostgreSQL 备份中: $pgDb -> $pgOut"
    try {
      & $pgDumpCmd -h $pgHost -p $pgPort -U $pgUser -d $pgDb 2>$null | Out-File -Encoding utf8 -FilePath $pgTmp
      if ((Test-Path $pgTmp) -and (Get-Item $pgTmp).Length -gt 0) {
        Compress-GzipFile $pgTmp $pgOut
        Remove-Item $pgTmp -Force
        Write-Step "PostgreSQL 备份完成: $([math]::Round((Get-Item $pgOut).Length / 1KB, 1)) KB"
      } else {
        Remove-Item $pgTmp -Force -ErrorAction SilentlyContinue
        Write-ErrorMsg "PostgreSQL 备份失败（输出为空）"
        $success = $false
      }
    } catch {
      Remove-Item $pgTmp -Force -ErrorAction SilentlyContinue
      Write-ErrorMsg "PostgreSQL 备份异常: $($_.Exception.Message)"
      $success = $false
    }
  }
}

# ---------- 4. 清理旧备份（仅保留最近 KeepCount 份） ----------
$all = Get-ChildItem $BackupDir -Filter "*.sql.gz" | Sort-Object LastWriteTime -Descending
$toRemove = $all | Select-Object -Skip $KeepCount
foreach ($old in $toRemove) {
  Remove-Item $old.FullName -Force
  Write-Step "清理旧备份: $($old.Name)"
}
Write-Step "当前保留: $(($all.Count - $toRemove.Count)) 份"

# ---------- 5. 结果 ----------
if ($success) {
  Write-Host "[OK] 数据库备份完成" -ForegroundColor Green
  exit 0
} else {
  Write-Host "[WARN] 备份存在失败项，请检查上方日志" -ForegroundColor Yellow
  exit 1
}
