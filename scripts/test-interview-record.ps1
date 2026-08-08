# 面试记录落库端到端验证：完成一场面试 → 记录列表 → 详情
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$out = [System.Collections.Generic.List[string]]::new()
function Log($m) { $out.Add($m); Write-Host $m }

function Invoke-Api {
    param($Method, $Uri, $Token, $Body)
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    try {
        if ($Body) { $resp = Invoke-RestMethod -Uri $Uri -Method $Method -ContentType "application/json; charset=utf-8" -Headers $headers -Body $Body }
        else { $resp = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers }
        if ($resp.code -ne 200) { throw "BIZ_ERROR code=$($resp.code) msg=$($resp.message)" }
        return $resp
    } catch {
        $detail = $_.ErrorDetails.Message
        if (-not $detail) { $detail = $_.Exception.Message }
        throw "HTTP_ERROR $($_.Exception.Response.StatusCode.value__) $detail"
    }
}

$login = Invoke-Api -Method POST -Uri "http://localhost:8080/api/auth/login" -Body '{"username":"demo","password":"123456"}'
$t = $login.data.token
Log "登录: $($login.data.username)"

# 1. 开始面试
$start = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/start" -Token $t -Body '{"position":"\u540e\u7aef"}'
$sid = $start.data.sessionId
Log "start: session=$($sid) 第1题=$($start.data.question.Substring(0,[Math]::Min(30,$start.data.question.Length)))..."

# 2. 完成 5 题
for ($i = 1; $i -le 5; $i++) {
    $body = @{ sessionId = $sid; answer = "Answer for Q$i : core concept, structure, completeness." } | ConvertTo-Json -Compress
    $ans = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/answer" -Token $t -Body $body
    Log "answer[$i]: finished=$($ans.data.finished) score=$($ans.data.review.totalScore)"
    if ($i -lt 5) { Start-Sleep -Milliseconds 500 }
}

# 3. report（触发落库）
$rep = Invoke-Api -Method GET -Uri "http://localhost:8080/api/interview/report?sessionId=$sid" -Token $t
Log "report: position=$($rep.data.position) total=$($rep.data.totalScore) items=$($rep.data.items.Count)"

# 4. 记录列表
$records = Invoke-Api -Method GET -Uri "http://localhost:8080/api/interview/records" -Token $t
Log "records 总数: $($records.data.Count)"
foreach($rec in $records.data){ Log "  - [$($rec.id)] $($rec.position)岗 $($rec.totalScore)分 dims=$($rec.dimensions.Count) $($rec.createdAt)" }

# 5. 详情
if ($records.data.Count -gt 0) {
    $detail = Invoke-Api -Method GET -Uri "http://localhost:8080/api/interview/records/$($records.data[0].id)" -Token $t
    Log "detail: position=$($detail.data.position) total=$($detail.data.totalScore) items=$($detail.data.items.Count) 第1题=$($detail.data.items[0].question.Substring(0,[Math]::Min(25,$detail.data.items[0].question.Length)))... score=$($detail.data.items[0].score)"
}

# 6. 周报/看板面试维度
$weekly = Invoke-Api -Method GET -Uri "http://localhost:8080/api/user/weekly-report" -Token $t
Log "周报 interviews=$($weekly.data.output.interviews)"
$admin = Invoke-Api -Method POST -Uri "http://localhost:8080/api/auth/login" -Body '{"username":"demo","password":"123456"}'
$stats = Invoke-Api -Method GET -Uri "http://localhost:8080/api/admin/stats" -Token $admin.data.token
Log "看板 interviews total=$($stats.data.interviews.total) week=$($stats.data.interviews.week)"

$outPath = "C:\Users\tan\Desktop\my_agent\my_career_agent\interview-record-e2e.txt"
[System.IO.File]::WriteAllLines($outPath, $out, [System.Text.Encoding]::UTF8)
Write-Host "RESULT_FILE=$outPath"
