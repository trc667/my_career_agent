# 积分商城端到端验证：商品列表 / 兑换扣分 / 资料发放 / VIP 卡开通 / 积分不足
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

# 1. admin(demo) 给 testuser01 发 300 积分（走管理员流水）
$admin = Invoke-Api -Method POST -Uri "http://localhost:8080/api/auth/login" -Body '{"username":"demo","password":"123456"}'
Log "admin 登录: $($admin.data.username)"
try {
    $grant = Invoke-Api -Method POST -Uri "http://localhost:8080/api/admin/points" -Token $admin.data.token -Body '{"userId":1,"delta":300,"reason":"商城验证发分"}'
    Log "发分成功: $($grant.message)"
} catch {
    Log "发分失败: $_"
}

# 2. testuser01 商城流程
$login = Invoke-Api -Method POST -Uri "http://localhost:8080/api/auth/login" -Body '{"username":"testuser01","password":"test123456"}'
$t = $login.data.token
$profile = Invoke-Api -Method GET -Uri "http://localhost:8080/api/user/points" -Token $t
Log "testuser01 积分: $($profile.data.points) level=$($profile.data.level)"

$items = Invoke-Api -Method GET -Uri "http://localhost:8080/api/shop/items" -Token $t
Log "商品列表: $($items.data.Count) 件"
foreach($it in $items.data){ Log "  - [$($it.id)] $($it.name) $($it.points)分 type=$($it.type)" }

# 3. 兑换简历模板（30 分 CONTENT）
$r1 = Invoke-Api -Method POST -Uri "http://localhost:8080/api/shop/redeem" -Token $t -Body '{"itemId":1}'
Log "兑换[简历模板合集]: cost=$($r1.data.cost) pointsLeft=$($r1.data.pointsLeft) payload前20字=$($r1.data.payload.Substring(0,[Math]::Min(20,$r1.data.payload.Length)))"

# 4. 兑换 VIP 卡（200 分 VIP_CARD）
try {
    $r2 = Invoke-Api -Method POST -Uri "http://localhost:8080/api/shop/redeem" -Token $t -Body '{"itemId":4}'
    Log "兑换[7天VIP体验卡]: cost=$($r2.data.cost) pointsLeft=$($r2.data.pointsLeft) payload=$($r2.data.payload)"
    $profile2 = Invoke-Api -Method GET -Uri "http://localhost:8080/api/user/points" -Token $t
    Log "兑换后 level=$($profile2.data.level) vipExpireAt=$($profile2.data.vipExpireAt) [预期 VIP]"
} catch {
    Log "VIP 卡兑换失败 [FAIL]: $_"
}

# 5. 积分不足场景（剩余 < 200 再兑 VIP 卡应失败）
$profile3 = Invoke-Api -Method GET -Uri "http://localhost:8080/api/user/points" -Token $t
Log "剩余积分: $($profile3.data.points)（再兑 200 分 VIP 卡应被拒）"
try {
    $r3 = Invoke-Api -Method POST -Uri "http://localhost:8080/api/shop/redeem" -Token $t -Body '{"itemId":4}'
    Log "重复兑换 VIP 未拒绝 [FAIL] pointsLeft=$($r3.data.pointsLeft)"
} catch {
    Log "积分不足兑换被拒 [PASS] -> $_"
}

# 6. 兑换记录
$records = Invoke-Api -Method GET -Uri "http://localhost:8080/api/shop/records" -Token $t
Log "兑换记录: $($records.data.Count) 条"
foreach($rec in $records.data){ Log "  - $($rec.itemName) -$($rec.points)分 $($rec.createTime)" }

$outPath = "C:\Users\tan\Desktop\my_agent\my_career_agent\shop-e2e-result.txt"
[System.IO.File]::WriteAllLines($outPath, $out, [System.Text.Encoding]::UTF8)
Write-Host "RESULT_FILE=$outPath"
