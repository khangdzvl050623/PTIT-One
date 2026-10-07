import { khungGio } from '@/shared/lib'

import type { HinhThucHoc, Term, TimetableEntry } from '../types'

/**
 * Dữ liệu tạm để dựng giao diện — thay bằng `GET /api/terms` và
 * `GET /api/me/timetable?maHocKy=&tuan=` khi nối API.
 *
 * Khung giờ 12 tiết nằm ở `shared/lib/periods` — lịch dạy giảng viên dùng chung.
 */

/**
 * Mới nhất trước (ô chọn lấy phần tử đầu). Kỳ đang học chứa ngày hôm nay để
 * tuần mặc định có dữ liệu; kỳ tương lai chỉ hiện trên tiến trình học tập.
 */
export const DEMO_TERMS: readonly Term[] = [
  {
    maHocKy: '2026-2027-HK2',
    tenHocKy: 'Học kỳ 2 - Năm học 2026 - 2027',
    ngayBatDau: '2027-01-18',
    ngayKetThuc: '2027-05-30',
  },
  {
    maHocKy: '2026-2027-HK1',
    tenHocKy: 'Học kỳ 1 - Năm học 2026 - 2027',
    ngayBatDau: '2026-08-31',
    ngayKetThuc: '2026-12-20',
  },
  {
    maHocKy: '2025-2026-HK3',
    tenHocKy: 'Học kỳ 3 (hè) - Năm học 2025 - 2026',
    ngayBatDau: '2026-06-15',
    ngayKetThuc: '2026-08-09',
  },
  {
    maHocKy: '2025-2026-HK2',
    tenHocKy: 'Học kỳ 2 - Năm học 2025 - 2026',
    ngayBatDau: '2026-01-19',
    ngayKetThuc: '2026-05-31',
  },
  {
    maHocKy: '2025-2026-HK1',
    tenHocKy: 'Học kỳ 1 - Năm học 2025 - 2026',
    ngayBatDau: '2025-08-25',
    ngayKetThuc: '2026-01-11',
  },
]

interface Session {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  tenGiangVien: string
  hinhThucHoc: HinhThucHoc
  thu: number
  tietBatDau: number
  soTiet: number
  phongHoc: string
  tuanBatDau: number
  tuanKetThuc: number
}

/* Giờ suy từ khung tiết như server làm — không gõ tay để khỏi lệch. */
function entry(s: Session): TimetableEntry {
  return { ...s, ...khungGio(s.tietBatDau, s.soTiet) }
}

