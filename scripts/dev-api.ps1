<# Windows PowerShell 5.1 / PowerShell 7.
   Load private apps/api/.env as data, then run Maven Wrapper.
   Existing process variables win. No user/machine environment is changed. #>
[CmdletBinding()]
param(
    [string] $EnvFile,
    [string[]] $MavenArguments = @('spring-boot:run'),
    [switch] $ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$apiDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) 'apps/api'
$explicitEnvFile = $PSBoundParameters.ContainsKey('EnvFile')
if (-not $explicitEnvFile) {
    $EnvFile = Join-Path $apiDirectory '.env'
}

# Parse the entire file before changing the environment or starting Maven.
# Hashtable keys are case-insensitive, like Windows environment variables.
$values = @{}
if (Test-Path -LiteralPath $EnvFile -PathType Leaf) {
    try {
        $encoding = New-Object System.Text.UTF8Encoding($false, $true)
        $lines = [System.IO.File]::ReadAllLines(
            $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($EnvFile), $encoding)
    } catch {
        throw 'Cannot read env file as UTF-8. Check its path, permissions and encoding.'
    }
    $lineNumber = 0
    foreach ($line in $lines) {
        $lineNumber++
        $entry = $line.Trim()
        if ($entry.Length -eq 0 -or $entry.StartsWith('#')) { continue }
        if ($entry -notmatch '^([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
            throw "Invalid env syntax at line ${lineNumber}. Expected NAME=value."
        }
        $name = $Matches[1]
        $value = $Matches[2].Trim()
        if ($values.ContainsKey($name)) {
            throw "Duplicate env name at line ${lineNumber}."
        }
        if ($value.StartsWith('"') -or $value.StartsWith("'")) {
            $quote = $value.Substring(0, 1)
            if ($value.Length -lt 2 -or -not $value.EndsWith($quote)) {
                throw "Unclosed env quote at line ${lineNumber}. Use a single-line value."
            }
            $value = $value.Substring(1, $value.Length - 2)
        }
        if ($value.IndexOf([char]0) -ge 0) {
            throw "Invalid env character at line ${lineNumber}."
        }
        # No interpolation, escape decoding, inline comments or execution.
        $values[$name] = $value
    }
} elseif ($explicitEnvFile) {
    throw 'The specified env file does not exist or is not a file.'
} elseif (Test-Path -LiteralPath $EnvFile) {
    throw 'The default env path must be a file.'
} else {
    Write-Host 'No apps/api/.env; using the existing process environment.'
}

if ($ValidateOnly) {
    Write-Host 'Env syntax OK. No values displayed, environment changed or Maven started.'
    return
}

$wrapper = Join-Path $apiDirectory 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    throw 'Maven Wrapper not found at apps/api/mvnw.cmd.'
}
$originalValues = @{}
$locationChanged = $false
$exitCode = 1
try {
    foreach ($name in $values.Keys) {
        $existing = [Environment]::GetEnvironmentVariable($name, 'Process')
        # Empty entries in the template mean "not configured".
        if ($null -eq $existing -and $values[$name].Length -gt 0) {
            $originalValues[$name] = $existing
            [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process')
        }
    }
    Push-Location -LiteralPath $apiDirectory
    $locationChanged = $true
    & $wrapper @MavenArguments
    $exitCode = $LASTEXITCODE
} finally {
    if ($locationChanged) { Pop-Location }
    foreach ($name in $originalValues.Keys) {
        [Environment]::SetEnvironmentVariable($name, $originalValues[$name], 'Process')
    }
}
exit $exitCode
