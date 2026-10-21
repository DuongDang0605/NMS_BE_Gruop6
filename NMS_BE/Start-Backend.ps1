param(
    [string]$JdkHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
    throw 'Set DB_PASSWORD to the SQL Server login password before starting the backend.'
}
$sqlLogin = if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) { 'sa' } else { $env:DB_USERNAME }

function Test-Jdk21([string]$Candidate) {
    if (-not $Candidate -or -not (Test-Path -LiteralPath (Join-Path $Candidate 'bin\javac.exe'))) {
        return $false
    }
    $releasePath = Join-Path $Candidate 'release'
    if (-not (Test-Path -LiteralPath $releasePath)) {
        return $false
    }
    $versionLine = Get-Content -LiteralPath $releasePath | Where-Object { $_ -match '^JAVA_VERSION=' }
    return ($versionLine -match '"(\d+)' -and [int]$Matches[1] -ge 21)
}

if (-not (Test-Jdk21 $JdkHome)) {
    $jdkCandidates = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.jdks'),
        'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue
    $JdkHome = $jdkCandidates | Where-Object { Test-Jdk21 $_.FullName } |
        Select-Object -First 1 -ExpandProperty FullName
}

if (-not (Test-Jdk21 $JdkHome)) {
    throw 'JDK 21+ is required. Run .\Start-Backend.ps1 -JdkHome "C:\path\to\jdk-21".'
}

Get-Command sqlcmd -ErrorAction Stop | Out-Null
$previousJavaHome = $env:JAVA_HOME
$previousSqlcmdPassword = $env:SQLCMDPASSWORD
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME = $JdkHome
    $env:SQLCMDPASSWORD = $env:DB_PASSWORD
    Write-Host "Using JDK: $JdkHome"
    & sqlcmd -S 'tcp:localhost,1433' -U $sqlLogin -C -b -l 10 -i (Join-Path $PSScriptRoot 'database\create-database.sql')
    if ($LASTEXITCODE -ne 0) {
        throw 'Database setup failed. Check SQL Server Authentication and SQL login permissions.'
    }
    & sqlcmd -S 'tcp:localhost,1433' -d 'FUNewsManagementSystem' -U $sqlLogin -C -b -l 10 `
        -i (Join-Path $PSScriptRoot 'database\migrations\001-unicode-text-columns.sql')
    if ($LASTEXITCODE -ne 0) {
        throw 'Unicode schema migration failed.'
    }
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Backend exited with code $LASTEXITCODE."
    }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:SQLCMDPASSWORD = $previousSqlcmdPassword
    Pop-Location
}
