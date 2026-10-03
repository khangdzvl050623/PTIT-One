import { NGUONG_DAT, tongKet } from '../lib/grading'
import type { StudentGrade } from '../types'

/**
 * Dữ liệu tạm để dựng giao diện — thay bằng `GET /api/me/grades` (bỏ trống
 * `maHocKy` thì API trả mọi học kỳ, kỳ mới nhất trước).
 */

const TEN_HOC_KY: Record<string, string> = {
  '2026-2027-HK1': 'Học kỳ 1 - Năm học 2026 - 2027',
  '2025-2026-HK3': 'Học kỳ 3 (hè) - Năm học 2025 - 2026',
  '2025-2026-HK2': 'Học kỳ 2 - Năm học 2025 - 2026',
  '2025-2026-HK1': 'Học kỳ 1 - Năm học 2025 - 2026',
}

type Row = [
  maMonHoc: string,
  tenMonHoc: string,
  soTinChi: number,
  lop: string,
  /** `null` = chưa công bố. */
  diem: [cc: number, gk: number, ck: number] | null,
  ngayCongBo?: string,
]

/* Tổng kết và kết quả tính như server (`GradePolicy`) — không gõ tay để khỏi lệch. */
function term(maHocKy: string, rows: Row[]): StudentGrade[] {
  const [y1, , k] = maHocKy.split('-')
  return rows.map(([maMonHoc, tenMonHoc, soTinChi, lop, diem, ngayCongBo]) => {
    const tk = diem ? tongKet(...diem) : null
    return {
      maHocKy,
      tenHocKy: TEN_HOC_KY[maHocKy] ?? maHocKy,
      maLopHP: `${maMonHoc}-${y1}-${k?.slice(2)}-${lop}`,
      maMonHoc,
      tenMonHoc,
      soTinChi,
      diemChuyenCan: diem?.[0] ?? null,
      diemGiuaKy: diem?.[1] ?? null,
      diemCuoiKy: diem?.[2] ?? null,
      diemTongKet: tk,
      ketQua: tk === null ? null : tk >= NGUONG_DAT ? 'DAT' : 'KHONG_DAT',
      daCongBo: diem !== null,
      ngayCongBo: diem ? (ngayCongBo ?? null) : null,
    }
  })
}

/** Kỳ mới nhất trước. Kỳ đang học: chưa môn nào công bố điểm. */
export const DEMO_GRADES: readonly StudentGrade[] = [
  ...term('2026-2027-HK1', [
    ['BAS1158', 'Tiếng Anh (Course 2)', 4, 'HCM08', null],
    ['BAS1203', 'Giải tích 2', 3, 'HCM01', null],
    ['INT1339', 'Ngôn ngữ lập trình C++', 3, 'HCM01', null],
    ['INT1340', 'Nhập môn công nghệ phần mềm', 3, 'HCM01', null],
    ['INT1358', 'Toán rời rạc 1', 3, 'HCM02', null],
    ['SKD1102', 'Kỹ năng làm việc nhóm', 1, 'HCM03', null],
  ]),
  // Học lại môn trượt kỳ trước — tích luỹ lấy lần điểm cao nhất.
  ...term('2025-2026-HK3', [
    ['BAS1224', 'Vật lý 1 và thí nghiệm', 4, 'HCM01', [8, 7, 6.5], '2026-08-14T09:00:00Z'],
  ]),
  ...term('2025-2026-HK2', [
    ['BAS1107', 'Giáo dục thể chất 2', 1, 'HCM04', [9, 8, 7], '2026-06-05T09:00:00Z'],
    ['BAS1151', 'Kinh tế chính trị Mác - Lênin', 2, 'HCM05', [8, 7, 6.5], '2026-06-08T09:00:00Z'],
    ['BAS1210', 'Lý thuyết xác suất và thống kê', 3, 'HCM02', [8, 7.5, 8], '2026-06-10T09:00:00Z'],
    ['BAS1224', 'Vật lý 1 và thí nghiệm', 4, 'HCM01', [7, 3, 3], '2026-06-10T09:00:00Z'],
    ['INT1155', 'Tin học cơ sở 2', 2, 'HCM01', [9, 8, 8.5], '2026-06-12T09:00:00Z'],
    ['SKD1101', 'Kỹ năng thuyết trình', 1, 'HCM08', [10, 9, 9], '2026-06-03T09:00:00Z'],
  ]),
  ...term('2025-2026-HK1', [
    ['BAS1106', 'Giáo dục thể chất 1', 1, 'HCM03', [9, 8, 8], '2026-01-16T09:00:00Z'],
    ['BAS1150', 'Triết học Mác - Lênin', 3, 'HCM01', [8, 7, 7], '2026-01-20T09:00:00Z'],
    ['BAS1157', 'Tiếng Anh (Course 1)', 4, 'HCM08', [9, 8, 8], '2026-01-19T09:00:00Z'],
    ['BAS1201', 'Đại số', 3, 'HCM02', [9, 7, 7.5], '2026-01-21T09:00:00Z'],
    ['BAS1202', 'Giải tích 1', 3, 'HCM02', [8, 6, 6], '2026-01-22T09:00:00Z'],
    ['INT1154', 'Tin học cơ sở 1', 2, 'HCM01', [10, 9, 8.5], '2026-01-15T09:00:00Z'],
  ]),
]
