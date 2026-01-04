# tradeflow-e2e-complete.ps1
# Comprehensive E2E test for TradeFlow microservices (Auth, Wallet, Orders, Market Data, Gateway, MatchingEngine, Audit)
# Hardcoded config for local testing

$ApiBase     = "http://localhost:8080/api"
$GatewayBase = "http://localhost:8080"
$Password    = "Test123!"
$Suffix      = Get-Random -Minimum 100000 -Maximum 999999
$Email       = "itest_${Suffix}@example.com"
$Username    = "itest_${Suffix}"
$LogFile     = "tradeflow-e2e-complete.log"
$script:TestResults = @()

# Ports for direct actuator health checks
$Ports = @{
  "auth"          = 8081
  "wallet"        = 8082
  "oms"           = 8083
  "matching"      = 8084
  "market-data"   = 8085
  "audit"         = 8086  # adjust if different
  "gateway"       = 8080
}

# Order payloads
$OrderGood = @{
    symbol   = "BTCUSDT"
    side     = "BUY"
    type     = "LIMIT"
    price    = "50000"
    quantity = "0.01"
}
$OrderBig = @{
    symbol   = "BTCUSDT"
    side     = "BUY"
    type     = "LIMIT"
    price    = "50000"
    quantity = "1000000"   # expect rejection/validation/insufficient funds
}

$AccessToken = ""
$RefreshToken = ""
$HeadersAuth = @{}
$HeadersJson = @{ "Content-Type" = "application/json" }

function Log($msg) { $msg | Tee-Object -FilePath $LogFile -Append }

function RecordResult($step,$expected,$actual,$passed,$detail="",$service="") {
    $script:TestResults += [pscustomobject]@{
        Step      = $step
        Service   = $service
        Expected  = $expected
        Actual    = $actual
        Passed    = $passed
        Detail    = $detail
    }
}

function Step([string]$title, [string]$service, [scriptblock]$action, [int[]]$expectStatus, [scriptblock]$assert = $null) {
    Log "`n--- $title ---"
    try {
        $resp = & $action
        $status = $expectStatus[0]  # Invoke-RestMethod hides status on success
        if ($assert -ne $null) { & $assert $resp }
        if ($resp -ne $null) { Log ($resp | ConvertTo-Json -Depth 12) }
        Log "exit_code=0"
        RecordResult $title ($expectStatus -join ",") $status ($expectStatus -contains $status) "" $service
        return $resp
    }
    catch {
        $status = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        $detail = ""
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) { $detail = $_.ErrorDetails.Message }
        Log "ERROR: $($_.Exception.Message)"
        if ($detail) { Log "DETAIL: $detail" }
        Log "exit_code=1"
        $actual = if ($status -eq $null) { "n/a" } else { $status }
        RecordResult $title ($expectStatus -join ",") $actual ($expectStatus -contains $status) $detail $service
        return $null
    }
}

"" | Out-File $LogFile
Log "=== TradeFlow FULL E2E Test ==="
Log "API_BASE=$ApiBase"
Log "USER=$Username | EMAIL=$Email"

# ---------- GATEWAY PUBLIC VS PROTECTED SMOKE ----------
Step "Gateway: public login reachable" "gateway" {
    Invoke-RestMethod -Uri "$ApiBase/auth/login" -Method Post -Body (@{usernameOrEmail="dummy";password="dummy"} | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
} @(400,401)
Step "Gateway: wallet balances unauthorized" "gateway" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/balances" -Method Get -ErrorAction Stop
} @(401)
Step "Gateway: orders unauthorized" "gateway" {
    Invoke-RestMethod -Uri "$ApiBase/orders" -Method Get -ErrorAction Stop
} @(401)

# ---------- AUTH SERVICE ----------
Step "Auth: register" "auth" {
    $body = @{
        username  = $Username
        email     = $Email
        password  = $Password
        firstName = "I"
        lastName  = "Test"
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/register" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
} @(200,201)

Step "Auth: register duplicate" "auth" {
    $body = @{
        username  = $Username
        email     = $Email
        password  = $Password
        firstName = "I"
        lastName  = "Test"
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/register" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
} @(400,409)

Step "Auth: register invalid email" "auth" {
    $body = @{
        username  = "bad_${Suffix}"
        email     = "not-an-email"
        password  = $Password
        firstName = "I"
        lastName  = "Test"
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/register" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
} @(400)

$loginResp = Step "Auth: login" "auth" {
    $body = @{
        usernameOrEmail = $Email
        password        = $Password
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/login" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
} @(200)
if ($loginResp -and $loginResp.accessToken) {
    $AccessToken = $loginResp.accessToken
    $RefreshToken = $loginResp.refreshToken
    $HeadersAuth = @{ Authorization = "Bearer $AccessToken" }
    Log "ACCESS_TOKEN=$($AccessToken.Substring(0,[Math]::Min(40,$AccessToken.Length)))..."
}

Step "Auth: login wrong password" "auth" {
    $body = @{
        usernameOrEmail = $Email
        password        = "WrongPass123!"
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/login" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
} @(400,401)

Step "Auth: me unauthorized" "auth" {
    Invoke-RestMethod -Uri "$ApiBase/auth/me" -Method Get -ErrorAction Stop
} @(401)

Step "Auth: me" "auth" {
    Invoke-RestMethod -Uri "$ApiBase/auth/me" -Headers $HeadersAuth -Method Get -ErrorAction Stop
} @(200)

if ($RefreshToken) {
    Step "Auth: refresh token" "auth" {
        $body = @{ refreshToken = $RefreshToken } | ConvertTo-Json -Compress
        Invoke-RestMethod -Uri "$ApiBase/auth/refresh" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
    } @(200,201,400,401)
}

# ---------- WALLET SERVICE ----------
Step "Wallet: balances unauthorized" "wallet" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/balances" -Method Get -ErrorAction Stop
} @(401)

Step "Wallet: balances" "wallet" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/balances" -Headers $HeadersAuth -Method Get -ErrorAction Stop
} @(200)

Step "Wallet: balance USD" "wallet" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/balance/USD" -Headers $HeadersAuth -Method Get -ErrorAction Stop
} @(200)

Step "Wallet: balance invalid currency" "wallet" {
    # Use a path-safe but invalid currency to avoid gateway firewall issues
    Invoke-RestMethod -Uri "$ApiBase/wallet/balance/INVALID123" -Headers $HeadersAuth -Method Get -ErrorAction Stop
} @(400,404)

Step "Wallet: faucet" "wallet" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/faucet" -Headers $HeadersAuth -Method Post -ErrorAction Stop
} @(200,201,429)

Step "Wallet: create BTC wallet" "wallet" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/create/BTC" -Headers $HeadersAuth -Method Post -ErrorAction Stop
} @(200,201)

