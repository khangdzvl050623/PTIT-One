<# Bootstrap/inspection only; Windows Authentication; no migrations or seed.
   From repo root: .\db\central\run.ps1 -Action CreateDatabase -WhatIf
   Config is data, not executable PowerShell. No passwords are stored here. #>
[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('CreateDatabase', 'VerifyDatabase')]
    [string] $Action,
    [string] $ConfigPath = (Join-Path $PSScriptRoot 'config.local.psd1')
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if (-not (Test-Path -LiteralPath $ConfigPath -PathType Leaf)) {
    throw 'Missing config. Copy db/central/config.example.psd1 to config.local.psd1 and edit it.'
}
$config = Import-PowerShellDataFile -LiteralPath $ConfigPath
foreach ($key in @('SqlServer', 'DatabaseName', 'TrustServerCertificate')) {
    if (-not $config.ContainsKey($key)) { throw "Missing config key: $key" }
}
$server = $config.SqlServer
$database = $config.DatabaseName
if ($server -isnot [string] -or [string]::IsNullOrWhiteSpace($server) -or
    $server -match '[\r\n\x00"]' -or $server.StartsWith('-')) {
    throw 'SqlServer must be a non-empty SQL Server address.'
}
if ($database -isnot [string] -or $database.Length -gt 128 -or
    $database -cnotmatch '^PTITONE_CENTRAL(?:_[A-Z0-9_]+)?$') {
    throw 'DatabaseName must be PTITONE_CENTRAL or PTITONE_CENTRAL_<UPPERCASE_SUFFIX>.'
}
if ($config.TrustServerCertificate -isnot [bool]) {
    throw 'TrustServerCertificate must be $true or $false.'
}

if ($Action -eq 'CreateDatabase') {
    $targetDatabase = 'master'
    $scriptPath = Join-Path $PSScriptRoot '00-create-database.sql'
} else {
    $targetDatabase = $database
    $scriptPath = Join-Path $PSScriptRoot 'tests/00-verify-database.sql'
}
$sqlArguments = @('-S', $server, '-d', $targetDatabase, '-E', '-N',
    '-b', '-r', '1', '-l', '15', '-t', '60', '-f', '65001')
$certificateOption = ''
if ($config.TrustServerCertificate) {
    $sqlArguments += '-C'
    $certificateOption = ' -C'
}
$sqlArguments += @('-v', "CentralDatabase=$database", '-i', $scriptPath)
Write-Host "sqlcmd -S `"$server`" -d $targetDatabase -E -N -b -r 1 -l 15 -t 60 -f 65001$certificateOption -v CentralDatabase=`"$database`" -i `"$scriptPath`""
if (-not $PSCmdlet.ShouldProcess("$server / $database", $Action)) { return }

$sqlcmd = Get-Command sqlcmd -CommandType Application -ErrorAction Stop
& $sqlcmd.Source @sqlArguments
if ($LASTEXITCODE -ne 0) {
    throw "sqlcmd failed (exit $LASTEXITCODE). Check the error above; setup is not complete."
}
