import type {
  BestResults,
  ClassOffer,
  CourseSummary,
  EnrolledCourse,
  EnrollmentPeriod,
  ScheduleSlot,
  StudentProgram,
} from '../types'

/**
 * Dữ liệu tạm cho màn đăng ký — sinh viên `B26DCCN001`, cơ sở HCM. Mỗi lớp
 * "mở" được chọn để thử một nhánh của luồng kiểm tra trên server:
 *
 * | Lớp                  | Bấm Đăng ký sẽ ra            |
 * |----------------------|------------------------------|
 * | INT1341 · HCM01      | thành công                   |
 * | INT13162 · HCM04     | thành công (đủ 23/24 TC)     |
 * | INT13147 · HCM01     | `CREDIT_LIMIT_EXCEEDED` sau hai lớp trên |
 * | INT1342 · HCM01      | `SCHEDULE_CLASH` với INT1339 |
 * | INT14148 · HCM01     | `PREREQUISITE_NOT_MET`       |
 * | INT1450 · HCM01      | `CLASS_FULL`                 |
 * | INT1339 · HCM02      | `ENROLLMENT_DUPLICATE_COURSE`|
 * | MAR1322 · HCM01      | `COURSE_NOT_IN_PROGRAM`      |
 * | INT1313 · HCM01      | thành công — **học lại** (đã rớt) |
 */

export const DEMO_MA_HOC_KY = '2026-2027-HK1'
export const DEMO_TEN_HOC_KY = 'Học kỳ 1 - Năm học 2026 - 2027'
/** `HocKy.NgayBatDau` — tuần n bắt đầu từ ngày này + 7·(n−1). */
export const DEMO_NGAY_BAT_DAU = '2026-08-31'

/** Trần tín chỉ — trùng mặc định `ptitone.enrollment.tran-tin-chi` (giả định demo). */
export const TRAN_TIN_CHI = 24

export const DEMO_PERIOD: EnrollmentPeriod = {
  maDot: 'DOT-2026-1-HCM',
  maHocKy: DEMO_MA_HOC_KY,
  maCoSo: 'HCM',
  thoiGianMo: '2026-09-28T01:00:00Z', // 08:00 giờ Việt Nam
  thoiGianDong: '2026-10-10T10:00:00Z', // 17:00 giờ Việt Nam
  trangThai: 'DANG_MO',
}

/** Tên học kỳ cho các đợt — thay bằng `GET /api/terms`. */
export const TERM_NAMES: Readonly<Record<string, string>> = {
  '2027-2028-HK1': 'Học kỳ 1 - Năm học 2027 - 2028',
  '2026-2027-HK2': 'Học kỳ 2 - Năm học 2026 - 2027',
  '2026-2027-HK1': 'Học kỳ 1 - Năm học 2026 - 2027',
  '2025-2026-HK3': 'Học kỳ 3 (hè) - Năm học 2025 - 2026',
  '2025-2026-HK2': 'Học kỳ 2 - Năm học 2025 - 2026',
  '2025-2026-HK1': 'Học kỳ 1 - Năm học 2025 - 2026',
}

/**
 * Các đợt của cơ sở HCM (`GET /api/enrollment-periods` — sinh viên chỉ thấy
 * cơ sở mình). Mỗi học kỳ tối đa MỘT đợt `DANG_MO` (unique index V3).
 * Giờ lưu UTC; 01:00Z = 08:00, 10:00Z = 17:00 giờ Việt Nam.
 */
export const DEMO_PERIODS: readonly EnrollmentPeriod[] = [
  {
    maDot: 'DOT-2026-2-HCM',
    maHocKy: '2026-2027-HK2',
    maCoSo: 'HCM',
    thoiGianMo: '2027-01-04T01:00:00Z',
    thoiGianDong: '2027-01-15T10:00:00Z',
    trangThai: 'CHUA_MO',
  },
  DEMO_PERIOD,
  {
    maDot: 'DOT-2025-3-HCM',
    maHocKy: '2025-2026-HK3',
    maCoSo: 'HCM',
    thoiGianMo: '2026-05-25T01:00:00Z',
    thoiGianDong: '2026-06-05T10:00:00Z',
    trangThai: 'DA_DONG',
  },
  {
    maDot: 'DOT-2025-2-HCM',
    maHocKy: '2025-2026-HK2',
    maCoSo: 'HCM',
    thoiGianMo: '2026-01-05T01:00:00Z',
    thoiGianDong: '2026-01-16T10:00:00Z',
    trangThai: 'DA_DONG',
  },
  {
    maDot: 'DOT-2025-1-HCM',
    maHocKy: '2025-2026-HK1',
    maCoSo: 'HCM',
    thoiGianMo: '2025-08-11T01:00:00Z',
    thoiGianDong: '2025-08-22T10:00:00Z',
    trangThai: 'DA_DONG',
  },
]

