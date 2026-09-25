# Offline integration tests: fake Maven Wrapper, synthetic values only.
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('ptit-one-env-' + [Guid]::NewGuid().ToString('N'))
$fixture = Join-Path $testRoot 'project with spaces'
$api = Join-Path $fixture 'apps/api'
$launcher = Join-Path $fixture 'scripts/dev-api.ps1'
$envFile = Join-Path $api '.env'
$probeFile = Join-Path $api 'probe.json'
$utf8 = New-Object Text.UTF8Encoding($false)
$names = @('PTITONE_ENV_TEST_VALUE', 'PTITONE_ENV_TEST_EXISTING', 'PTITONE_ENV_TEST_BLANK', 'PTITONE_ENV_TEST_EXIT')
$saved = @{}
$startingLocation = (Get-Location).Path
$cases = 0

function Assert-True([bool] $Condition, [string] $Message) {
    if (-not $Condition) { throw $Message }
}
function Write-Env([string] $Content) {
    [IO.File]::WriteAllText($envFile, $Content, $utf8)
    if (Test-Path -LiteralPath $probeFile) { Remove-Item -LiteralPath $probeFile }
}
function Read-Probe {
    Get-Content -LiteralPath $probeFile -Raw -Encoding UTF8 | ConvertFrom-Json
}
function Assert-Rejected([string] $Content, [string] $ExpectedError) {
    Write-Env $Content
    $message = ''
    try { & $launcher | Out-Null } catch { $message = $_.Exception.Message }
    Assert-True ($message -like $ExpectedError) 'Expected a sanitized validation error.'
    Assert-True (-not (Test-Path -LiteralPath $probeFile)) 'Invalid file started Maven.'
    Assert-True ($null -eq [Environment]::GetEnvironmentVariable($names[0], 'Process')) 'Invalid file changed environment.'
    $script:cases++
}

