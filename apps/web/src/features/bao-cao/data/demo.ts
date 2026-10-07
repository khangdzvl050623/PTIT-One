import type { Campus, CourseRow, ReportTerm } from '../types'

/**
 * Dữ liệu tạm cho dashboard quản trị — thay bằng `GET /api/reports/summary` và
 * `GET /api/reports/courses`. HCM học kỳ hiện tại khớp 15 lớp của màn đăng ký
 * (585/835 chỗ). HK2 2025-26 đã công bố điểm để biểu đồ phân bố điểm có dữ liệu.
 */

export const CAMPUSES: readonly Campus[] = [
  { maCoSo: 'HCM', tenCoSo: 'Cơ sở TP. Hồ Chí Minh' },
  { maCoSo: 'HN', tenCoSo: 'Cơ sở Hà Nội' },
  { maCoSo: 'DN', tenCoSo: 'Cơ sở Đà Nẵng' },
]

/** Mới nhất trước. */
export const REPORT_TERMS: readonly ReportTerm[] = [
  { maHocKy: '2026-2027-HK1', tenHocKy: 'Học kỳ 1 - Năm học 2026 - 2027' },
  { maHocKy: '2025-2026-HK2', tenHocKy: 'Học kỳ 2 - Năm học 2025 - 2026' },
]

/**
 * Một môn: số lớp, lượt, sức chứa và (tuỳ chọn) phân bố điểm đã công bố.
 * Đạt = 4 khoảng từ 4.0 trở lên; trượt = `<4.0`; còn lại là chưa có kết quả.
 */
function row(
  maMonHoc: string,
  tenMonHoc: string,
  soLop: number,
  luotDangKy: number,
  tongSucChua: number,
  phanBoDiem: readonly number[] = [0, 0, 0, 0, 0],
): CourseRow {
  const coDiem = phanBoDiem.reduce((s, n) => s + n, 0)
  return {
    maMonHoc,
    tenMonHoc,
    soLop,
    luotDangKy,
    tongSucChua,
    tongDaDangKy: luotDangKy,
    tiLeLapDay: tongSucChua ? Math.round((luotDangKy / tongSucChua) * 10_000) / 10_000 : null,
    soTruot: phanBoDiem[0] ?? 0,
    soDat: coDiem - (phanBoDiem[0] ?? 0),
    chuaCoKetQua: luotDangKy - coDiem,
    phanBoDiem,
  }
}

/** Cơ sở khác cùng chương trình — co giãn theo quy mô, điểm đều đã công bố. */
function scaled(rows: readonly CourseRow[], factor: number): CourseRow[] {
  return rows.map((r) => {
    const buckets = r.phanBoDiem.map((n) => Math.round(n * factor))
    const luot = buckets.reduce((s, n) => s + n, 0)
    return row(
      r.maMonHoc,
      r.tenMonHoc,
      Math.max(1, Math.round(r.soLop * factor)),
      luot,
      Math.max(luot, Math.round(r.tongSucChua * factor)),
      buckets,
    )
  })
}

const HCM_HK1: CourseRow[] = [
  row('BAS1158', 'Tiếng Anh (Course 2)', 1, 38, 45),
  row('BAS1203', 'Giải tích 2', 1, 52, 60),
  row('INT1313', 'Cơ sở dữ liệu', 1, 30, 60),
  row('INT1339', 'Ngôn ngữ lập trình C++', 2, 88, 120),
  row('INT1340', 'Nhập môn công nghệ phần mềm', 1, 71, 80),
  row('INT1341', 'Nhập môn trí tuệ nhân tạo', 1, 48, 60),
  row('INT1342', 'Phân tích và thiết kế hệ thống thông tin', 1, 20, 60),
  row('INT13147', 'Thực tập cơ sở', 1, 12, 40),
  row('INT13162', 'Lập trình với Python', 1, 55, 60),
  row('INT1358', 'Toán rời rạc 1', 1, 44, 60),
  row('INT1450', 'Quản lý dự án phần mềm', 1, 40, 40),
  row('INT14148', 'Cơ sở dữ liệu phân tán', 1, 33, 60),
  row('MAR1322', 'Marketing căn bản', 1, 25, 50),
  row('SKD1102', 'Kỹ năng làm việc nhóm', 1, 29, 40),
]