function slot(thu: number, tietBatDau: number, soTiet: number, phongHoc: string): ScheduleSlot {
  return { thu, tietBatDau, soTiet, phongHoc, tuanBatDau: 1, tuanKetThuc: 15 }
}

function offer(
  maMonHoc: string,
  tenMonHoc: string,
  soTinChi: number,
  nhom: string,
  tenGiangVien: string,
  siSo: [daDangKy: number, toiDa: number],
  lich: ScheduleSlot[],
  hinhThucHoc = 'TRUC_TIEP',
): ClassOffer {
  return {
    maLopHP: `${maMonHoc}-2026-1-HCM${nhom}`,
    maMonHoc,
    tenMonHoc,
    soTinChi,
    maHocKy: DEMO_MA_HOC_KY,
    maCoSoHost: 'HCM',
    maGiangVien: null,
    tenGiangVien,
    soLuongDaDangKy: siSo[0],
    soLuongToiDa: siSo[1],
    trangThai: 'MO',
    choPhepLienCoSo: hinhThucHoc === 'TRUC_TUYEN',
    hinhThucHoc,
    phienBanLich: 1,
    lich,
  }
}

export const DEMO_CLASSES: readonly ClassOffer[] = [
  // Đang đăng ký sẵn (khớp thời khoá biểu demo) — 17 tín chỉ.
  offer('BAS1158', 'Tiếng Anh (Course 2)', 4, '08', 'Đỗ Thị Hồng Sương', [38, 45], [
    slot(4, 1, 4, '2E15-Ngoại ngữ'),
    slot(6, 6, 4, '2E15-Ngoại ngữ'),
  ]),
  offer('BAS1203', 'Giải tích 2', 3, '01', 'Bùi Thái Thanh Danh', [52, 60], [slot(3, 6, 4, '2E27-2E27')]),
  offer('INT1339', 'Ngôn ngữ lập trình C++', 3, '01', 'Nguyễn Hồng Quân', [57, 60], [slot(2, 1, 4, '2B34-2B34')]),
  offer('INT1340', 'Nhập môn công nghệ phần mềm', 3, '01', 'Lê Hoàng Mai', [71, 80], [slot(5, 6, 4, 'Trực tuyến')], 'TRUC_TUYEN'),
  offer('INT1358', 'Toán rời rạc 1', 3, '02', 'Đỗ Như Lực', [44, 60], [slot(2, 6, 4, '2B25-2B25')]),
  offer('SKD1102', 'Kỹ năng làm việc nhóm', 1, '03', 'Phạm Thu Hà', [29, 40], [slot(7, 1, 3, '2A08-2A08')], 'KET_HOP'),

  // Lớp còn mở để thử từng nhánh kiểm tra.
  offer('INT1339', 'Ngôn ngữ lập trình C++', 3, '02', 'Trần Văn Hải', [31, 60], [slot(4, 6, 4, '2B34-2B34')]),
  offer('INT1341', 'Nhập môn trí tuệ nhân tạo', 3, '01', 'Nguyễn Thị Hoa', [48, 60], [slot(3, 1, 4, '1A201')]),
  offer('INT1342', 'Phân tích và thiết kế hệ thống thông tin', 3, '01', 'Vũ Minh Tuấn', [20, 60], [slot(2, 1, 4, '1A208')]),
  offer('INT13147', 'Thực tập cơ sở', 3, '01', 'Hoàng Văn Nam', [12, 40], [slot(7, 6, 4, '1A201')]),
  offer('INT13162', 'Lập trình với Python', 3, '04', 'Lý Thanh Bình', [55, 60], [slot(4, 6, 4, '1A105')]),
  offer('INT1450', 'Quản lý dự án phần mềm', 2, '01', 'Phan Đức Long', [40, 40], [slot(6, 1, 3, '1A201')]),
  offer('INT14148', 'Cơ sở dữ liệu phân tán', 3, '01', 'Đặng Quốc Việt', [33, 60], [slot(5, 1, 4, '1A201')]),
  offer('INT1313', 'Cơ sở dữ liệu', 3, '01', 'Đặng Quốc Việt', [30, 60], [slot(6, 1, 3, '1A105')]),
  offer('MAR1322', 'Marketing căn bản', 3, '01', 'Lê Hoàng Mai', [25, 50], [slot(3, 6, 3, '2B34-2B34')]),
]