try {
    foreach ($name in $names) {
        $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    $null = New-Item -ItemType Directory -Path $api, (Split-Path -Parent $launcher) -Force
    Copy-Item -LiteralPath (Join-Path (Split-Path -Parent $PSScriptRoot) 'dev-api.ps1') -Destination $launcher
    [IO.File]::WriteAllText((Join-Path $api 'mvnw.cmd'), @'
@echo off
powershell.exe -NoProfile -File "%~dp0probe.ps1" %*
exit /b %errorlevel%
'@, $utf8)
    [IO.File]::WriteAllText((Join-Path $api 'probe.ps1'), @'
@{
    Value = $env:PTITONE_ENV_TEST_VALUE
    Existing = $env:PTITONE_ENV_TEST_EXISTING
    Blank = $env:PTITONE_ENV_TEST_BLANK
    Arguments = @($args)
    Directory = (Get-Location).Path
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'probe.json') -Encoding UTF8
if ($env:PTITONE_ENV_TEST_EXIT) { exit ([int] $env:PTITONE_ENV_TEST_EXIT) }
exit 0
'@, $utf8)
    # Launch from outside the project; missing default file is optional.
    Push-Location -LiteralPath $testRoot
    & $launcher
    $probe = Read-Probe
    Assert-True ($probe.Directory -eq $api) 'Wrong Maven working directory.'
    Assert-True ($probe.Arguments[0] -eq 'spring-boot:run') 'Wrong default Maven goal.'
    Assert-True ((Get-Location).Path -eq $testRoot) 'Caller location was not restored.'
    $cases++

    # BOM, CRLF, UTF-8, whitespace, quotes and shell metacharacters stay literal.
    $literal = '  fake=$env:PATH;$(throw "must-not-run");#&|<>%PATH%`\n=C:\test;Ti' + [char]0x1EBF + 'ng Vi' + [char]0x1EC7 + 't  '
    $content = "# comment`r`n`r`nPTITONE_ENV_TEST_VALUE='" + $literal + "'`r`nPTITONE_ENV_TEST_EXISTING=file`r`nPTITONE_ENV_TEST_BLANK=`r`n"
    Write-Env $content
    [IO.File]::WriteAllText($envFile, $content, (New-Object Text.UTF8Encoding($true)))
    [Environment]::SetEnvironmentVariable($names[1], 'process', 'Process')
    $output = (& $launcher -MavenArguments @('clean', 'verify') 6>&1 | Out-String)
    $probe = Read-Probe
    Assert-True ($probe.Value -ceq $literal) 'Literal env value was changed or evaluated.'
    Assert-True ($probe.Existing -eq 'process') 'Process environment must win.'
    Assert-True ([string]::IsNullOrEmpty($probe.Blank)) 'Blank template entry was applied.'
    Assert-True (($probe.Arguments -join ',') -eq 'clean,verify') 'Maven arguments were lost.'
    Assert-True (-not $output.Contains('fake=')) 'Loader printed a secret value.'
    Assert-True ($null -eq [Environment]::GetEnvironmentVariable($names[0], 'Process')) 'Loaded value leaked into caller.'
    Assert-True ([Environment]::GetEnvironmentVariable($names[1], 'Process') -eq 'process') 'Caller variable changed.'
    $cases++

    Write-Env 'PTITONE_ENV_TEST_VALUE="literal$variable#;=end"'
    & $launcher
    Assert-True ((Read-Probe).Value -ceq 'literal$variable#;=end') 'Double quotes did not preserve literal value.'
    $cases++

    Write-Env 'PTITONE_ENV_TEST_VALUE=jdbc:sqlserver://localhost:1433;databaseName=TEST;encrypt=true#literal'
    & $launcher
    Assert-True ((Read-Probe).Value -ceq 'jdbc:sqlserver://localhost:1433;databaseName=TEST;encrypt=true#literal') 'Unquoted URL was truncated.'
    $cases++

    Write-Env "PTITONE_ENV_TEST_VALUE=temporary`nPTITONE_ENV_TEST_EXIT=7"
    & $launcher
    Assert-True ($LASTEXITCODE -eq 7) 'Maven failure exit code was lost.'
    Assert-True ($null -eq [Environment]::GetEnvironmentVariable($names[0], 'Process')) 'Failure did not restore environment.'
    Assert-True ((Get-Location).Path -eq $testRoot) 'Failure did not restore location.'
    $cases++

    Write-Env 'PTITONE_ENV_TEST_VALUE=validate-only'
    & $launcher -ValidateOnly
    Assert-True (-not (Test-Path -LiteralPath $probeFile)) 'ValidateOnly started Maven.'
    Assert-True ($null -eq [Environment]::GetEnvironmentVariable($names[0], 'Process')) 'ValidateOnly changed environment.'
    $cases++

    & $launcher -EnvFile (Join-Path 'project with spaces' 'apps/api/.env')
    Assert-True ((Read-Probe).Value -eq 'validate-only') 'Explicit relative env path failed.'
    $cases++

    Assert-Rejected "PTITONE_ENV_TEST_VALUE=temporary`nsecret-invalid-line" 'Invalid env syntax at line 2.*'
    Assert-Rejected "PTITONE_ENV_TEST_VALUE=first`nptitone_env_test_value=second" 'Duplicate env name at line 2.*'
    Assert-Rejected 'PTITONE_ENV_TEST_VALUE="unclosed-secret' 'Unclosed env quote at line 1.*'
    Assert-Rejected 'export PTITONE_ENV_TEST_VALUE=secret' 'Invalid env syntax at line 1.*'
    Assert-Rejected "PTITONE_ENV_TEST_VALUE=bad$([char]0)value" 'Invalid env character at line 1.*'

    Write-Env ''
    $message = ''
    try { & $launcher -EnvFile (Join-Path $testRoot 'missing.env') } catch { $message = $_.Exception.Message }
    Assert-True ($message -eq 'The specified env file does not exist or is not a file.') 'Missing explicit file was accepted.'
    Assert-True (-not (Test-Path -LiteralPath $probeFile)) 'Missing explicit file started Maven.'
    $cases++
    Pop-Location
    Write-Output "PASS: $cases env loader integration cases (no Maven/JDK/DB required)."
} finally {
    Set-Location -LiteralPath $startingLocation
    foreach ($name in $saved.Keys) {
        [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process')
    }
    # Only remove the unique fixture under the OS temp directory.
    $resolvedRoot = [IO.Path]::GetFullPath($testRoot)
    $tempPrefix = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if (-not $resolvedRoot.StartsWith($tempPrefix, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolvedRoot) -notlike 'ptit-one-env-*') {
        throw 'Refusing cleanup outside the test temp directory.'
    }
    if (Test-Path -LiteralPath $resolvedRoot) { Remove-Item -LiteralPath $resolvedRoot -Recurse -Force }
}
