function CallStep([string]$title, [scriptblock]$action) {
    Log "`n--- $title ---"
    try {
        # Capture response AND status code (PowerShell 7+)
        $result = & $action
        if ($result) { Log ($result | ConvertTo-Json -Depth 5 -Compress) }
        Log "exit_code=0"
        return $true
    } catch {
        # Show detailed API error response
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorBody = ""
        if ($_.ErrorDetails.Message) {
            $errorBody = $_.ErrorDetails.Message
        }
        Log "ERROR: HTTP $statusCode"
        Log "Message: $($_.Exception.Message)"
        if ($errorBody) { Log "API Response: $errorBody" }
        Log "exit_code=1"
        return $false
    }
}

# Register - don't suppress errors, just handle them
$registerSuccess = CallStep "Register" {
    $body = @{
        username  = $Username
        email     = $Email
        password  = $Password
        firstName = "I"
        lastName  = "Test"
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$ApiBase/auth/register" -Method Post `
        -ContentType "application/json" -Body $body -ErrorAction Stop
}

if (-not $registerSuccess) {
    Log "Registration failed (user may already exist), continuing..."
}
