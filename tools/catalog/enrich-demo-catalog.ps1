[CmdletBinding()]
param(
    [string] $CatalogPath = (Join-Path $PSScriptRoot '..\..\Авторизация\src\main\resources\data\demo-catalog.psv'),
    [string] $AuditPath = (Join-Path $PSScriptRoot '..\..\docs\backend\catalog-data-quality.csv')
)

$ErrorActionPreference = 'Stop'
$invariant = [System.Globalization.CultureInfo]::InvariantCulture
$catalog = (Resolve-Path -LiteralPath $CatalogPath).Path
$lines = [System.IO.File]::ReadAllLines($catalog, [System.Text.Encoding]::UTF8)
$headers = $lines[0].Substring(2).Split('|')
$rows = $lines[1..($lines.Length - 1)] | ConvertFrom-Csv -Delimiter '|' -Header $headers

if ($rows.Count -ne 42) {
    throw "Expected 42 catalog records, found $($rows.Count)."
}

function Format-Number([double] $Value) {
    return $Value.ToString('0.###', $invariant)
}

function Set-Official($Row, [string] $Field, $Value) {
    if ([string]::IsNullOrWhiteSpace([string]$Row.$Field)) {
        $Row.$Field = [string]$Value
    }
}

$official = @{
    'Danfoss|B3-012-40-3.0-H (111B6315)' = @{ massKg = '2.36' }
    'Kelvion|NX 25M' = @{ pressureMaxBar = '31' }
    'Kelvion|NX 50M' = @{ pressureMaxBar = '31' }
    'Kelvion|NX 100X' = @{ pressureMaxBar = '31' }
    'Kelvion|NX 150L' = @{ pressureMaxBar = '31' }
    'Kelvion|NX 250L' = @{ pressureMaxBar = '31' }
    'Kelvion|NX 400X' = @{ pressureMaxBar = '31' }
    'SWEP|B5T All-Stainless' = @{ temperatureMaxC = '350' }
    'SWEP|B10TS All-Stainless' = @{ temperatureMaxC = '350' }
    'SWEP|B15T All-Stainless' = @{ temperatureMaxC = '350' }
    'SWEP|B80S All-Stainless' = @{ temperatureMaxC = '350' }
    'Basco|Type 500' = @{ temperatureMaxC = '148.9'; pressureMaxBar = '20.7' }
    'Alfa Laval|SHE LTL 2S' = @{ temperatureMinC = '-40'; temperatureMaxC = '400'; pressureMaxBar = '12'; pressureCurve = '100:12,200:10.5,300:9,400:6' }
    'Alfa Laval|SHE LTL 4L' = @{ temperatureMinC = '-40'; temperatureMaxC = '400'; pressureMaxBar = '10.5'; pressureCurve = '100:10.5,200:10.5,300:9,400:6' }
    'Alfa Laval|SHE LTL 8L' = @{ temperatureMinC = '-40'; temperatureMaxC = '400'; pressureMaxBar = '10.5'; pressureCurve = '100:10.5,200:10.5,300:9,400:6' }
    'Alfa Laval|SHE LTL 30L' = @{ temperatureMinC = '-40'; temperatureMaxC = '400'; pressureMaxBar = '10.5'; pressureCurve = '100:10.5,200:10,300:8.5,400:6' }
    'Alfa Laval|SHE Cond 1S' = @{ temperatureMinC = '-100'; temperatureMaxC = '400'; pressureMaxBar = '7.5'; pressureCurve = '150:7.5,250:6.5,300:6,400:5.5' }
    'Alfa Laval|SHE Cond 14L' = @{ temperatureMinC = '-100'; temperatureMaxC = '400'; pressureMaxBar = '7'; pressureCurve = '150:7,250:6,300:5.5,400:5' }
}

$areaByModel = @{
    'B3-012-40-3.0-H (111B6315)' = 1.05; 'B3-113-78-4.5-HDQ (111B6015)' = 8.2
    'B3-113-110-3.0-HDQ (111B0321)' = 11.6; 'B3-113-90-4.5-HDQ (111B0320)' = 9.4
    'B3-095B-101 (111B0103)' = 7.8; 'B3-027-14 (111B1131)' = 0.45
    'NX 25M' = 3.2; 'NX 50M' = 6.5; 'NX 100X' = 12; 'NX 150L' = 18; 'NX 250L' = 28; 'NX 400X' = 42
    'B5T All-Stainless' = 0.35; 'B10TS All-Stainless' = 0.8; 'B15T All-Stainless' = 1.4; 'B80S All-Stainless' = 6
    'Type 500' = 12; 'BW' = 18; 'BWS' = 22; 'Type OP' = 30; 'Type HT' = 8; 'Type AHT' = 10
    'LF Drycooler' = 120; 'LF-S Drycooler' = 40; 'RF-S Condenser' = 60; 'GF-S Gas Cooler' = 55; 'LV-M Drycooler' = 80; 'RV-T Condenser' = 180
}

