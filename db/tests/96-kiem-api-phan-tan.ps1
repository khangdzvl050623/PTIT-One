<# Goi MOI endpoint cua API o che do phan tan, dung vai duoc phep.
   2xx = OK, 4xx = tu choi nghiep vu (in ma loi de doi chieu), 5xx = HONG.
   Phan ghi chay tren du lieu thu roi don sach.

   CHAY:  powershell -NoProfile -File db\tests\96-kiem-api-phan-tan.ps1
   CAN:   API dang chay voi profile `central,distributed` #>
[CmdletBinding()]
param(
    [string] $Base = 'http://localhost:8080/api',
    [string] $MatKhau = 'PtitOne@2026',
    [string] $HocKy = '2026-1',
    [string] $Mon = 'INT1358',
    [string] $SvTam = 'B26DCCN902'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Web
$ketQua = New-Object System.Collections.Generic.List[object]

function Goi($vai, $s, $method, $path, $body, [string[]] $choPhep = @()) {
    $h = @{}
    if ($method -ne 'GET' -and $s) {
        $c = Invoke-RestMethod -Uri "$Base/auth/csrf" -WebSession $s -TimeoutSec 20
        $h[$c.headerName] = $c.token
    }
    $a = @{ Uri = "$Base$path"; Method = $method; Headers = $h; TimeoutSec = 60; UseBasicParsing = $true }
    if ($s) { $a.WebSession = $s }
    if ($null -ne $body) { $a.ContentType = 'application/json; charset=utf-8'; $a.Body = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 6)) }
    $ma = ''; $noiDung = $null; $than = ''
    try {
        $r = Invoke-WebRequest @a
        $st = [int]$r.StatusCode
        if ($r.Content) { try { $noiDung = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json } catch {} }
    } catch {
        $resp = $_.Exception.Response
        if (-not $resp) { throw }
        $st = [int]$resp.StatusCode
        try { $than = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd(); if ($than -match '"code":"([A-Z_]+)"') { $ma = $Matches[1] } } catch {}
    }
    $kl = if ($st -lt 300 -or ($ma -and $ma -in $choPhep) -or @($choPhep | Where-Object { $than -match $_ }).Count) { 'OK' } elseif ($st -ge 500) { 'HONG' } else { 'TU CHOI' }
    $ketQua.Add([pscustomobject]@{ KL = $kl; Vai = $vai; Goi = "$method $path"; St = $st; Ma = $ma })
    return $noiDung
}
function DangNhap($u) {
    $null = Invoke-RestMethod -Uri "$Base/auth/csrf" -SessionVariable s -TimeoutSec 20
    $null = Goi $u $s 'POST' '/auth/login' @{ username = $u; password = $MatKhau }
    return $s
}
function Sql($srv, $db, $q) {
    $r = sqlcmd -S $srv -d $db -E -C -I -b -h -1 -W -Q "SET NOCOUNT ON;`n$q"
    if ($LASTEXITCODE -ne 0) { throw "sqlcmd loi tren $db : $r" }
}

$sv = DangNhap 'B26DCCN001'; $gv = DangNhap 'GVHCM001'; $ad = DangNhap 'admin.hcm'; $ms = DangNhap 'admin.master'
# Chua dang nhap van can phien de mang cookie CSRF.
$null = Invoke-RestMethod -Uri "$Base/auth/csrf" -SessionVariable an -TimeoutSec 20
$hk = "?maHocKy=$HocKy"

# --- Doc: danh muc va cong khai ---------------------------------------------
$null = Goi 'anon' $null 'GET' '/health' $null
foreach ($p in '/campuses', '/terms', '/faculties', '/programs', '/programs/CN-CNTT-2022', '/courses', "/courses/$Mon", '/prerequisites', "/schedules$hk", "/classes$hk", "/enrollment-periods$hk") {
    $null = Goi 'SV' $sv 'GET' $p $null
}
# --- Doc: tung vai ----------------------------------------------------------
foreach ($p in '/auth/me', '/auth/email', '/me/profile', "/me/enrollments$hk", '/me/grades', '/me/transcript', "/me/timetable$hk", '/me/notifications', '/me/notifications/unread-count') { $null = Goi 'SV' $sv 'GET' $p $null }
foreach ($p in '/me/teacher-profile', "/me/teaching-classes$hk", "/me/teaching-schedule$hk") { $null = Goi 'GV' $gv 'GET' $p $null }
$null = Goi 'AdHCM' $ad 'GET' '/health/media' $null @('(?i)"status"\s*:\s*"down"')
foreach ($p in '/health/db', '/teachers', "/reports/summary$hk", "/reports/courses$hk", '/notifications') { $null = Goi 'AdHCM' $ad 'GET' $p $null }
foreach ($p in '/accounts', "/reports/summary$hk", "/reports/courses$hk", '/notifications', '/teachers', "/classes$hk") { $null = Goi 'Master' $ms 'GET' $p $null }

