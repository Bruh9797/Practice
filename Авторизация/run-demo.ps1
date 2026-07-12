[CmdletBinding()]
param(
    [switch] $SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

Push-Location $projectRoot
try {
    if (-not $SkipBuild) {
        & "$projectRoot\mvnw.cmd" clean verify
        if ($LASTEXITCODE -ne 0) {
            throw "ThermoSelect build failed with exit code $LASTEXITCODE."
        }
    }

    $jar = Get-ChildItem -LiteralPath "$projectRoot\target" -Filter '*.jar' |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw 'Executable JAR was not found. Run this script without -SkipBuild.'
    }

    Write-Host "ThermoSelect: http://localhost:8080"
    Write-Host "User: demo / demo12345"
    Write-Host "Administrator: admin / admin12345"
    & java -jar $jar.FullName --spring.profiles.active=demo
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
