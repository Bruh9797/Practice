[CmdletBinding()]
param(
    [switch] $SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

function Stop-ExistingThermoSelect {
    $targetRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'target'))
    $processes = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object {
            $commandLine = $_.CommandLine
            $commandLine -and
            $commandLine.IndexOf('-jar', [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -and
            ($commandLine.IndexOf($targetRoot, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
             $commandLine.IndexOf('heat-exchanger-selector-0.0.1-SNAPSHOT.jar', [System.StringComparison]::OrdinalIgnoreCase) -ge 0)
        }

    foreach ($process in $processes) {
        Write-Host "Stopping previous ThermoSelect process (PID $($process.ProcessId))..."
        Stop-Process -Id $process.ProcessId -Force
        Wait-Process -Id $process.ProcessId -Timeout 10 -ErrorAction SilentlyContinue
    }
}

Push-Location $projectRoot
try {
    Stop-ExistingThermoSelect

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