const HN_HK1: CourseRow[] = [
  row('BAS1158', 'Tiếng Anh (Course 2)', 3, 150, 180),
  row('BAS1203', 'Giải tích 2', 2, 110, 120),
  row('INT1313', 'Cơ sở dữ liệu', 2, 40, 60),
  row('INT1339', 'Ngôn ngữ lập trình C++', 3, 165, 180),
  row('INT1340', 'Nhập môn công nghệ phần mềm', 2, 140, 160),
  row('INT1341', 'Nhập môn trí tuệ nhân tạo', 2, 80, 120),
  row('INT13162', 'Lập trình với Python', 2, 60, 120),
  row('INT1358', 'Toán rời rạc 1', 2, 98, 120),
  row('INT14148', 'Cơ sở dữ liệu phân tán', 2, 47, 120),
  row('SKD1102', 'Kỹ năng làm việc nhóm', 2, 20, 20),
]

const DN_HK1: CourseRow[] = [
  row('BAS1158', 'Tiếng Anh (Course 2)', 2, 70, 90),
  row('BAS1203', 'Giải tích 2', 1, 45, 60),
  row('INT1339', 'Ngôn ngữ lập trình C++', 2, 80, 120),
  row('INT1340', 'Nhập môn công nghệ phần mềm', 1, 40, 60),
  row('INT1358', 'Toán rời rạc 1', 1, 30, 60),
  row('SKD1102', 'Kỹ năng làm việc nhóm', 2, 36, 60),
]

const HCM_HK2: CourseRow[] = [
  row('BAS1107', 'Giáo dục thể chất 2', 2, 92, 100, [2, 5, 18, 40, 27]),
  row('BAS1151', 'Kinh tế chính trị Mác - Lênin', 2, 105, 120, [6, 14, 35, 38, 12]),
  row('BAS1210', 'Lý thuyết xác suất và thống kê', 2, 110, 120, [9, 16, 30, 40, 15]),
  row('BAS1224', 'Vật lý 1 và thí nghiệm', 2, 98, 120, [21, 20, 27, 22, 8]),
  row('INT1155', 'Tin học cơ sở 2', 2, 115, 120, [4, 10, 26, 45, 30]),
  row('INT1313', 'Cơ sở dữ liệu', 2, 96, 120, [17, 18, 25, 26, 10]),
  row('SKD1101', 'Kỹ năng thuyết trình', 1, 40, 40, [0, 2, 8, 18, 12]),
]

/**
 * Phần tóm tắt KHÔNG suy được từ dòng theo môn: số sinh viên khác nhau, số
 * lớp đầy và trạng thái bảng điểm theo lớp. `chuaCongBo = soLop − daKhoa − daCongBo`.
 */
export interface ScopeExtras {
  soSinhVien: number
  soLopDay: number
  daKhoa: number
  daCongBo: number
}

interface Dataset {
  rows: readonly CourseRow[]
  extras: ScopeExtras
}

export const REPORT_DATA: Readonly<Record<string, Readonly<Record<string, Dataset>>>> = {
  '2026-2027-HK1': {
    HCM: { rows: HCM_HK1, extras: { soSinhVien: 212, soLopDay: 1, daKhoa: 0, daCongBo: 0 } },
    HN: { rows: HN_HK1, extras: { soSinhVien: 338, soLopDay: 4, daKhoa: 0, daCongBo: 0 } },
    DN: { rows: DN_HK1, extras: { soSinhVien: 117, soLopDay: 0, daKhoa: 0, daCongBo: 0 } },
  },
  '2025-2026-HK2': {
    HCM: { rows: HCM_HK2, extras: { soSinhVien: 120, soLopDay: 2, daKhoa: 11, daCongBo: 2 } },
    HN: {
      rows: scaled(HCM_HK2, 1.4),
      extras: { soSinhVien: 170, soLopDay: 3, daKhoa: 18, daCongBo: 1 },
    },
    DN: {
      rows: scaled(HCM_HK2, 0.6),
      extras: { soSinhVien: 74, soLopDay: 1, daKhoa: 6, daCongBo: 1 },
    },
  },
}

/** Giờ chốt số liệu demo — API thật trả `capNhatLuc` của lần tính. */
export const DEMO_UPDATED_AT = '2026-10-03T08:00:00Z'
