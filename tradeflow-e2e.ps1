# tradeflow-e2e.ps1
# End-to-end smoke via API Gateway (auth, wallet, orders, market)
# All settings hardcoded here.

$ApiBase   = "http://localhost:8080/api"
$Password  = "Test123!"
$Suffix    = Get-Random -Minimum 100000 -Maximum 999999
$Email     = "itest_${Suffix}@example.com"
$Username  = "itest_${Suffix}"
$LogFile   = "tradeflow-e2e.log"

# Order payload
$OrderPayload = @{
    symbol   = "BTCUSDT"
    side     = "BUY"
    type     = "LIMIT"
    price    = "50000"
    quantity = "0.01"
}

$AccessToken = ""
$HeadersAuth = @{}
$HeadersJson = @{ "Content-Type" = "application/json" }

function Log($msg) {
    $msg | Tee-Object -FilePath $LogFile -Append
}

function Step([string]$title, [scriptblock]$action) {
    Log "`n--- $title ---"
    try {
        $result = & $action
        if ($null -ne $result) {
            Log ($result | ConvertTo-Json -Depth 10)
        } else {
            Log "(no content)"
        }
        Log "exit_code=0"
        return $result
    }
    catch {
        Log "ERROR: $($_.Exception.Message)"
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            Log "DETAIL: $($_.ErrorDetails.Message)"
        }
        Log "exit_code=1"
        return $null
    }
}

# Start log
"" | Out-File $LogFile
Log "=== TradeFlow E2E Smoke Test ==="
Log "API_BASE=$ApiBase"
Log "USER=$Username | EMAIL=$Email"

# 1) Register (ok if already exists)
Step "Register" {
    $body = @{
        username  = $Username
        email     = $Email
        password  = $Password
        firstName = "I"
        lastName  = "Test"
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/register" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
}

# 2) Login
$loginResp = Step "Login" {
    $body = @{
        usernameOrEmail = $Email
        password        = $Password
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/login" -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
}

if ($null -ne $loginResp -and $loginResp.accessToken) {
    $AccessToken = $loginResp.accessToken
    $HeadersAuth = @{ Authorization = "Bearer $AccessToken" }
    Log "ACCESS_TOKEN=$($AccessToken.Substring(0, [Math]::Min(30,$AccessToken.Length)))..."
} else {
    Log "Login failed; skipping auth-protected steps."
}

# 3) Wallet balances
Step "Wallet balances" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/balances" -Headers $HeadersAuth -Method Get -ErrorAction Stop
}

# 4) Wallet faucet
Step "Wallet faucet" {
    Invoke-RestMethod -Uri "$ApiBase/wallet/faucet" -Headers $HeadersAuth -Method Post -ErrorAction Stop
}

# 5) Place order
Step "Place order" {
    $body = $OrderPayload | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/orders" -Headers $HeadersAuth -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
}

# 6) List orders
Step "List orders" {
    Invoke-RestMethod -Uri "$ApiBase/orders" -Headers $HeadersAuth -Method Get -ErrorAction Stop
}

# 7) Open orders
Step "Open orders" {
    Invoke-RestMethod -Uri "$ApiBase/orders/open" -Headers $HeadersAuth -Method Get -ErrorAction Stop
}

# 8) Market ticker (public)
Step "Market ticker BTCUSDT" {
    Invoke-RestMethod -Uri "$ApiBase/market/ticker/BTCUSDT" -Method Get -ErrorAction Stop
}

Log "`n=== Done. Full log in $LogFile ==="
Write-Host "Finished. See $LogFile for details."
