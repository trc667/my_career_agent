# 数据库定时备份说明

## 一、手动执行一次备份

```powershell
# 1. 设置数据库密码环境变量（不写入脚本/仓库）
$env:MYSQL_PASSWORD="你的MySQL密码"
$env:PGPASSWORD="你的PostgreSQL密码"

# 2. 执行备份（MySQL ai_love_master + PostgreSQL vectordb → backups/db/*.sql.gz）
powershell -File scripts/backup-db.ps1
```

可选参数：`-BackupDir 备份目录`（默认 `backups/db`）、`-KeepCount 保留份数`（默认 7，自动清理旧备份）。

## 二、Windows 计划任务（每天 03:00 自动备份）

```powershell
schtasks /create /tn "CareerMasterDBBackup" /sc daily /st 03:00 /f ^
  /tr "powershell -ExecutionPolicy Bypass -File C:\path\to\my_career_agent\scripts\backup-db.ps1"
```

> 注意：计划任务环境变量需在任务内配置（系统级），或在脚本所在用户环境设置 `MYSQL_PASSWORD` / `PGPASSWORD`。
> 可用 `setx MYSQL_PASSWORD "你的密码"` 持久化（仅当前用户，勿提交仓库）。

查询/删除任务：

```powershell
schtasks /query /tn "CareerMasterDBBackup"
schtasks /delete /tn "CareerMasterDBBackup" /f
```

## 三、恢复

```bash
# MySQL（先建库）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS ai_love_master DEFAULT CHARSET utf8mb4;"
gunzip -c backups/db/mysql_20260806_030000.sql.gz | mysql -uroot -p ai_love_master

# PostgreSQL（先建库）
createdb -U postgres vectordb
gunzip -c backups/db/pg_20260806_030000.sql.gz | psql -U postgres -d vectordb
```

## 四、可选：备份上传 OSS

如需异地容灾，可扩展脚本在备份后上传 `backups/db/*.sql.gz` 到阿里云 OSS（环境变量配置
`OSS_ENDPOINT` / `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` / `OSS_BACKUP_BUCKET`），
默认跳过（保持脚本零依赖）。

## 五、注意事项

- 密码只从环境变量读取，脚本本身不含任何敏感值，可安全提交仓库
- `backups/` 目录已在 .gitignore 中忽略，不会污染仓库
- 生产环境建议同时保留本地与异地（OSS）各一份
