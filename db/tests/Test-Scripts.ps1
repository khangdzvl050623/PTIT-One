<# Kiem tra offline: SQLCMD variables, database runner va cu phap T-SQL.
   Khong ket noi SQL Server. ScriptDom lay tu ban SSMS da cai san.
   Chay: powershell -NoProfile -File db/tests/Test-Scripts.ps1
   Co the truyen -ScriptDomPath neu SSMS nam o duong dan khac. #>
[CmdletBinding()]
param(
    [string] $ScriptDomPath = 'C:\Program Files\Microsoft SQL Server Management Studio 22\Release\Common7\IDE\Extensions\Application\Microsoft.SqlServer.TransactSql.ScriptDom.dll'
)
$ErrorActionPreference = 'Stop'
$dbRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath $ScriptDomPath)) {
    throw 'Khong tim thay ScriptDom. Truyen -ScriptDomPath toi DLL cua SSMS/DacFx.'
}
Add-Type -Path $ScriptDomPath
$parser = New-Object Microsoft.SqlServer.TransactSql.ScriptDom.TSql160Parser($true)
# MOI script .sql deu phai co mat o day. Bo sot mot file la mat luoi chan:
# bon script master/ va 30-distributor.sql tung khong bien dich duoc suot
# nhieu commit vi RAISERROR nhan CAST()/DB_NAME() lam tham so — chi hang va
# bien moi hop le. Khong ai phat hien vi chung chua nam trong danh sach nay.
# Db: chi dat khi KHAC $cfg.Databases[$site] (bootstrap chay trong master).
$scripts = @(
    @{ Path = '00-create-databases.sql'; Sites = @('MASTER','HCM','HN','DN'); Db = 'master' }
    @{ Path = 'master/01-schema-thamchieu.sql'; Sites = @('MASTER') }
    @{ Path = 'master/02-danhba-nguoidung.sql'; Sites = @('MASTER') }
    @{ Path = 'master/03-taikhoan-master.sql'; Sites = @('MASTER') }
    @{ Path = 'master/04-seed-danhmuc.sql'; Sites = @('MASTER') }
    @{ Path = 'replication/30-distributor.sql'; Sites = @('MASTER') }
    @{ Path = 'replication/31-publication.sql'; Sites = @('MASTER') }
    @{ Path = 'replication/32-subscription.sql'; Sites = @('MASTER') }
    @{ Path = 'replication/39-go-subscription.sql'; Sites = @('MASTER') }
    @{ Path = 'replication/39-go-subscriber.sql'; Sites = @('HCM','HN','DN') }
    @{ Path = 'replication/39-go-publication.sql'; Sites = @('MASTER') }
    @{ Path = 'replication/39-go-distributor.sql'; Sites = @('MASTER') }
    @{ Path = 'site/10-schema-vanhanh.sql'; Sites = @('HCM','HN','DN') }
    @{ Path = 'site/11-rangbuoc.sql'; Sites = @('HCM','HN','DN') }
    @{ Path = 'site/12-chimuc.sql'; Sites = @('HCM','HN','DN') }
    # Demo doc trong SSMS, khong phai buoc cai dat. Van phai bien dich duoc.
    # File nay CO CHU Y ghi thang ten database thay vi dung bien SQLCMD:
    # SSMS khong giu :setvar giua hai lan F5, con qua sqlcmd thi :setvar de
    # len -v — nen bien SQLCMD o day khong doi duoc dich, chi them rac roi.
    @{ Path = 'tests/90-demo-nhan-ban.sql'; Sites = @('HCM') }
)

# Khong file .sql nao duoc nam ngoai danh sach tren.
$declared = $scripts.Path | ForEach-Object { $_ -replace '/', '\' }
$onDisk = Get-ChildItem -LiteralPath $dbRoot -Recurse -Filter *.sql |
    ForEach-Object { $_.FullName.Substring($dbRoot.Length + 1) }
$missing = $onDisk | Where-Object { $_ -notin $declared }
if ($missing) { throw "Script chua duoc kiem tra: $($missing -join ', ')" }
$cfg = Import-PowerShellDataFile (Join-Path $dbRoot 'config.ps1')
$caseCount = 0
foreach ($script in $scripts) {
    $source = Get-Content -LiteralPath (Join-Path $dbRoot $script.Path) -Raw -Encoding UTF8
    foreach ($site in $script.Sites) {
        $preview = (& (Join-Path $dbRoot 'run.ps1') -Script $script.Path -On $site -WhatIf 6>&1 | Out-String -Width 32767)
        $expectDb = if ($script.Db) { $script.Db } else { $cfg.Databases[$site] }
        if ($preview -notmatch ('-d ' + [regex]::Escape($expectDb) + '(?:\s|$)')) {
            throw "Runner khong chon dung DB: $($script.Path) / $site"
        }
        $variables = @{}
        foreach ($match in [regex]::Matches($preview, '-v ([A-Za-z][A-Za-z0-9]*)=([^\r\n]*?)(?= -v |\r?\n|$)')) {
            $value = $match.Groups[2].Value.TrimEnd()
            if (-not ($value.StartsWith('"') -and $value.EndsWith('"'))) {
                throw "Bien SQLCMD phai co dau nhay (dau phay/khoang trang): $($match.Groups[1].Value)"
            }
            $variables[$match.Groups[1].Value] = $value.Substring(1, $value.Length - 2).Replace('""', '"')
        }
        $resolved = [regex]::Replace($source, '\$\(([A-Za-z][A-Za-z0-9]*)\)', {
            param($match)
            $key = $match.Groups[1].Value
            if (-not $variables.ContainsKey($key)) { throw "Runner thieu bien SQLCMD: $key" }
            return $variables[$key]
        })
        # SQLCMD directive khong phai T-SQL; giu dong trong de line number dung.
        # :setvar dat gia tri mac dinh cho nguoi mo file thang trong SSMS;
        # qua run.ps1 thi -v tren dong lenh moi la gia tri thang.
        $resolved = [regex]::Replace($resolved, '(?m)^:(?:ON ERROR EXIT|setvar\s.*?)\r?$', '')
        $parseErrors = $null
        $reader = New-Object IO.StringReader($resolved)
        $null = $parser.Parse($reader, [ref] $parseErrors)
        $reader.Dispose()
        if ($parseErrors.Count -gt 0) {
            $details = $parseErrors | ForEach-Object { "line $($_.Line): $($_.Message)" }
            throw "$($script.Path) / ${site}: $($details -join '; ')"
        }
        $caseCount++
    }
    Write-Output "PASS syntax + SQLCMD + DB: $($script.Path)"
}
# Regression: tao database khong duoc ket noi vao DB chua ton tai.
foreach ($site in @('MASTER','HCM','HN','DN')) {
    $preview = (& (Join-Path $dbRoot 'run.ps1') -Script '00-create-databases.sql' -On $site -WhatIf 6>&1 | Out-String -Width 32767)
    if ($preview -notmatch '-d master(?:\s|$)') { throw "Bootstrap sai database: $site" }
}
# Regression: master/ khong duoc roi ve default DB cua login.
$preview = (& (Join-Path $dbRoot 'run.ps1') -Script 'master/01-schema-thamchieu.sql' -On MASTER -WhatIf 6>&1 | Out-String -Width 32767)
if ($preview -notmatch ('-d ' + [regex]::Escape($cfg.Databases.MASTER) + '(?:\s|$)')) {
    throw 'Runner master/ sai database.'
}
Write-Output "PASS: $caseCount SQL/site cases + 5 runner regression cases. CHUA kiem chung SQL Server runtime."
