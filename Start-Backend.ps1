param(
    [string]$JdkHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'

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
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME = $JdkHome
    Write-Host "Using JDK: $JdkHome"
    & sqlcmd -S 'tcp:localhost,1433' -E -C -b -l 10 -i (Join-Path $PSScriptRoot 'database\create-database.sql')
    if ($LASTEXITCODE -ne 0) {
        throw 'Database setup failed. Check SQL Server and Windows account permissions.'
    }
    & sqlcmd -S 'tcp:localhost,1433' -d 'FUNewsManagementSystem' -E -C -b -l 10 `
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
    Pop-Location
}
