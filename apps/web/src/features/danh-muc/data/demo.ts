import type { AccountSummary, Campus, Faculty, StudyProgram } from '../types'

/**
 * Dữ liệu tạm cho màn Hồ sơ và tài khoản — khớp seed
 * `db/central/seed/10-auth-seed.sql`. Bốn tài khoản demo của README có ở đây
 * nên đăng nhập bằng tài khoản nào cũng thấy chính mình trong danh bạ.
 */

/**
 * Ba cơ sở của seed. Trùng danh sách ở màn thống kê — hai feature không import
 * nhau nên mỗi bên giữ một bản; bỏ được khi backend mở `/api/campuses`.
 */
export const CAMPUSES: readonly Campus[] = [
  { maCoSo: 'HCM', tenCoSo: 'Cơ sở TP. Hồ Chí Minh' },
  { maCoSo: 'HN', tenCoSo: 'Cơ sở Hà Nội' },
  { maCoSo: 'DN', tenCoSo: 'Cơ sở Đà Nẵng' },
]

/** `GET /api/faculties`. */
export const FACULTIES: readonly Faculty[] = [
  { maKhoa: 'CNTT1', tenKhoa: 'Khoa Công nghệ thông tin 1' },
  { maKhoa: 'CNTT2', tenKhoa: 'Khoa Công nghệ thông tin 2' },
  { maKhoa: 'VT1', tenKhoa: 'Khoa Viễn thông 1' },
  { maKhoa: 'ATTT', tenKhoa: 'Khoa An toàn thông tin' },
  { maKhoa: 'QTKD', tenKhoa: 'Khoa Quản trị kinh doanh' },
]

/** `GET /api/programs`. */
export const PROGRAMS: readonly StudyProgram[] = [
  { maCTDT: 'CNTT-2022', tenCTDT: 'Công nghệ thông tin 2022', maKhoa: 'CNTT2', tongTinChi: 152 },
  { maCTDT: 'ATTT-2022', tenCTDT: 'An toàn thông tin 2022', maKhoa: 'ATTT', tongTinChi: 150 },
  { maCTDT: 'DTVT-2022', tenCTDT: 'Điện tử viễn thông 2022', maKhoa: 'VT1', tongTinChi: 150 },
  { maCTDT: 'QTKD-2022', tenCTDT: 'Quản trị kinh doanh 2022', maKhoa: 'QTKD', tongTinChi: 130 },
]

/* --- Danh bạ ban đầu ------------------------------------------------------ */

function sv(maSinhVien: string, maCoSo: string, daKichHoat = true): AccountSummary {
  return {
    tenDangNhap: maSinhVien,
    loaiNguoiDung: 'SINH_VIEN',
    maCoSo,
    maThucThe: maSinhVien,
    trangThai: 'HOAT_DONG',
    daKichHoat,
  }
}

function gv(maGiangVien: string, maCoSo: string, daKichHoat = true): AccountSummary {
  return {
    tenDangNhap: maGiangVien,
    loaiNguoiDung: 'GIANG_VIEN',
    maCoSo,
    maThucThe: maGiangVien,
    trangThai: 'HOAT_DONG',
    daKichHoat,
  }
}

/**
 * Email đã lưu của danh bạ.
 *
 * Để riêng vì `AccountSummary` của backend **không** có trường email — server
 * biết địa chỉ nhưng không trả ra danh sách. Bảng này mô phỏng phần server
 * biết, để nhánh "mã chỉ đi qua thư" chạy được: `GVHCM003` có email, còn
 * `B26DCCN004` thì không nên chỉ cấp mã trao tay được.
 */
export const EMAILS: Readonly<Record<string, string>> = {
  GVHCM003: 'suongdth@ptithcm.edu.vn',
}

/**
 * Danh bạ demo. Cố ý có đủ các trạng thái màn hình phải xử lý:
 * - `B26DCCN004` chưa kích hoạt, **không** email → mã trao tay.
 * - `GVHCM003` chưa kích hoạt, **có** email → mã chỉ đi qua thư.
 * - `B26DCCN005` đang bị khoá → nút đổi thành "Mở khoá".
 * - `admin.master` không khoá được và không cấp mã được (server chặn cả hai).
 */
export const DEMO_ACCOUNTS: readonly AccountSummary[] = [
  sv('B26DCCN001', 'HCM'),
  sv('B26DCCN002', 'HCM'),
  sv('B26DCCN003', 'HCM'),
  sv('B26DCCN004', 'HCM', false),
  { ...sv('B26DCCN005', 'HCM'), trangThai: 'NGUNG' },
  sv('B26DCAT101', 'HCM'),
  sv('B25DCCN088', 'HN'),
  sv('B25DCVT011', 'DN'),
  gv('GVHCM001', 'HCM'),
  gv('GVHCM002', 'HCM'),
  gv('GVHCM003', 'HCM', false),
  gv('GVHN001', 'HN'),
  {
    tenDangNhap: 'admin.hcm',
    loaiNguoiDung: 'ADMIN_CO_SO',
    maCoSo: 'HCM',
    maThucThe: null,
    trangThai: 'HOAT_DONG',
    daKichHoat: true,
  },
  {
    tenDangNhap: 'admin.hn',
    loaiNguoiDung: 'ADMIN_CO_SO',
    maCoSo: 'HN',
    maThucThe: null,
    trangThai: 'HOAT_DONG',
    daKichHoat: true,
  },
  {
    tenDangNhap: 'admin.master',
    loaiNguoiDung: 'ADMIN_MASTER',
    maCoSo: null,
    maThucThe: null,
    trangThai: 'HOAT_DONG',
    daKichHoat: true,
  },
]
