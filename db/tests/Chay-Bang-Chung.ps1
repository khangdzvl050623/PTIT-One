<# Chay ca nam yeu cau bat buoc, luu output va in mot bang tong ket.
   Chay: powershell -NoProfile -File db/tests/Chay-Bang-Chung.ps1

   Moi muc tu khang dinh (in PASS/FAIL cua chinh no). Script nay chi goi,
   thu output va doi chieu dau hieu PASS — khong tu phan xet thay.

   Output mac dinh vao bang-chung/<ngay-gio>/ (da gitignore, nam tren o D). #>
[CmdletBinding()]
param(
    [string] $OutDir,
    [int[]]  $Chi
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$dbRoot   = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $dbRoot
if (-not $OutDir) {
    $OutDir = Join-Path $repoRoot ('bang-chung/' + (Get-Date -Format 'yyyyMMdd-HHmm'))
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$muc = @(
    @{ So = 1; Ten = 'Phan manh ngang';   Dau = 'PHAN MANH NGANG: PASS'
       Sql = 'migrate\51-doi-soat.sql' }
    @{ So = 2; Ten = 'Nhan ban mot chieu'; Dau = 'NHAN BAN: PASS'
       Sql = 'tests\93-do-tre-nhan-ban.sql' }
    @{ So = 3; Ten = 'Giao dich phan tan'; Dau = '2PC: PASS'
       Sql = 'tests\91-demo-chuyen-co-so.sql' }
    @{ So = 4; Ten = 'Tuong tranh 100/30'; Dau = 'BUILD SUCCESS'; Sql = $null }
    @{ So = 5; Ten = 'Truy van phan tan';  Dau = 'TRUY VAN PHAN TAN: PASS'
       Sql = 'tests\92-do-truy-van-phan-tan.sql' }
)

$ketQua = @()
foreach ($m in $muc) {
    if ($Chi -and $m.So -notin $Chi) { continue }

    $file = Join-Path $OutDir ('{0}-{1}.txt' -f $m.So, ($m.Ten -replace '[^\w]', '-'))
    Write-Host ('[{0}] {1} ...' -f $m.So, $m.Ten) -NoNewline

    if ($m.Sql) {
        # ⚠️ Goi qua TIEN TRINH CON. run.ps1 in bang Write-Host, von di thang
        # ra host chu khong qua stdout — goi truc tiep roi `> $file` thi file
        # rong, moi muc deu "FAIL" trong khi console van hien PASS day du.
        Push-Location -LiteralPath $dbRoot
        try {
            & powershell -NoProfile -File (Join-Path $dbRoot 'run.ps1') `
                -Script $m.Sql -On MASTER > $file
        } finally { Pop-Location }
    } else {
        # Yeu cau 4 la test Java, can .env nen goi qua dev-api.ps1.
        Push-Location -LiteralPath $repoRoot
        try {
            & (Join-Path $repoRoot 'scripts/dev-api.ps1') -MavenArguments `
                'clean', 'test', '-Dtest=EnrollmentConcurrencyIntegrationTest',
                '-DfailIfNoTests=false' > $file
        } finally { Pop-Location }
    }

    $dat = (Select-String -LiteralPath $file -SimpleMatch -Pattern $m.Dau -Quiet) -eq $true
    Write-Host (' {0}' -f $(if ($dat) { 'PASS' } else { 'FAIL' }))
    $ketQua += [pscustomobject]@{
        '#'        = $m.So
        'Yeu cau'  = $m.Ten
        'Ket luan' = $(if ($dat) { 'PASS' } else { 'FAIL' })
        'Output'   = Split-Path -Leaf $file
    }
}

$bang = $ketQua | Format-Table -AutoSize | Out-String
Write-Host $bang
$bang | Out-File -FilePath (Join-Path $OutDir 'TONG-KET.txt') -Encoding utf8

Write-Host ('Output: {0}' -f $OutDir)
if ($ketQua.'Ket luan' -contains 'FAIL') { exit 1 }
