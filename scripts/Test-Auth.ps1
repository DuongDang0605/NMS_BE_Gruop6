#requires -Version 7.0
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$AdminEmail = 'admin@fu.edu.vn',
    [Parameter(Mandatory = $true)][string]$AdminPassword
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$temporaryId = $null
$adminHeaders = $null
$checks = [System.Collections.Generic.List[string]]::new()

function Send-Api {
    param([string]$Method, [string]$Path, [hashtable]$Headers = @{}, $Body = $null)
    $request = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        Headers = $Headers
        SkipHttpErrorCheck = $true
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = $Body | ConvertTo-Json -Compress
    }
    Invoke-WebRequest @request
}

function Confirm-Status {
    param($Response, [int]$Expected, [string]$Label)
    if ([int]$Response.StatusCode -ne $Expected) {
        throw "$Label expected HTTP $Expected, got $($Response.StatusCode)"
    }
    $checks.Add($Label)
}

try {
    $response = Send-Api GET '/api/auth/me'
    Confirm-Status $response 401 'Anonymous me rejected'
    $errorBody = $response.Content | ConvertFrom-Json
    if ($errorBody.status -ne 401 -or $errorBody.path -ne '/api/auth/me') { throw 'Invalid error schema' }

    $response = Send-Api POST '/api/auth/login' -Body @{
        accountEmail = "missing-$([guid]::NewGuid().ToString('N'))@example.com"
        accountPassword = 'Invalid@123'
    }
    Confirm-Status $response 401 'Unknown login rejected'

    $response = Send-Api POST '/api/auth/login' -Body @{ accountEmail = $AdminEmail; accountPassword = $AdminPassword }
    Confirm-Status $response 200 'Admin login'
    $admin = $response.Content | ConvertFrom-Json
    if ($admin.roleName -ne 'ADMIN' -or $admin.tokenType -ne 'Bearer') { throw 'Expected Admin Bearer login' }
    $adminHeaders = @{ Authorization = "Bearer $($admin.token)" }
    $response = Send-Api GET '/api/auth/me' $adminHeaders
    Confirm-Status $response 200 'Admin me'
    $response = Send-Api GET '/api/users' $adminHeaders
    Confirm-Status $response 200 'Admin can list users'

    $staffEmail = "smoke-$([guid]::NewGuid().ToString('N'))@example.com"
    $staffPassword = "Temporary@$([guid]::NewGuid().ToString('N'))"
    $staffBody = @{ accountName = 'Temporary security test'; accountEmail = $staffEmail; accountRole = 2; accountPassword = $staffPassword }
    $response = Send-Api POST '/api/users' $adminHeaders $staffBody
    Confirm-Status $response 201 'Admin creates Staff'
    $temporaryId = ($response.Content | ConvertFrom-Json).accountId
    if (-not $temporaryId) { throw 'Missing created user ID' }
    $response = Send-Api POST '/api/auth/login' -Body @{ accountEmail = $staffEmail; accountPassword = $staffPassword }
    Confirm-Status $response 200 'Staff login'
    $staff = $response.Content | ConvertFrom-Json
    if ($staff.roleName -ne 'STAFF') { throw 'Expected Staff role' }
    $staffHeaders = @{ Authorization = "Bearer $($staff.token)" }
    $response = Send-Api GET '/api/auth/me' $staffHeaders
    Confirm-Status $response 200 'Staff me'
    foreach ($method in @('GET', 'POST')) {
        $response = Send-Api $method '/api/users' $staffHeaders
        Confirm-Status $response 403 "Staff $method users denied"
    }
    foreach ($method in @('PUT', 'DELETE')) {
        $response = Send-Api $method "/api/users/$temporaryId" $staffHeaders
        Confirm-Status $response 403 "Staff $method user denied"
    }
    $response = Send-Api GET '/api/auth/me' @{ Authorization = 'Bearer invalid-token' }
    Confirm-Status $response 401 'Invalid token rejected'

    $update = @{ accountName = 'Updated temporary test'; accountEmail = $staffEmail; accountRole = 2 }
    $response = Send-Api PUT "/api/users/$temporaryId" $adminHeaders $update
    Confirm-Status $response 200 'Update without changing password'
    $response = Send-Api POST '/api/auth/login' -Body @{ accountEmail = $staffEmail; accountPassword = $staffPassword }
    Confirm-Status $response 200 'Original password retained'
    $newPassword = "Changed@$([guid]::NewGuid().ToString('N'))"
    $update.accountPassword = $newPassword
    $response = Send-Api PUT "/api/users/$temporaryId" $adminHeaders $update
    Confirm-Status $response 200 'Password change'
    $response = Send-Api GET '/api/auth/me' $staffHeaders
    Confirm-Status $response 401 'Previous token invalidated'
    $response = Send-Api POST '/api/auth/login' -Body @{ accountEmail = $staffEmail; accountPassword = $newPassword }
    Confirm-Status $response 200 'New password login'

    $response = Send-Api OPTIONS '/api/users' @{
        Origin = 'http://localhost:5173'
        'Access-Control-Request-Method' = 'POST'
        'Access-Control-Request-Headers' = 'authorization,content-type'
    }
    Confirm-Status $response 200 'CORS preflight'
    if ($response.Headers['Access-Control-Allow-Origin'] -ne 'http://localhost:5173') { throw 'Missing allowed CORS origin' }
} finally {
    if ($temporaryId -and $adminHeaders) {
        $response = Send-Api DELETE "/api/users/$temporaryId" $adminHeaders
        if ([int]$response.StatusCode -notin @(204, 404)) { throw 'Could not remove temporary security test account' }
    }
}

Write-Output "PASS: $($checks.Count) HTTP authentication/authorization checks; temporary account removed."
