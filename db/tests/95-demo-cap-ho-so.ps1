<# Demo F02 o Phan 2: Admin Master cap ho so SV (co so HN) va GV (co so DN).
   Master khong co bang SinhVien/GiangVien/TaiKhoan -> danh ba CHO_KICH_HOAT +
   Outbox; worker dung ho so o co so nha TRUOC, mo danh ba + SENT SAU.

   CHAY:  powershell -NoProfile -File db\tests\95-demo-cap-ho-so.ps1
   CAN:   API dang chay voi profile `central,distributed` #>
[CmdletBinding()]
param(
    [string] $Base = 'http://localhost:8080/api',
    [string] $MatKhauAdmin = 'PtitOne@2026',
    [string] $Sv = 'B26DCCN901',
    [string] $Gv = 'GVDN901',
    [int]    $ChoGiay = 60
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$Srv = @{ MASTER = 'tcp:localhost,14330'; HN = 'tcp:localhost,14331'; DN = 'tcp:localhost,14332' }
$Db = @{ MASTER = 'PTITONE_MASTER'; HN = 'PTITONE_HN'; DN = 'PTITONE_DN' }
$loi = @()

function Sql($nut, $q) {
    $r = sqlcmd -S $Srv[$nut] -d $Db[$nut] -E -C -I -b -h -1 -W -Q "SET NOCOUNT ON;`n$q"
    if ($LASTEXITCODE -ne 0) { throw "sqlcmd loi tren $nut : $r" }
    $dong = @($r | Where-Object { $_ -and $_.Trim() -ne '' })
    if ($dong.Count -eq 0) { return '' }
    return $dong[-1].Trim()
}
function Kiem($ten, $thuc, $mong) {
    if ("$thuc" -eq "$mong") { Write-Host ("  [ok]  {0,-44} {1}" -f $ten, $thuc) }
    else {
        Write-Host ("  [!!]  {0,-44} {1}  (mong doi {2})" -f $ten, $thuc, $mong)
        $script:loi += $ten
    }
}
function Csrf($s) { Invoke-RestMethod -Uri "$Base/auth/csrf" -WebSession $s -TimeoutSec 20 }
function Goi($s, $method, $path, $body) {
    $c = Csrf $s
    try {
        return Invoke-RestMethod -Uri "$Base$path" -Method $method -WebSession $s -TimeoutSec 60 `
            -Headers @{ $c.headerName = $c.token } -ContentType 'application/json' `
            -Body ($body | ConvertTo-Json -Depth 5)
    } catch {
        $than = ''
        if ($_.Exception.Response) {
            try { $than = (New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() } catch {}
        }
        throw "$method $path -> $than"
    }
}
function DangNhap($u, $p) {
    $null = Invoke-RestMethod -Uri "$Base/auth/csrf" -SessionVariable s -TimeoutSec 20
    $null = Goi $s 'Post' '/auth/login' @{ username = $u; password = $p }
    return $s
}
function Cho($nut, $q, $mong) {
    $het = (Get-Date).AddSeconds($ChoGiay)
    do { $v = Sql $nut $q; if ("$v" -eq "$mong") { return $v }; Start-Sleep -Seconds 2 } while ((Get-Date) -lt $het)
    return $v
}
function DonSach {
    foreach ($x in @(@('HN', $Sv, 'SinhVien', 'MaSinhVien'), @('DN', $Gv, 'GiangVien', 'MaGiangVien'))) {
        $null = Sql $x[0] @"
DELETE t FROM dbo.TokenLamMoi t JOIN dbo.PhienDangNhap p ON p.MaPhien = t.MaPhien WHERE p.TenDangNhap = '$($x[1])';
DELETE FROM dbo.PhienDangNhap WHERE TenDangNhap = '$($x[1])';
DELETE FROM dbo.MaXacThuc WHERE TenDangNhap = '$($x[1])';
DELETE FROM dbo.MaKichHoat WHERE TenDangNhap = '$($x[1])';
DELETE FROM dbo.TaiKhoan WHERE TenDangNhap = '$($x[1])';
DELETE FROM dbo.$($x[2]) WHERE $($x[3]) = '$($x[1])';
SELECT 0;
"@
    }
    $null = Sql 'MASTER' "DELETE FROM dbo.OutboxSuKien WHERE KhoaThucThe IN ('$Sv', '$Gv'); DELETE FROM dbo.DanhBaNguoiDung WHERE TenDangNhap IN ('$Sv', '$Gv'); SELECT 0;"
}

Write-Host "`n=== Pha 0: don ==="
DonSach
Kiem 'khong con ho so cu' (Sql 'MASTER' "SELECT COUNT(*) FROM dbo.DanhBaNguoiDung WHERE TenDangNhap IN ('$Sv', '$Gv');") 0

$admin = DangNhap 'admin.master' $MatKhauAdmin
$ca = @(
    @{ Ma = $Sv; Nut = 'HN'; Bang = 'SinhVien'; Cot = 'MaSinhVien'; Path = '/students'
       Body = @{ maSinhVien = $Sv; hoTen = 'Sinh Vien Demo Outbox'; ngaySinh = '2006-05-01'; maCoSoNha = 'HN'; maCTDT = 'CN-CNTT-2022' } }
    @{ Ma = $Gv; Nut = 'DN'; Bang = 'GiangVien'; Cot = 'MaGiangVien'; Path = '/teachers'
       Body = @{ maGiangVien = $Gv; hoTen = 'Giang Vien Demo Outbox'; maCoSo = 'DN'; maKhoa = 'ATTT'; hocVi = 'ThS' } }
)

foreach ($c in $ca) {
    $ma = $c.Ma; $nut = $c.Nut
    Write-Host "`n=== $ma -> co so $nut ==="
    $kq = Goi $admin 'Post' $c.Path $c.Body
    $maKichHoat = $kq.kichHoat.maKichHoat
    Kiem 'Master tra ma kich hoat trao tay' ([bool]$maKichHoat) $true
    Kiem 'Master: danh ba CHO_KICH_HOAT hoac da mo' ((Sql 'MASTER' "SELECT TrangThai FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = '$ma';") -in 'CHO_KICH_HOAT', 'HOAT_DONG') $true
    Kiem 'Master: mot su kien Outbox' (Sql 'MASTER' "SELECT COUNT(*) FROM dbo.OutboxSuKien WHERE KhoaThucThe = '$ma';") 1

    Kiem 'worker: su kien SENT' (Cho 'MASTER' "SELECT TrangThai FROM dbo.OutboxSuKien WHERE KhoaThucThe = '$ma';" 'SENT') 'SENT'
    Kiem "co so nha: $($c.Bang) + TaiKhoan + MaKichHoat" (Sql $nut "SELECT CONCAT((SELECT COUNT(*) FROM dbo.$($c.Bang) WHERE $($c.Cot) = '$ma'), (SELECT COUNT(*) FROM dbo.TaiKhoan WHERE TenDangNhap = '$ma' AND MatKhauHash IS NULL), (SELECT COUNT(*) FROM dbo.MaKichHoat WHERE TenDangNhap = '$ma'));") '111'
    Kiem 'Master: danh ba HOAT_DONG' (Sql 'MASTER' "SELECT TrangThai FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = '$ma';") 'HOAT_DONG'
    Kiem 'nhan ban danh ba ve co so nha' (Cho $nut "SELECT TrangThai FROM dbo.DanhBaNguoiDung WHERE TenDangNhap = '$ma';" 'HOAT_DONG') 'HOAT_DONG'

    # Gui lap: dua su kien ve PENDING, worker phai giao lai ma khong sinh dong thu hai.
    $null = Sql 'MASTER' "UPDATE dbo.OutboxSuKien SET TrangThai = 'PENDING', ThoiDiemXuLy = NULL WHERE KhoaThucThe = '$ma'; SELECT 0;"
    Kiem 'gui lap: SENT lai' (Cho 'MASTER' "SELECT TrangThai FROM dbo.OutboxSuKien WHERE KhoaThucThe = '$ma';" 'SENT') 'SENT'
    Kiem 'gui lap: khong sinh dong thu hai' (Sql $nut "SELECT CONCAT((SELECT COUNT(*) FROM dbo.$($c.Bang) WHERE $($c.Cot) = '$ma'), (SELECT COUNT(*) FROM dbo.TaiKhoan WHERE TenDangNhap = '$ma'), (SELECT COUNT(*) FROM dbo.MaKichHoat WHERE TenDangNhap = '$ma'));") '111'

    $anon = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Goi $anon 'Post' '/auth/activate' @{ tenDangNhap = $ma; maKichHoat = $maKichHoat; matKhauMoi = 'Demo@2026x' }
    $s = DangNhap $ma 'Demo@2026x'
    $me = Invoke-RestMethod -Uri "$Base/auth/me" -WebSession $s -TimeoutSec 20
    Kiem 'kich hoat roi dang nhap: co so' $me.homeCampus $nut
}

Write-Host "`n=== Don sach ==="
DonSach
Kiem 'khong con vet' (Sql 'MASTER' "SELECT COUNT(*) FROM dbo.OutboxSuKien WHERE KhoaThucThe IN ('$Sv', '$Gv');") 0

Write-Host ''
if ($loi.Count -eq 0) {
    Write-Host '  CAP HO SO PHAN TAN: PASS - Outbox Master -> co so nha, gui lap idempotent, kich hoat duoc.'
    exit 0
}
Write-Host ("  CAP HO SO PHAN TAN: FAIL - {0} muc lech: {1}" -f $loi.Count, ($loi -join ', '))
exit 1
