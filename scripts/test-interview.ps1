# AI 面试模拟端到端验证：FREE 每日 2 次 / VIP 不限次 / 5 题完整流程
# 结果写入 interview-e2e-result.txt（UTF-8，避免控制台乱码）
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$out = [System.Collections.Generic.List[string]]::new()
function Log($m) { $out.Add($m); Write-Host $m }

function Invoke-Api {
    param($Method, $Uri, $Token, $Body)
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    try {
        if ($Body) {
            $resp = Invoke-RestMethod -Uri $Uri -Method $Method -ContentType "application/json; charset=utf-8" -Headers $headers -Body $Body
        } else {
            $resp = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers
        }
        if ($resp.code -ne 200) { throw "BIZ_ERROR code=$($resp.code) msg=$($resp.message)" }
        return $resp
    } catch {
        $detail = $_.ErrorDetails.Message
        if (-not $detail) { $detail = $_.Exception.Message }
        throw "HTTP_ERROR $($_.Exception.Response.StatusCode.value__) $detail"
    }
}

Log "===== 1. FREE 用户流程（testuser01，预期每日 2 次） ====="
$login = Invoke-Api -Method POST -Uri "http://localhost:8080/api/auth/login" -Body '{"username":"testuser01","password":"test123456"}'
$freeToken = $login.data.token
Log "登录: $($login.data.username) role=$($login.data.role)"

$quota = Invoke-Api -Method GET -Uri "http://localhost:8080/api/interview/quota" -Token $freeToken
Log "quota: vip=$($quota.data.vip) dailyLimit=$($quota.data.dailyLimit) quotaLeft=$($quota.data.quotaLeft) [预期 vip=False quotaLeft=2]"

$start = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/start" -Token $freeToken -Body '{"position":"\u540e\u7aef"}'
$sid = $start.data.sessionId
Log "start#1: sessionId=$sid index=$($start.data.index) total=$($start.data.total) vip=$($start.data.vip) quotaLeft=$($start.data.quotaLeft)"
Log "第 1 题: $($start.data.question)"

for ($i = 1; $i -le 5; $i++) {
    $q = if ($i -eq 1) { $start.data.question } else { "第 ${i} 题（续）" }
    $body = @{ sessionId = $sid; answer = "My answer for Q$i on $q : covers core concept, structure, and completeness." } | ConvertTo-Json -Compress
    $ans = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/answer" -Token $freeToken -Body $body
    $dims = ($ans.data.review.dimensions | ForEach-Object { "$($_.name):$($_.score)" }) -join ","
    Log "answer[$i]: finished=$($ans.data.finished) totalScore=$($ans.data.review.totalScore) dims=[$dims]"
    if ($i -lt 5) { Log "  下一题: $($ans.data.nextQuestion)" }
}

$report = Invoke-Api -Method GET -Uri "http://localhost:8080/api/interview/report?sessionId=$sid" -Token $freeToken
$repDims = ($report.data.dimensions | ForEach-Object { "$($_.name):$($_.score)" }) -join ","
Log "report: position=$($report.data.position) totalScore=$($report.data.totalScore) dims=[$repDims] items=$($report.data.items.Count)"

Log "===== 2. FREE 第 2 次 + 第 3 次（第 3 次应被拒） ====="
$start2 = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/start" -Token $freeToken -Body '{"position":"\u524d\u7aef"}'
Log "start#2: quotaLeft=$($start2.data.quotaLeft) [预期 0]"
try {
    $start3 = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/start" -Token $freeToken -Body '{"position":"\u540e\u7aef"}'
    Log "start#3: 未按预期拒绝！返回 code=$($start3.code) msg=$($start3.message) [FAIL]"
} catch {
    Log "start#3: 已拒绝 [PASS] -> $_"
}

Log "===== 3. VIP 流程（demo，预期不限次 + qwen-max 深度点评） ====="
$vipLogin = Invoke-Api -Method POST -Uri "http://localhost:8080/api/auth/login" -Body '{"username":"demo","password":"123456"}'
$vipToken = $vipLogin.data.token
Log "登录: $($vipLogin.data.username) role=$($vipLogin.data.role)"

$vipQuota = Invoke-Api -Method GET -Uri "http://localhost:8080/api/interview/quota" -Token $vipToken
Log "quota: vip=$($vipQuota.data.vip) quotaLeft=$($vipQuota.data.quotaLeft) [预期 vip=True quotaLeft=-1]"

$vipStart = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/start" -Token $vipToken -Body '{"position":"\u7b97\u6cd5"}'
Log "start: vip=$($vipStart.data.vip) quotaLeft=$($vipStart.data.quotaLeft) index=$($vipStart.data.index)"
Log "VIP 第 1 题: $($vipStart.data.question)"

$vipBody = @{ sessionId = $vipStart.data.sessionId; answer = "Deep answer: algorithm complexity analysis with tradeoffs and extension." } | ConvertTo-Json -Compress
$vipAns = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/answer" -Token $vipToken -Body $vipBody
$vipDims = ($vipAns.data.review.dimensions | ForEach-Object { $_.name }) -join "/"
Log "VIP answer: totalScore=$($vipAns.data.review.totalScore) dims=[$vipDims] [预期 4 维度含深度与见解]"

Log "===== 4. VIP 连续 start（不限次验证，再开 2 次应成功） ====="
try {
    $v1 = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/start" -Token $vipToken -Body '{"position":"\u6d4b\u8bd5"}'
    Log "VIP start#2(测试): quotaLeft=$($v1.data.quotaLeft) index=$($v1.data.index) [PASS，测试岗兜底出题]"
    $v2 = Invoke-Api -Method POST -Uri "http://localhost:8080/api/interview/start" -Token $vipToken -Body '{"position":"\u8fd0\u7ef4"}'
    Log "VIP start#3(运维): quotaLeft=$($v2.data.quotaLeft) index=$($v2.data.index) [PASS，运维岗兜底出题]"
} catch {
    Log "VIP 连续 start 被拒 [FAIL] -> $_"
}

$outPath = "C:\Users\tan\Desktop\my_agent\my_career_agent\interview-e2e-result.txt"
[System.IO.File]::WriteAllLines($outPath, $out, [System.Text.Encoding]::UTF8)
Write-Host "RESULT_FILE=$outPath"