$airDimensions = @{
    'LF Drycooler' = @(2400, 1500, 6200, 1450); 'LF-S Drycooler' = @(1200, 950, 2200, 280)
    'RF-S Condenser' = @(1600, 1100, 3100, 420); 'GF-S Gas Cooler' = @(1600, 1100, 3100, 440)
    'LV-M Drycooler' = @(2200, 2300, 3600, 780); 'RV-T Condenser' = @(2400, 2500, 7200, 1800)
}

$shellDimensions = @{
    'Type 500' = @(320, 520, 1800, 320); 'BW' = @(400, 650, 2200, 480)
    'BWS' = @(450, 700, 2400, 560); 'Type OP' = @(600, 900, 3000, 980)
    'Type HT' = @(260, 420, 1200, 180); 'Type AHT' = @(280, 450, 1400, 230)
}

$numericFields = @(
    'surfaceAreaM2', 'flowMinM3h', 'flowMaxM3h', 'powerMinKw', 'powerMaxKw',
    'temperatureMinC', 'temperatureMaxC', 'pressureMinBar', 'pressureMaxBar',
    'widthMm', 'heightMm', 'depthMm', 'massKg'
)

$audit = @()
$familyCounters = @{}
foreach ($row in $rows) {
    $existingMockFields = @()
    if ($row.facts -match '(?:^|;)mockFields=([^;]*)') {
        $existingMockFields = @($Matches[1].Split(',', [System.StringSplitOptions]::RemoveEmptyEntries))
    }
    $row.measurementBasis = ($row.measurementBasis -replace '\s*\[DEMO\].*$', '').Trim()
    $row.facts = (($row.facts.Split(';') | Where-Object {
        $_ -notmatch '^(dataOrigin|mockFields|mockMethod)='
    }) -join ';')

    $key = "$($row.manufacturer)|$($row.model)"
    if ($official.ContainsKey($key)) {
        foreach ($entry in $official[$key].GetEnumerator()) {
            Set-Official $row $entry.Key $entry.Value
        }
    }

    $mockFields = [System.Collections.Generic.List[string]]::new()
    foreach ($field in $existingMockFields) {
        if (-not $mockFields.Contains($field)) {
            $mockFields.Add($field)
        }
    }
    $familyCounters[$row.family] = 1 + [int]($familyCounters[$row.family])
    $familyIndex = [int]$familyCounters[$row.family]

    if ([string]::IsNullOrWhiteSpace($row.surfaceAreaM2)) {
        $area = if ($areaByModel.ContainsKey($row.model)) { [double]$areaByModel[$row.model] } else { 0.5 + $familyIndex }
        $row.surfaceAreaM2 = Format-Number $area
        $mockFields.Add('surfaceAreaM2')
    }
    $area = [double]::Parse($row.surfaceAreaM2, $invariant)

    $defaults = switch ($row.family) {
        'PLATE' {
            $maxPressure = if ($row.manufacturer -eq 'SWEP') { 43 } elseif ($row.manufacturer -eq 'Kelvion') { 31 } elseif ($row.model -match '^T6') { 16 } else { 10 }
            $minTemp = if ($row.manufacturer -eq 'SWEP') { -40 } elseif ($row.manufacturer -eq 'Kelvion') { -40 } else { -20 }
            $maxTemp = if ($row.manufacturer -eq 'SWEP') { 350 } elseif ($row.manufacturer -eq 'Danfoss') { 200 } else { 180 }
            @{
                flowMinM3h = [math]::Max(0.2, $area * 0.6); flowMaxM3h = [math]::Max(2, $area * 10 + 1)
                powerMinKw = [math]::Max(5, $area * 12); powerMaxKw = [math]::Max(30, $area * 90)
                temperatureMinC = $minTemp; temperatureMaxC = $maxTemp; pressureMinBar = 0; pressureMaxBar = $maxPressure
                widthMm = 140 + [math]::Sqrt($area) * 45; heightMm = 300 + [math]::Sqrt($area) * 140
                depthMm = 65 + $area * 18; massKg = 5 + $area * 10
            }
        }
        'SHELL_AND_TUBE' {
            $dims = $shellDimensions[$row.model]
            @{
                flowMinM3h = [math]::Max(2, $area * 0.4); flowMaxM3h = $area * 7
                powerMinKw = $area * 10; powerMaxKw = $area * 80
                temperatureMinC = -20; temperatureMaxC = 150; pressureMinBar = 0; pressureMaxBar = 16
                widthMm = $dims[0]; heightMm = $dims[1]; depthMm = $dims[2]; massKg = $dims[3]
            }
        }
        'AIR_COOLED' {
            $dims = $airDimensions[$row.model]
            $pmin = if ([string]::IsNullOrWhiteSpace($row.powerMinKw)) { 10 } else { [double]$row.powerMinKw }
            $pmax = if ([string]::IsNullOrWhiteSpace($row.powerMaxKw)) { 100 } else { [double]$row.powerMaxKw }
            @{
                flowMinM3h = [math]::Max(1, $pmin / 18); flowMaxM3h = [math]::Max(8, $pmax / 8)
                powerMinKw = $pmin; powerMaxKw = $pmax
                temperatureMinC = -40; temperatureMaxC = 120; pressureMinBar = 0; pressureMaxBar = 16
                widthMm = $dims[0]; heightMm = $dims[1]; depthMm = $dims[2]; massKg = $dims[3]
            }
        }
        'SPIRAL' {
            @{
                flowMinM3h = [math]::Max(1, $area * 0.5); flowMaxM3h = [math]::Max(8, $area * 5)
                powerMinKw = [math]::Max(15, $area * 8); powerMaxKw = [math]::Max(80, $area * 65)
                temperatureMinC = -40; temperatureMaxC = 400; pressureMinBar = 0; pressureMaxBar = 10
                widthMm = 350 + [math]::Sqrt($area) * 90; heightMm = 500 + [math]::Sqrt($area) * 150
                depthMm = 220 + [math]::Sqrt($area) * 80; massKg = 90 + $area * 42
            }
        }
    }

    foreach ($field in $numericFields) {
        if ([string]::IsNullOrWhiteSpace([string]$row.$field)) {
            $row.$field = Format-Number ([double]$defaults[$field])
            $mockFields.Add($field)
        }
    }

    if ([string]::IsNullOrWhiteSpace($row.pressureCurve)) {
        $minTemp = Format-Number ([double]$row.temperatureMinC)
        $maxTemp = Format-Number ([double]$row.temperatureMaxC)
        $maxPressure = [double]$row.pressureMaxBar
        $reducedPressure = Format-Number ([math]::Max(0, $maxPressure * 0.7))
        $row.pressureCurve = "$minTemp`:$($row.pressureMaxBar),$maxTemp`:$reducedPressure"
        $mockFields.Add('pressureCurve')
    }

    $mockList = [string]::Join(',', $mockFields)
    $row.measurementBasis = "$($row.measurementBasis) [DEMO] Fields generated for interface and filter testing: $mockList. Not for thermal sizing."
    $extraFacts = "dataOrigin=OFFICIAL+MOCK;mockFields=$mockList;mockMethod=Deterministic representative values by exchanger family"
    $row.facts = if ([string]::IsNullOrWhiteSpace($row.facts)) { $extraFacts } else { "$($row.facts);$extraFacts" }

    $audit += [pscustomobject]@{
        slug = $row.slug
        manufacturer = $row.manufacturer
        model = $row.model
        family = $row.family
        granularity = $row.granularity
        completenessPercent = 100
        mockFields = $mockList
        sourceUrl = $row.sourceUrl
    }
}

foreach ($row in $rows) {
    foreach ($field in $numericFields + 'pressureCurve') {
        if ([string]::IsNullOrWhiteSpace([string]$row.$field)) {
            throw "Field $field is empty for $($row.slug)."
        }
    }
}

$output = [System.Collections.Generic.List[string]]::new()
$output.Add('# ' + ($headers -join '|'))
foreach ($row in $rows) {
    $values = foreach ($header in $headers) {
        ([string]$row.$header).Replace('|', '/')
    }
    $output.Add($values -join '|')
}
[System.IO.File]::WriteAllLines($catalog, $output, [System.Text.UTF8Encoding]::new($false))

$auditDirectory = Split-Path -Parent $AuditPath
New-Item -ItemType Directory -Force -Path $auditDirectory | Out-Null
$audit | Export-Csv -LiteralPath $AuditPath -NoTypeInformation -Encoding UTF8

Write-Host "Enriched $($rows.Count) records. Audit: $AuditPath"