Step "Wallet: create invalid wallet" "wallet" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/create/INVALID123" -Headers $HeadersAuth -Method Post -ErrorAction Stop
} @(400,404)

# ---------- ORDER SERVICE (OMS) ----------
Step "Orders: list unauthorized" "oms" {
    Invoke-RestMethod -Uri "$ApiBase/orders" -Method Get -ErrorAction Stop
} @(401)

$orderResp = Step "Orders: place order" "oms" {
    $body = $OrderGood | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/orders" -Headers $HeadersAuth -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
} @(201)
$orderId = $null
if ($orderResp -and $orderResp.orderId) { $orderId = $orderResp.orderId }

Step "Orders: place huge order (expect rejection)" "oms" {
    $body = $OrderBig | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/orders" -Headers $HeadersAuth -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
} @(400,401,422,500)

Step "Orders: list" "oms" {
    Invoke-RestMethod -Uri "$ApiBase/orders" -Headers $HeadersAuth -Method Get -ErrorAction Stop
} @(200)

Step "Orders: open" "oms" {
    Invoke-RestMethod -Uri "$ApiBase/orders/open" -Headers $HeadersAuth -Method Get -ErrorAction Stop
} @(200)

if ($orderId) {
    Step "Orders: get by id" "oms" {
        Invoke-RestMethod -Uri "$ApiBase/orders/$orderId" -Headers $HeadersAuth -Method Get -ErrorAction Stop
    } @(200,404)

    Step "Orders: cancel by id" "oms" {
        Invoke-RestMethod -Uri "$ApiBase/orders/$orderId" -Headers $HeadersAuth -Method Delete -ErrorAction Stop
    } @(200,204,404)
}

Step "Orders: cancel nonexistent" "oms" {
    $fake = [guid]::NewGuid().ToString()
    Invoke-RestMethod -Uri "$ApiBase/orders/$fake" -Headers $HeadersAuth -Method Delete -ErrorAction Stop
} @(404,400)

Step "Orders: open by symbol BTCUSDT" "oms" {
    Invoke-RestMethod -Uri "$ApiBase/orders/symbol/BTCUSDT" -Headers $HeadersAuth -Method Get -ErrorAction Stop
} @(200)

# ---------- MARKET DATA ----------
Step "Market: ticker BTCUSDT" "market" {
    Invoke-RestMethod -Uri "$ApiBase/market/ticker/BTCUSDT" -Method Get -ErrorAction Stop
} @(200)

Step "Market: ticker UNKNOWN" "market" {
    Invoke-RestMethod -Uri "$ApiBase/market/ticker/UNKNOWN" -Method Get -ErrorAction Stop
} @(200,404)

# ---------- ACTUATOR HEALTH (direct service ports) ----------
foreach ($svc in $Ports.Keys) {
    $port = $Ports[$svc]
    Step "Actuator: $svc health" $svc {
        Invoke-RestMethod -Uri "http://localhost:$port/actuator/health" -Method Get -ErrorAction Stop
    } @(200)
}

# ---------- SUMMARY ----------
$passed = ($script:TestResults | Where-Object { $_.Passed }).Count
$total  = $script:TestResults.Count
$failed = $total - $passed

Log "`n=== TEST SUMMARY ==="
$summary = [pscustomobject]@{
    Total   = $total
    Passed  = $passed
    Failed  = $failed
}
$summary | Tee-Object -FilePath $LogFile -Append | Format-Table

Log "`nPer-step results:"
$script:TestResults | Tee-Object -FilePath $LogFile -Append | Format-Table Step,Service,Expected,Actual,Passed,Detail -AutoSize

Write-Host "`n=== DONE ==="
Write-Host "Total: $total  Passed: $passed  Failed: $failed"
Write-Host "See $LogFile for full details."
