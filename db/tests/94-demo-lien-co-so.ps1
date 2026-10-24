<# Demo dau-cuoi: SV co so HCM dang ky lop TRUC TUYEN o HN, diem tra ve HCM.

   CHAY:  powershell -NoProfile -File db\tests\94-demo-lien-co-so.ps1
   CAN:   API dang chay voi profile `central,distributed` (scripts\dev-api.ps1)

   Bon pha, moi pha tu khang dinh. In PASS/FAIL, khong tuong thuat. #>
[CmdletBinding()]
param(
    [string] $Base = 'http://localhost:8080/api',
    [string] $SvHCM = 'B26DCCN001',
    [string] $GvHN = 'GVHN001',
    [string] $MatKhau = 'PtitOne@2026',
    [string] $Lop = 'DEMO-LCS-HN01',
    # Mon KHONG co tien quyet va khong nam trong CTDT da giu cho cua SV.
    [string] $Mon = 'INT1358',
    [string] $HocKy = '2026-1',
    [int]    $ChoOutboxGiay = 40
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$SrvHCM = 'tcp:localhost,14330'
$SrvHN = 'tcp:localhost,14331'
$loi = @()

# SET NOCOUNT ON va lay DONG CUOI: khong co no thi "(n rows affected)" lot vao
# ket qua va moi phep so sanh so deu hong.
function Sql($server, $db, $q) {
    $r = sqlcmd -S $server -d $db -E -C -I -b -h -1 -W -Q "SET NOCOUNT ON;`n$q"
    if ($LASTEXITCODE -ne 0) { throw "sqlcmd loi tren $db : $r" }
    $dong = @($r | Where-Object { $_ -and $_.Trim() -ne '' -and $_ -notmatch 'rows affected' })
    if ($dong.Count -eq 0) { return '' }
    return $dong[-1].Trim()
}
function So($server, $db, $q) { return [int](Sql $server $db $q) }
function Kiem($ten, $thuc, $mong) {
    if ("$thuc" -eq "$mong") { Write-Host ("  [ok]  {0,-44} {1}" -f $ten, $thuc) }
    else {
        Write-Host ("  [!!]  {0,-44} {1}  (mong doi {2})" -f $ten, $thuc, $mong)
        $script:loi += $ten
    }
}
function DangNhap($u) {
    $c = Invoke-RestMethod -Uri "$Base/auth/csrf" -SessionVariable s -TimeoutSec 20
    $null = Invoke-RestMethod -Uri "$Base/auth/login" -Method Post -ContentType 'application/json' `
        -Body (@{ username = $u; password = $MatKhau } | ConvertTo-Json) `
        -Headers @{ $c.headerName = $c.token } -WebSession $s -TimeoutSec 30
    return $s
}
function Goi($s, $method, $path, $body) {
    $c = Invoke-RestMethod -Uri "$Base/auth/csrf" -WebSession $s -TimeoutSec 20
    $args = @{ Uri = "$Base$path"; Method = $method; WebSession = $s; TimeoutSec = 60
               Headers = @{ $c.headerName = $c.token } }
    if ($body) { $args.ContentType = 'application/json'; $args.Body = ($body | ConvertTo-Json -Depth 5) }
    try { return Invoke-RestMethod @args }
    catch {
        # In than loi: 409 tran khong kem ma loi thi khong biet dieu kien nao chan.
        $than = ''
        if ($_.Exception.Response) {
            try { $than = (New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() } catch {}
        }
        throw "$method $path -> $than"
    }
}

# --- Pha 0: don va gieo lop truc tuyen o HN --------------------------------
Write-Host "`n=== Pha 0: fixture ==="
$donHN = @"
-- Cong bo diem sinh ThongBao tro vao lop, phai xoa truoc khi xoa lop.
DELETE n FROM dbo.ThongBaoNguoiNhan n
  JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao WHERE t.MaLopHP = '$Lop';
DELETE FROM dbo.ThongBao WHERE MaLopHP = '$Lop';
DELETE FROM dbo.OutboxSuKien WHERE KhoaThucThe LIKE '$Lop%';
DELETE FROM dbo.KetQuaXuLyYeuCau WHERE MaLopHP = '$Lop';
DELETE FROM dbo.Diem WHERE MaLopHP = '$Lop';
DELETE FROM dbo.DangKyHocPhan WHERE MaLopHP = '$Lop';
DELETE FROM dbo.LichHoc WHERE MaLopHP = '$Lop';
DELETE FROM dbo.LopHocPhan WHERE MaLopHP = '$Lop';
INSERT INTO dbo.LopHocPhan (MaLopHP, MaMonHoc, MaHocKy, MaCoSoHost, MaGiangVien,
                            SoLuongToiDa, SoLuongDaDangKy, TrangThai, ChoPhepLienCoSo,
                            HinhThucHoc, PhienBanLich)
VALUES ('$Lop', '$Mon', '$HocKy', 'HN', '$GvHN', 40, 0, 'MO', 1, 'TRUC_TUYEN', 1);
INSERT INTO dbo.LichHoc (MaLopHP, Thu, TietBatDau, SoTiet, PhongHoc, TuanBatDau, TuanKetThuc)
VALUES ('$Lop', 7, 1, 3, N'Truc tuyen', 1, 15);
UPDATE dbo.DotDangKy SET TrangThai = 'DANG_MO',
       ThoiGianMo = DATEADD(day, -1, SYSUTCDATETIME()),
       ThoiGianDong = DATEADD(day, 30, SYSUTCDATETIME())
 WHERE MaCoSo = 'HN' AND MaHocKy = '$HocKy';
SELECT COUNT(*) FROM dbo.LopHocPhan WHERE MaLopHP = '$Lop';
"@
Kiem 'lop truc tuyen tai HN' (So $SrvHN 'PTITONE_HN' $donHN) 1

$donHCM = @"
DELETE FROM dbo.BangDiemMirror WHERE MaLopHP = '$Lop';
DELETE FROM dbo.LichHocMirror
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy' AND MaMonHoc = '$Mon';
DELETE FROM dbo.YeuCauHocLienCoSo WHERE MaLopHP = '$Lop';
DELETE FROM dbo.DangKyMonHoc
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy' AND MaMonHoc = '$Mon';
UPDATE dbo.SinhVienHocKy SET SoTinChiDangGiuCho = 0
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy';
SELECT 0;
"@
$null = Sql $SrvHCM 'PTITONE_HCM' $donHCM

$tinChiTruoc = So $SrvHCM 'PTITONE_HCM' @"
SELECT ISNULL((SELECT SoTinChiDaDangKy FROM dbo.SinhVienHocKy
                WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy'), 0);
"@

# --- Pha 1: saga dang ky ---------------------------------------------------
Write-Host "`n=== Pha 1: saga dang ky ==="
$sv = DangNhap $SvHCM
$yc = Goi $sv 'Post' '/me/cross-campus' @{ maCoSoHost = 'HN'; maLopHP = $Lop }
Kiem 'trang thai yeu cau' $yc.trangThai 'DA_DANG_KY'
Kiem 'co so Host' $yc.maCoSoHost 'HN'

Kiem 'HOST: ghi danh' (So $SrvHN 'PTITONE_HN' @"
SELECT COUNT(*) FROM dbo.DangKyHocPhan
 WHERE MaLopHP = '$Lop' AND MaSinhVien = '$SvHCM' AND TrangThai = 'DA_DANG_KY'
   AND MaCoSoNhaSV = 'HCM' AND MaYeuCau IS NOT NULL;
"@) 1
Kiem 'HOST: si so lop' (So $SrvHN 'PTITONE_HN' "SELECT SoLuongDaDangKy FROM dbo.LopHocPhan WHERE MaLopHP = '$Lop';") 1
Kiem 'HOST: ket qua xu ly' (So $SrvHN 'PTITONE_HN' "SELECT COUNT(*) FROM dbo.KetQuaXuLyYeuCau WHERE MaLopHP = '$Lop' AND KetQua = 'DA_DANG_KY';") 1
Kiem 'HOST: dong diem rong' (So $SrvHN 'PTITONE_HN' "SELECT COUNT(*) FROM dbo.Diem WHERE MaLopHP = '$Lop' AND MaSinhVien = '$SvHCM';") 1

Kiem 'HOME: yeu cau' (So $SrvHCM 'PTITONE_HCM' "SELECT COUNT(*) FROM dbo.YeuCauHocLienCoSo WHERE MaLopHP = '$Lop' AND TrangThai = 'DA_DANG_KY';") 1
Kiem 'HOME: dang ky mon' (So $SrvHCM 'PTITONE_HCM' "SELECT COUNT(*) FROM dbo.DangKyMonHoc WHERE MaSinhVien = '$SvHCM' AND MaLopHP = '$Lop' AND TrangThai = 'DA_DANG_KY';") 1
Kiem 'HOME: lich hoc mirror' (So $SrvHCM 'PTITONE_HCM' "SELECT COUNT(*) FROM dbo.LichHocMirror WHERE MaSinhVien = '$SvHCM' AND MaMonHoc = '$Mon';") 1
Kiem 'HOME: tin chi chuyen sang da dung' (So $SrvHCM 'PTITONE_HCM' @"
SELECT SoTinChiDaDangKy - $tinChiTruoc FROM dbo.SinhVienHocKy
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy';
"@) (So $SrvHN 'PTITONE_HN' "SELECT SoTinChi FROM dbo.MonHoc WHERE MaMonHoc = '$Mon';")
Kiem 'HOME: khong con giu cho' (So $SrvHCM 'PTITONE_HCM' @"
SELECT SoTinChiDangGiuCho FROM dbo.SinhVienHocKy
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy';
"@) 0

# Yeu cau DA co ket luan thi retry phai bi tu choi — khong danh gia lai.
$maRetry = ''
try { $null = Goi $sv 'Post' "/me/cross-campus/$($yc.maYeuCau)/retry" $null }
catch { if ("$_" -match '"code":"([A-Z_]+)"') { $maRetry = $Matches[1] } }
Kiem 'retry sau khi da co ket luan' $maRetry 'CROSS_CAMPUS_ALREADY_CLOSED'
Kiem 'si so KHONG tang them' (So $SrvHN 'PTITONE_HN' "SELECT SoLuongDaDangKy FROM dbo.LopHocPhan WHERE MaLopHP = '$Lop';") 1
Kiem 'chi MOT dong ket qua xu ly' (So $SrvHN 'PTITONE_HN' "SELECT COUNT(*) FROM dbo.KetQuaXuLyYeuCau WHERE MaLopHP = '$Lop';") 1

# --- Pha 2: giang vien HN nhap va cong bo diem -----------------------------
Write-Host "`n=== Pha 2: nhap va cong bo diem tai HN ==="
$gv = DangNhap $GvHN
$bang = Goi $gv 'Get' "/classes/$Lop/grades" $null
$dong = @($bang.diem | Where-Object { $_.maSinhVien -eq $SvHCM })[0]
$null = Goi $gv 'Put' "/classes/$Lop/grades" @{ diem = @(@{
    maSinhVien = $SvHCM; diemChuyenCan = 9; diemGiuaKy = 8; diemCuoiKy = 8.5; version = $dong.version }) }
$null = Goi $gv 'Post' "/classes/$Lop/grades/publish" $null

$tongKet = (Sql $SrvHN 'PTITONE_HN' "SELECT CONVERT(varchar(10), DiemTongKet) FROM dbo.Diem WHERE MaLopHP = '$Lop' AND MaSinhVien = '$SvHCM';")
Kiem 'HOST: da cong bo' (So $SrvHN 'PTITONE_HN' "SELECT COUNT(*) FROM dbo.Diem WHERE MaLopHP = '$Lop' AND NgayCongBo IS NOT NULL;") 1
Kiem 'HOST: su kien Outbox' (So $SrvHN 'PTITONE_HN' "SELECT COUNT(*) FROM dbo.OutboxSuKien WHERE KhoaThucThe = '$Lop`:$SvHCM' AND LoaiSuKien = 'DiemDaChot';") 1

# --- Pha 3: worker day diem ve HCM -----------------------------------------
Write-Host "`n=== Pha 3: Outbox tra diem ve co so nha ==="
$het = (Get-Date).AddSeconds($ChoOutboxGiay)
do {
    Start-Sleep -Seconds 3
    $veDich = So $SrvHCM 'PTITONE_HCM' "SELECT COUNT(*) FROM dbo.BangDiemMirror WHERE MaLopHP = '$Lop' AND MaSinhVien = '$SvHCM';"
} while ($veDich -eq 0 -and (Get-Date) -lt $het)

Kiem 'HOME: bang diem mirror' $veDich 1
if ($veDich -eq 1) {
    Kiem 'HOME: diem khop voi Host' (Sql $SrvHCM 'PTITONE_HCM' "SELECT CONVERT(varchar(10), DiemTongKet) FROM dbo.BangDiemMirror WHERE MaLopHP = '$Lop' AND MaSinhVien = '$SvHCM';") $tongKet
}
Kiem 'HOST: su kien da SENT' (So $SrvHN 'PTITONE_HN' "SELECT COUNT(*) FROM dbo.OutboxSuKien WHERE KhoaThucThe = '$Lop`:$SvHCM' AND TrangThai = 'SENT';") 1

# --- Pha 4: don sach ---------------------------------------------------------
# BAT BUOC. Demo them lop va ghi danh ma CENTRAL khong co, de lai thi doi soat
# cua yeu cau 1 bao LECH va trong nhu loi phan manh.
Write-Host "`n=== Pha 4: don sach ==="
$null = Sql $SrvHN 'PTITONE_HN' @"
-- Cong bo diem sinh ThongBao tro vao lop, phai xoa truoc khi xoa lop.
DELETE n FROM dbo.ThongBaoNguoiNhan n
  JOIN dbo.ThongBao t ON t.MaThongBao = n.MaThongBao WHERE t.MaLopHP = '$Lop';
DELETE FROM dbo.ThongBao WHERE MaLopHP = '$Lop';
DELETE FROM dbo.OutboxSuKien WHERE KhoaThucThe LIKE '$Lop%';
DELETE FROM dbo.KetQuaXuLyYeuCau WHERE MaLopHP = '$Lop';
DELETE FROM dbo.Diem WHERE MaLopHP = '$Lop';
DELETE FROM dbo.DangKyHocPhan WHERE MaLopHP = '$Lop';
DELETE FROM dbo.LichHoc WHERE MaLopHP = '$Lop';
DELETE FROM dbo.LopHocPhan WHERE MaLopHP = '$Lop';
SELECT 0;
"@
$null = Sql $SrvHCM 'PTITONE_HCM' @"
DELETE FROM dbo.BangDiemMirror WHERE MaLopHP = '$Lop';
DELETE FROM dbo.LichHocMirror
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy' AND MaMonHoc = '$Mon';
DELETE FROM dbo.YeuCauHocLienCoSo WHERE MaLopHP = '$Lop';
DELETE FROM dbo.DangKyMonHoc
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy' AND MaMonHoc = '$Mon';
UPDATE dbo.SinhVienHocKy
   SET SoTinChiDaDangKy = $tinChiTruoc, SoTinChiDangGiuCho = 0
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy';
SELECT 0;
"@
Kiem 'HOST: khong con vet' (So $SrvHN 'PTITONE_HN' "SELECT COUNT(*) FROM dbo.LopHocPhan WHERE MaLopHP = '$Lop';") 0
Kiem 'HOME: khong con vet' (So $SrvHCM 'PTITONE_HCM' "SELECT COUNT(*) FROM dbo.YeuCauHocLienCoSo WHERE MaLopHP = '$Lop';") 0
Kiem 'HOME: tin chi ve nhu cu' (So $SrvHCM 'PTITONE_HCM' @"
SELECT SoTinChiDaDangKy FROM dbo.SinhVienHocKy
 WHERE MaSinhVien = '$SvHCM' AND MaHocKy = '$HocKy';
"@) $tinChiTruoc

# --- Ket luan ---------------------------------------------------------------
Write-Host ''
if ($loi.Count -eq 0) {
    Write-Host '  LIEN CO SO: PASS - saga ghi hai site, Outbox tra diem ve co so nha.'
    exit 0
}
Write-Host ("  LIEN CO SO: FAIL - {0} muc lech: {1}" -f $loi.Count, ($loi -join ', '))
exit 1