/** Ghi danh ban đầu — đăng ký lúc mở đợt. */
export const DEMO_ENROLLED: readonly EnrolledCourse[] = [
  'BAS1158-2026-1-HCM08',
  'BAS1203-2026-1-HCM01',
  'INT1339-2026-1-HCM01',
  'INT1340-2026-1-HCM01',
  'INT1358-2026-1-HCM02',
  'SKD1102-2026-1-HCM03',
].map((maLopHP) => {
  const lop = DEMO_CLASSES.find((c) => c.maLopHP === maLopHP)!
  return {
    maLopHP,
    maMonHoc: lop.maMonHoc,
    tenMonHoc: lop.tenMonHoc,
    soTinChi: lop.soTinChi,
    trangThai: 'DA_DANG_KY',
    ngayDangKy: '2026-09-28T01:50:59Z',
  }
})

/** Môn tiên quyết (`MonHocTienQuyet`) của các lớp demo. */
/**
 * `MonHocTienQuyet` — một nguồn cho cả màn đăng ký (kiểm khi đăng ký) và màn
 * xem môn tiên quyết. Chỉ INT14148 có môn yêu cầu chưa đạt (INT1313 đã rớt),
 * nên các kịch bản đăng ký khác trong bảng đầu file vẫn giữ nguyên kết quả.
 */
export const PREREQUISITES: Readonly<Record<string, readonly string[]>> = {
  BAS1158: ['BAS1157'],
  BAS1159: ['BAS1158'],
  BAS1160: ['BAS1159'],
  BAS1203: ['BAS1202'],
  INT1155: ['INT1154'],
  INT1313: ['INT1155'],
  INT1339: ['INT1155'],
  INT1341: ['INT1155'],
  INT1342: ['BAS1210'],
  INT1358: ['BAS1201'],
  INT13147: ['INT1154'],
  INT13162: ['INT1155'],
  INT14148: ['INT1313', 'INT1155'],
  INT1450: ['INT1155'],
}

/** Kết quả tốt nhất đã công bố của sinh viên. INT1313 rớt → cần học lại. */
export const BEST_RESULTS: BestResults = {
  BAS1106: 'DAT',
  BAS1107: 'DAT',
  BAS1150: 'DAT',
  BAS1151: 'DAT',
  BAS1157: 'DAT',
  BAS1201: 'DAT',
  BAS1202: 'DAT',
  BAS1210: 'DAT',
  BAS1224: 'DAT',
  INT1154: 'DAT',
  INT1155: 'DAT',
  INT1313: 'KHONG_DAT',
  SKD1101: 'DAT',
}

/** [tên, số tín chỉ] của các môn KHÔNG có lớp mở kỳ này (đã học hoặc học sau). */
const COURSE_INFO: Readonly<Record<string, readonly [string, number]>> = {
  BAS1106: ['Giáo dục thể chất 1', 1],
  BAS1107: ['Giáo dục thể chất 2', 1],
  BAS1150: ['Triết học Mác - Lênin', 3],
  BAS1151: ['Kinh tế chính trị Mác - Lênin', 2],
  BAS1157: ['Tiếng Anh (Course 1)', 4],
  BAS1159: ['Tiếng Anh (Course 3)', 4],
  BAS1160: ['Tiếng Anh (Course 3 Plus)', 2],
  BAS1201: ['Đại số', 3],
  BAS1202: ['Giải tích 1', 3],
  BAS1210: ['Lý thuyết xác suất và thống kê', 3],
  BAS1224: ['Vật lý 1 và thí nghiệm', 4],
  INT1154: ['Tin học cơ sở 1', 2],
  INT1155: ['Tin học cơ sở 2', 2],
  INT1405: ['Các hệ thống phân tán', 3],
  INT1433: ['Lập trình mạng', 3],
  INT14150: ['Nhập môn khoa học dữ liệu', 3],
  SKD1101: ['Kỹ năng thuyết trình', 1],
}