export const DEMO_TIMETABLES: Readonly<Record<string, readonly TimetableEntry[]>> = {
  '2026-2027-HK1': [
    entry({
      maLopHP: 'INT1339-2026-1-HCM01',
      maMonHoc: 'INT1339',
      tenMonHoc: 'Ngôn ngữ lập trình C++',
      tenGiangVien: 'Nguyễn Hồng Quân',
      hinhThucHoc: 'TRUC_TIEP',
      thu: 2,
      tietBatDau: 1,
      soTiet: 4,
      phongHoc: '2B34-2B34',
      tuanBatDau: 1,
      tuanKetThuc: 15,
    }),
    entry({
      maLopHP: 'INT1358-2026-1-HCM02',
      maMonHoc: 'INT1358',
      tenMonHoc: 'Toán rời rạc 1',
      tenGiangVien: 'Đỗ Như Lực',
      hinhThucHoc: 'TRUC_TIEP',
      thu: 2,
      tietBatDau: 6,
      soTiet: 4,
      phongHoc: '2B25-2B25',
      tuanBatDau: 1,
      tuanKetThuc: 12,
    }),
    entry({
      maLopHP: 'BAS1203-2026-1-HCM01',
      maMonHoc: 'BAS1203',
      tenMonHoc: 'Giải tích 2',
      tenGiangVien: 'Bùi Thái Thanh Danh',
      hinhThucHoc: 'TRUC_TIEP',
      thu: 3,
      tietBatDau: 6,
      soTiet: 4,
      phongHoc: '2E27-2E27',
      tuanBatDau: 1,
      tuanKetThuc: 15,
    }),
    entry({
      maLopHP: 'BAS1158-2026-1-HCM08',
      maMonHoc: 'BAS1158',
      tenMonHoc: 'Tiếng Anh (Course 2)',
      tenGiangVien: 'Đỗ Thị Hồng Sương',
      hinhThucHoc: 'TRUC_TIEP',
      thu: 4,
      tietBatDau: 1,
      soTiet: 4,
      phongHoc: '2E15-Ngoại ngữ',
      tuanBatDau: 1,
      tuanKetThuc: 15,
    }),
    entry({
      maLopHP: 'INT1340-2026-1-HCM01',
      maMonHoc: 'INT1340',
      tenMonHoc: 'Nhập môn công nghệ phần mềm',
      tenGiangVien: 'Lê Hoàng Mai',
      hinhThucHoc: 'TRUC_TUYEN',
      thu: 5,
      tietBatDau: 6,
      soTiet: 4,
      phongHoc: 'Trực tuyến',
      tuanBatDau: 1,
      tuanKetThuc: 10,
    }),
    entry({
      maLopHP: 'BAS1158-2026-1-HCM08',
      maMonHoc: 'BAS1158',
      tenMonHoc: 'Tiếng Anh (Course 2)',
      tenGiangVien: 'Đinh Nguyễn Thanh Nhàn',
      hinhThucHoc: 'TRUC_TIEP',
      thu: 6,
      tietBatDau: 6,
      soTiet: 4,
      phongHoc: '2E15-Ngoại ngữ',
      tuanBatDau: 1,
      tuanKetThuc: 15,
    }),
    // Chỉ từ tuần 6 — đổi tuần để thấy lịch thay đổi theo tuần.
    entry({
      maLopHP: 'SKD1102-2026-1-HCM03',
      maMonHoc: 'SKD1102',
      tenMonHoc: 'Kỹ năng làm việc nhóm',
      tenGiangVien: 'Phạm Thu Hà',
      hinhThucHoc: 'KET_HOP',
      thu: 7,
      tietBatDau: 1,
      soTiet: 3,
      phongHoc: '2A08-2A08',
      tuanBatDau: 6,
      tuanKetThuc: 10,
    }),
  ],
  '2025-2026-HK2': [
    entry({
      maLopHP: 'BAS1202-2025-2-HCM01',
      maMonHoc: 'BAS1202',
      tenMonHoc: 'Giải tích 1',
      tenGiangVien: 'Bùi Thái Thanh Danh',
      hinhThucHoc: 'TRUC_TIEP',
      thu: 3,
      tietBatDau: 1,
      soTiet: 4,
      phongHoc: '2B34-2B34',
      tuanBatDau: 1,
      tuanKetThuc: 15,
    }),
    entry({
      maLopHP: 'INT1154-2025-2-HCM02',
      maMonHoc: 'INT1154',
      tenMonHoc: 'Tin học cơ sở 1',
      tenGiangVien: 'Nguyễn Hồng Quân',
      hinhThucHoc: 'TRUC_TIEP',
      thu: 5,
      tietBatDau: 6,
      soTiet: 3,
      phongHoc: '2B25-2B25',
      tuanBatDau: 1,
      tuanKetThuc: 15,
    }),
  ],
}

const NO_ENTRIES: readonly TimetableEntry[] = []

/**
 * Buổi học cả học kỳ — thay cho `GET /api/me/timetable?maHocKy=`. Hàm cấp
 * module nên tham chiếu ổn định, `useMemo` ở component không tính lại thừa.
 */
export function demoTimetableOf(maHocKy: string): readonly TimetableEntry[] {
  return DEMO_TIMETABLES[maHocKy] ?? NO_ENTRIES
}