# So tin chi truoc khi chay: phan don tra ve dung gia tri nay (khong thi moi lan chay du 3 TC).
$tcTruoc = sqlcmd -S 'tcp:localhost,14330' -d PTITONE_HCM -E -C -h -1 -W -Q "SET NOCOUNT ON; SELECT ISNULL((SELECT SoTinChiDaDangKy FROM dbo.SinhVienHocKy WHERE MaSinhVien = 'B26DCCN001' AND MaHocKy = '$HocKy'), -1)"
# @(...) bao ngoai: mot dong ket qua la CHUOI, [0] se lay ky tu '6' -> [int] = 54.
$tcTruoc = [int](@(@($tcTruoc) | Where-Object { $_ -match '^-?\d+$' })[0])
$dot = $null; $ma = 'KHONG-TAO-DUOC'
try {
# --- Ghi: admin co so mo dot, tao lop ---------------------------------------
$dot = @(Goi 'AdHCM' $ad 'GET' "/enrollment-periods$hk" $null) | Where-Object { $_ -and $_.maCoSo -eq 'HCM' } | Select-Object -First 1
if ($dot) {
    $null = Goi 'AdHCM' $ad 'PUT' "/enrollment-periods/$($dot.maDot)" @{ maHocKy = $HocKy; trangThai = 'DANG_MO'
        thoiGianMo = (Get-Date).ToUniversalTime().AddDays(-1).ToString('o'); thoiGianDong = (Get-Date).ToUniversalTime().AddDays(30).ToString('o') }
}
$lop = Goi 'AdHCM' $ad 'POST' '/classes' @{ maMonHoc = $Mon; maHocKy = $HocKy; soLuongToiDa = 30; hinhThucHoc = 'TRUC_TIEP'; choPhepLienCoSo = $false }
$ma = if ($lop) { $lop.maLopHP } else { 'KHONG-TAO-DUOC' }
$null = Goi 'AdHCM' $ad 'GET' "/classes/$ma" $null
$null = Goi 'AdHCM' $ad 'PUT' "/classes/$ma/teacher" @{ maGiangVien = 'GVHCM001' }
$null = Goi 'AdHCM' $ad 'PUT' "/classes/$ma/schedule" @{ thu = 8; tietBatDau = 11; soTiet = 2; phongHoc = 'API-T96'; tuanBatDau = 1; tuanKetThuc = 15 }
$null = Goi 'AdHCM' $ad 'GET' "/classes/$ma/schedule" $null
$null = Goi 'AdHCM' $ad 'PUT' "/classes/$ma" @{ soLuongToiDa = 31; trangThai = 'MO'; hinhThucHoc = 'TRUC_TIEP'; choPhepLienCoSo = $false }

# --- Ghi: SV dang ky, huy, dang ky lai; admin go ----------------------------
$null = Goi 'SV' $sv 'POST' '/me/enrollments' @{ maLopHP = $ma }
$null = Goi 'SV' $sv 'DELETE' "/me/enrollments/$ma" $null
$null = Goi 'SV' $sv 'POST' '/me/enrollments' @{ maLopHP = $ma }
$null = Goi 'AdHCM' $ad 'GET' "/classes/$ma/students" $null
$null = Goi 'AdHCM' $ad 'POST' "/classes/$ma/students/B26DCCN001/remove" @{ lyDo = 'Kiem tra API' }
$null = Goi 'SV' $sv 'POST' '/me/enrollments' @{ maLopHP = $ma }

# --- Ghi: diem --------------------------------------------------------------
$bang = Goi 'GV' $gv 'GET' "/classes/$ma/grades" $null
$dong = if ($bang) { @($bang.diem | Where-Object { $_.maSinhVien -eq 'B26DCCN001' })[0] } else { $null }
if ($dong) {
    $null = Goi 'GV' $gv 'PUT' "/classes/$ma/grades" @{ diem = @(@{ maSinhVien = 'B26DCCN001'; diemChuyenCan = 9; diemGiuaKy = 8; diemCuoiKy = 7; version = $dong.version }) }
}
$null = Goi 'GV' $gv 'POST' "/classes/$ma/grades/publish" $null
$null = Goi 'AdHCM' $ad 'POST' "/classes/$ma/grades/lock" $null
# B3: Admin Master doc ghi danh va diem MOI site.
$null = Goi 'Master' $ms 'GET' "/classes/$ma/students" $null
$null = Goi 'Master' $ms 'GET' "/classes/$ma/grades" $null
# Lop da co diem khoa: huy lop phai bi tu choi, khong duoc 5xx.
$null = Goi 'AdHCM' $ad 'POST' "/classes/$ma/cancel" @{ lyDo = 'Kiem tra API' } @('GRADE_LOCKED')

# --- Ghi: thong bao, ca hai vai admin ---------------------------------------
foreach ($x in @(@('AdHCM', $ad, 'CO_SO', 'HCM'), @('Master', $ms, 'TOAN_TRUONG', $null))) {
    $tb = @{ tieuDe = "Kiem tra API $($x[0])"; noiDung = 'Noi dung thu'; mucDo = 'THONG_THUONG'; phamVi = $x[2]; maCoSo = $x[3]; doiTuong = 'SINH_VIEN' }
    $null = Goi $x[0] $x[1] 'POST' '/notifications/preview' $tb
    $n = Goi $x[0] $x[1] 'POST' '/notifications' $tb
    if ($n) {
        $id = $n.maThongBao
        $null = Goi $x[0] $x[1] 'GET' "/notifications/$id" $null
        $tb.tieuDe += ' (sua)'
        $null = Goi $x[0] $x[1] 'PUT' "/notifications/$id" $tb
        $null = Goi $x[0] $x[1] 'POST' "/notifications/$id/send" $null
        $nhap = Goi $x[0] $x[1] 'POST' '/notifications' $tb
        if ($nhap) { $null = Goi $x[0] $x[1] 'DELETE' "/notifications/$($nhap.maThongBao)" $null }
    }
}
$hop = Goi 'SV' $sv 'GET' '/me/notifications' $null
$mot = if ($hop) { @($hop.thongBao)[0] } else { $null }
if ($mot) { $null = Goi 'SV' $sv 'POST' "/me/notifications/$($mot.maThongBao)/read" $null }
$null = Goi 'SV' $sv 'POST' '/me/notifications/read-all' $null
# Thong bao toan truong cua Master phai toi hop thu SV o co so nha (Outbox ThongBaoDaGui).
$het = (Get-Date).AddSeconds(40)
do { Start-Sleep 2; $hop = Goi 'SV' $sv 'GET' '/me/notifications' $null; $toi = @($hop.thongBao | Where-Object { $_.tieuDe -like 'Kiem tra API Master*' }).Count } while (-not $toi -and (Get-Date) -lt $het)
$ketQua.Add([pscustomobject]@{ KL = $(if ($toi) { 'OK' } else { 'HONG' }); Vai = 'Master'; Goi = 'Thong bao toan truong toi hop thu SV'; St = 0; Ma = '' })

# --- Ghi: danh muc (Master) --------------------------------------------------
$null = Goi 'Master' $ms 'POST' '/courses' @{ maMonHoc = 'TST9601'; tenMonHoc = 'Mon kiem tra API'; soTinChi = 2; maKhoa = 'ATTT' }
$null = Goi 'Master' $ms 'PUT' '/courses/TST9601' @{ tenMonHoc = 'Mon kiem tra API (sua)'; soTinChi = 3; maKhoa = 'ATTT' }
$null = Goi 'Master' $ms 'PUT' '/courses/TST9601/prerequisites' @{ tienQuyet = @($Mon) }
$null = Goi 'Master' $ms 'PUT' '/courses/TST9601/prerequisites' @{ tienQuyet = @() }
$null = Goi 'Master' $ms 'DELETE' '/courses/TST9601' $null

# --- Ghi: ho so va tai khoan (Master) ---------------------------------------
$null = Goi 'Master' $ms 'POST' '/students' @{ maSinhVien = $SvTam; hoTen = 'SV kiem tra API'; ngaySinh = '2006-01-01'; maCoSoNha = 'HCM'; maCTDT = 'CN-CNTT-2022' }
$het = (Get-Date).AddSeconds(40)
do { Start-Sleep 2; $acc = @(Goi 'Master' $ms 'GET' '/accounts?maCoSo=HCM' $null) | Where-Object { $_ -and $_.tenDangNhap -eq $SvTam -and $_.trangThai -eq 'HOAT_DONG' } } while (-not $acc -and (Get-Date) -lt $het)
$ketQua.Add([pscustomobject]@{ KL = $(if ($acc) { 'OK' } else { 'HONG' }); Vai = 'Master'; Goi = 'Outbox dung tai khoan o co so nha'; St = 0; Ma = '' })
$null = Goi 'Master' $ms 'PUT' "/accounts/$SvTam/status" @{ trangThai = 'NGUNG' }
$null = Goi 'Master' $ms 'PUT' "/accounts/$SvTam/status" @{ trangThai = 'HOAT_DONG' }
$null = Goi 'Master' $ms 'PUT' "/accounts/$SvTam/email" @{ email = 'sv902@example.com' }
$null = Goi 'Master' $ms 'POST' "/accounts/$SvTam/activation-code?guiEmail=false" $null
$null = Goi 'Master' $ms 'POST' "/accounts/B26DCCN002/password-reset" $null @('ACCOUNT_NOT_MANAGEABLE')
$null = Goi 'Master' $ms 'POST' '/teachers' @{ maGiangVien = 'GVAPI96'; hoTen = 'GV kiem tra API'; maCoSo = 'HN'; maKhoa = 'ATTT'; hocVi = 'ThS' }

# --- Ghi: tai khoan ca nhan ---------------------------------------------------
# Doi mat khau thu hoi MOI phien (ke ca phien hien tai): dang nhap lai bang mat khau moi roi doi ve.
$null = Goi 'SV' $sv 'POST' '/auth/change-password' @{ matKhauHienTai = $MatKhau; matKhauMoi = 'TamThoi@2026x' }
$null = Invoke-RestMethod -Uri "$Base/auth/csrf" -SessionVariable sv -TimeoutSec 20
$null = Goi 'SV' $sv 'POST' '/auth/login' @{ username = 'B26DCCN001'; password = 'TamThoi@2026x' }
$null = Goi 'SV' $sv 'POST' '/auth/change-password' @{ matKhauHienTai = 'TamThoi@2026x'; matKhauMoi = $MatKhau }
$sv = DangNhap 'B26DCCN001'
$null = Goi 'SV' $sv 'PUT' '/me/profile' @{ dienThoai = '0900000096' } @('EMAIL_NOT_VERIFIED', 'PROFILE_EMAIL_UNVERIFIED')
$null = Goi 'GV' $gv 'PUT' '/me/teacher-profile' @{ dienThoai = '0900000096' } @('EMAIL_NOT_VERIFIED', 'PROFILE_EMAIL_UNVERIFIED')
$null = Goi 'SV' $sv 'POST' '/auth/email/resend' $null @('MAIL_DISABLED', 'EMAIL_NOT_SET')
$null = Goi 'anon' $an 'POST' '/auth/forgot-password' @{ tenDangNhap = 'B26DCCN001'; email = 'x@example.com' } @('MAIL_DISABLED')
$null = Goi 'anon' $an 'POST' '/auth/reset-password' @{ tenDangNhap = 'B26DCCN001'; maXacThuc = '000000'; matKhauMoi = 'KhongDung@2026' } @('RESET_CODE_INVALID')
$null = Goi 'anon' $an 'POST' '/auth/activate' @{ tenDangNhap = 'B26DCCN001'; maKichHoat = 'SAI-MA-0000'; matKhauMoi = 'KhongDung@2026' } @('ACTIVATION_INVALID')
$null = Goi 'anon' $an 'POST' '/auth/activate/resend' @{ tenDangNhap = 'B26DCCN001' } @('MAIL_DISABLED')
$null = Goi 'SV' $sv 'POST' '/auth/refresh' $null
$null = Goi 'SV' $sv 'POST' '/auth/logout-all' $null
$null = Goi 'GV' $gv 'POST' '/auth/logout' $null

} finally {
# --- Don ----------------------------------------------------------------------
$null = Sql 'tcp:localhost,14330' 'PTITONE_HCM' @"
DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao WHERE t.TieuDe LIKE N'Kiem tra API%' OR t.MaLopHP = '$ma';
DELETE FROM dbo.ThongBao WHERE TieuDe LIKE N'Kiem tra API%' OR MaLopHP = '$ma';
DELETE FROM dbo.Diem WHERE MaLopHP = '$ma';
DELETE FROM dbo.DangKyHocPhan WHERE MaLopHP = '$ma';
DELETE FROM dbo.DangKyMonHoc WHERE MaLopHP = '$ma';
DELETE FROM dbo.LichHoc WHERE MaLopHP = '$ma';
DELETE FROM dbo.LopHocPhan WHERE MaLopHP = '$ma';
DELETE FROM dbo.MaKichHoat WHERE TenDangNhap = '$SvTam';
DELETE FROM dbo.TaiKhoan WHERE TenDangNhap = '$SvTam';
DELETE FROM dbo.SinhVien WHERE MaSinhVien = '$SvTam';
UPDATE dbo.SinhVienHocKy SET SoTinChiDaDangKy = $tcTruoc, SoTinChiDangGiuCho = 0 WHERE MaSinhVien = 'B26DCCN001' AND MaHocKy = '$HocKy' AND $tcTruoc >= 0;
"@
$null = Sql 'tcp:localhost,14332' 'PTITONE_DN' "DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao WHERE t.TieuDe LIKE N'Kiem tra API%'; DELETE FROM dbo.ThongBao WHERE TieuDe LIKE N'Kiem tra API%';"
$null = Sql 'tcp:localhost,14331' 'PTITONE_HN' "DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao WHERE t.TieuDe LIKE N'Kiem tra API%'; DELETE FROM dbo.ThongBao WHERE TieuDe LIKE N'Kiem tra API%'; DELETE FROM dbo.MaKichHoat WHERE TenDangNhap = 'GVAPI96'; DELETE FROM dbo.TaiKhoan WHERE TenDangNhap = 'GVAPI96'; DELETE FROM dbo.GiangVien WHERE MaGiangVien = 'GVAPI96';"
$null = Sql 'tcp:localhost,14330' 'PTITONE_MASTER' "DELETE n FROM dbo.ThongBaoNguoiNhan n JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao WHERE t.TieuDe LIKE N'Kiem tra API%'; DELETE FROM dbo.ThongBao WHERE TieuDe LIKE N'Kiem tra API%'; DELETE FROM dbo.OutboxSuKien WHERE KhoaThucThe IN ('$SvTam', 'GVAPI96') OR (LoaiSuKien = 'ThongBaoDaGui' AND NoiDung LIKE N'%Kiem tra API%'); DELETE FROM dbo.DanhBaNguoiDung WHERE TenDangNhap IN ('$SvTam', 'GVAPI96'); DELETE FROM dbo.MonHoc WHERE MaMonHoc = 'TST9601';"
if ($dot) {
    $null = Goi 'AdHCM' $ad 'PUT' "/enrollment-periods/$($dot.maDot)" @{ maHocKy = $HocKy; trangThai = $dot.trangThai; thoiGianMo = $dot.thoiGianMo; thoiGianDong = $dot.thoiGianDong }
}

}

$ketQua | Where-Object { $_.KL -ne 'OK' } | Format-Table KL, Vai, Goi, St, Ma -AutoSize | Out-String -Width 200 | Write-Host
$dem = $ketQua | Group-Object KL | ForEach-Object { "$($_.Name)=$($_.Count)" }
Write-Host ("  KIEM API PHAN TAN: {0} cuoc goi | {1}" -f $ketQua.Count, ($dem -join ' '))
if ($ketQua.KL -contains 'HONG') { exit 1 }