/** Tên môn để báo lỗi tiên quyết dễ hiểu. */
export const COURSE_NAMES: Readonly<Record<string, string>> = Object.fromEntries(
  Object.entries(COURSE_INFO).map(([ma, [ten]]) => [ma, ten]),
)

const KHOA = {
  CB2: 'Khoa Cơ bản 2',
  CNTT2: 'Khoa Công nghệ thông tin 2',
  KNM: 'Trung tâm Kỹ năng mềm',
  QTKD2: 'Khoa Quản trị kinh doanh 2',
} as const

/** Khoa quản lý môn — `CourseSummary.maKhoa/tenKhoa` ở API thật. */
export function facultyOf(maMonHoc: string): { maKhoa: string; tenKhoa: string } {
  const maKhoa = khoaOf(maMonHoc)
  return { maKhoa, tenKhoa: KHOA[maKhoa] }
}

/** Khoa của môn theo tiền tố mã — đúng cho dữ liệu demo, API thật trả `maKhoa`. */
function khoaOf(maMonHoc: string): keyof typeof KHOA {
  if (maMonHoc.startsWith('INT')) return 'CNTT2'
  if (maMonHoc.startsWith('SKD')) return 'KNM'
  if (maMonHoc.startsWith('MAR')) return 'QTKD2'
  return 'CB2'
}

/** Danh mục môn (`GET /api/courses`) cho mọi môn có lớp mở. */
export const CATALOG: readonly CourseSummary[] = [
  ...new Map(DEMO_CLASSES.map((c) => [c.maMonHoc, c])).values(),
].map((c) => ({
  maMonHoc: c.maMonHoc,
  tenMonHoc: c.tenMonHoc,
  soTinChi: c.soTinChi,
  maKhoa: khoaOf(c.maMonHoc),
  tenKhoa: KHOA[khoaOf(c.maMonHoc)],
}))

/** [mã môn, học kỳ gợi ý] của CTĐT — MAR1322 cố ý không có (môn ngoài chương trình). */
const LO_TRINH: readonly (readonly [string, number])[] = [
  ['BAS1106', 1], ['BAS1150', 1], ['BAS1157', 1], ['BAS1201', 1], ['BAS1202', 1], ['INT1154', 1],
  ['BAS1107', 2], ['BAS1151', 2], ['BAS1210', 2], ['BAS1224', 2], ['INT1155', 2], ['INT1313', 2],
  ['SKD1101', 2],
  ['BAS1158', 3], ['BAS1203', 3], ['INT1339', 3], ['INT1340', 3], ['INT1341', 3], ['INT1358', 3],
  ['SKD1102', 3],
  ['INT1342', 4], ['INT13162', 4], ['INT14148', 4], ['INT1450', 4],
  ['INT13147', 5], ['INT1405', 5], ['INT1433', 5], ['INT14150', 5],
]

/**
 * Học kỳ thật ứng với kỳ thứ 1, 2, 3… của lộ trình — suy từ học kỳ nhập học,
 * bỏ kỳ hè. **API chưa trả**; bản demo gán sẵn cho khoá 2025.
 */
export const PLAN_TERMS: readonly string[] = [
  '2025-2026-HK1',
  '2025-2026-HK2',
  '2026-2027-HK1',
  '2026-2027-HK2',
  '2027-2028-HK1',
]

export const DEMO_PROGRAM: StudentProgram = {
  maCTDT: 'CNTT2026',
  tenCTDT: 'Công nghệ thông tin',
  // HK1 2025-26 là kỳ 1, HK2 là kỳ 2, kỳ hè không tính → HK1 2026-27 là kỳ 3.
  hocKyHienTai: 3,
  monHoc: LO_TRINH.map(([maMonHoc, hocKyGoiY]) => {
    const mon = CATALOG.find((c) => c.maMonHoc === maMonHoc)
    return {
      maMonHoc,
      tenMonHoc: mon?.tenMonHoc ?? COURSE_INFO[maMonHoc]?.[0] ?? maMonHoc,
      soTinChi: mon?.soTinChi ?? COURSE_INFO[maMonHoc]?.[1] ?? 0,
      hocKyGoiY,
      batBuoc: !maMonHoc.startsWith('SKD'),
    }
  }),
}
