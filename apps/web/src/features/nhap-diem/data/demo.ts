import { khungGio } from '@/shared/lib'

import type { GradeEntry, TeachingClass, TeachingScheduleEntry } from '../types'

/**
 * Dữ liệu tạm cho màn nhập điểm — giảng viên `GVHCM001` (Đặng Quốc Việt), cơ
 * sở HCM. Ba lớp học kỳ hiện tại trùng `DEMO_TEACHING` ở trang Thông tin; thêm
 * một lớp học kỳ trước **đã khoá** để thấy trạng thái chỉ đọc.
 *
 * Mỗi lớp dựng một tình huống khác nhau của luồng F06:
 *
 * | Lớp                     | Tình huống khi mở                         |
 * |-------------------------|-------------------------------------------|
 * | INT14148 · HCM01        | chưa nhập gì — nhập mới từ đầu            |
 * | INT1306 · HCM02         | thiếu điểm cuối kỳ → công bố bị `GRADE_INCOMPLETE` |
 * | INT1313 · HCM01         | đủ ba điểm → công bố được                 |
 * | INT1306 · HCM01 (kỳ trước) | `DA_KHOA` → chỉ đọc, không sửa được   |
 */

export const DEMO_MA_GIANG_VIEN = 'GVHCM001'
export const DEMO_TEN_GIANG_VIEN = 'Đặng Quốc Việt'
export const CAMPUS = 'HCM'

/** Học kỳ có lớp của giảng viên demo — thay bằng `GET /api/terms`. */
export const TERM_NAMES: Readonly<Record<string, string>> = {
  '2026-2027-HK1': 'Học kỳ 1 - Năm học 2026 - 2027',
  '2025-2026-HK2': 'Học kỳ 2 - Năm học 2025 - 2026',
}

/** `HocKy.NgayBatDau` — trùng `DEMO_TERMS` của feature `lich-hoc`. */
export const NGAY_BAT_DAU_HOC_KY: Readonly<Record<string, string>> = {
  '2026-2027-HK1': '2026-08-31',
  '2025-2026-HK2': '2026-01-19',
}

/** Mốc giãn ngày ghi danh của danh sách lớp — trùng giờ mở đợt ở màn đăng ký. */
export const NGAY_MO_DOT = '2026-09-28T01:00:00Z'

export const DEMO_MA_HOC_KY = '2026-2027-HK1'

/** Phần chung của mọi lớp demo — rút ra để bảng lớp bên dưới chỉ còn phần khác nhau. */
function lop(
  init: Pick<TeachingClass, 'maLopHP' | 'maMonHoc' | 'tenMonHoc' | 'maHocKy' | 'soLuongDaDangKy'> &
    Partial<TeachingClass>,
): TeachingClass {
  return {
    soTinChi: 3,
    maCoSoHost: CAMPUS,
    maGiangVien: DEMO_MA_GIANG_VIEN,
    tenGiangVien: DEMO_TEN_GIANG_VIEN,
    soLuongToiDa: 60,
    trangThai: 'MO',
    choPhepLienCoSo: false,
    hinhThucHoc: 'TRUC_TIEP',
    phienBanLich: 1,
    ...init,
  }
}

export const DEMO_CLASSES: readonly TeachingClass[] = [
  lop({
    maLopHP: 'INT1306-2026-1-HCM02',
    maMonHoc: 'INT1306',
    tenMonHoc: 'Cấu trúc dữ liệu và giải thuật',
    maHocKy: '2026-2027-HK1',
    soLuongDaDangKy: 52,
  }),
  lop({
    maLopHP: 'INT1313-2026-1-HCM01',
    maMonHoc: 'INT1313',
    tenMonHoc: 'Cơ sở dữ liệu',
    maHocKy: '2026-2027-HK1',
    soLuongDaDangKy: 30,
  }),
  lop({
    maLopHP: 'INT14148-2026-1-HCM01',
    maMonHoc: 'INT14148',
    tenMonHoc: 'Cơ sở dữ liệu phân tán',
    maHocKy: '2026-2027-HK1',
    soLuongDaDangKy: 33,
    hinhThucHoc: 'TRUC_TUYEN',
  }),
  lop({
    maLopHP: 'INT1306-2025-2-HCM01',
    maMonHoc: 'INT1306',
    tenMonHoc: 'Cấu trúc dữ liệu và giải thuật',
    maHocKy: '2025-2026-HK2',
    soLuongDaDangKy: 45,
    trangThai: 'DA_KHOA',
  }),
]

/* --- Lịch dạy ------------------------------------------------------------- */

interface Slot {
  maLopHP: string
  thu: number
  tietBatDau: number
  soTiet: number
  phongHoc: string
  tuanBatDau: number
  tuanKetThuc: number
}

/**
 * Buổi dạy của từng lớp — mỗi lớp một buổi/tuần.
 *
 * Bốn buổi cố ý **không** trùng khung giờ nhau: khi phân công giảng viên,
 * server từ chối nếu người đó đã dạy lớp khác cùng giờ, nên lịch dạy trùng là
 * dữ liệu hỏng chứ không phải tình huống cần dựng. (Lưới tuần vẫn tô vàng nếu
 * gặp, vì nó dùng chung với thời khoá biểu sinh viên — nơi trùng lịch có thật.)
 */
const SLOTS: readonly Slot[] = [
  { maLopHP: 'INT1306-2026-1-HCM02', thu: 2, tietBatDau: 6, soTiet: 4, phongHoc: '2A16', tuanBatDau: 1, tuanKetThuc: 15 },
  { maLopHP: 'INT1313-2026-1-HCM01', thu: 6, tietBatDau: 1, soTiet: 3, phongHoc: '1A105', tuanBatDau: 1, tuanKetThuc: 15 },
  { maLopHP: 'INT14148-2026-1-HCM01', thu: 5, tietBatDau: 1, soTiet: 4, phongHoc: 'Trực tuyến', tuanBatDau: 1, tuanKetThuc: 15 },
  { maLopHP: 'INT1306-2025-2-HCM01', thu: 3, tietBatDau: 1, soTiet: 4, phongHoc: '2A12', tuanBatDau: 1, tuanKetThuc: 15 },
]

/**
 * `buoiHoc` của `GET /api/me/teaching-schedule?maHocKy=`. Giờ suy từ khung
 * tiết như server làm — không gõ tay để khỏi lệch.
 */
export function teachingEntriesOf(maHocKy: string): TeachingScheduleEntry[] {
  return SLOTS.flatMap((slot) => {
    const lop = DEMO_CLASSES.find((c) => c.maLopHP === slot.maLopHP)
    // Lớp đã huỷ không lên lịch dạy — đúng như `ScheduleService`.
    if (!lop || lop.maHocKy !== maHocKy || lop.trangThai === 'DA_HUY') return []
    return [
      {
        maLopHP: lop.maLopHP,
        maMonHoc: lop.maMonHoc,
        tenMonHoc: lop.tenMonHoc,
        tenGiangVien: lop.tenGiangVien,
        hinhThucHoc: lop.hinhThucHoc as TeachingScheduleEntry['hinhThucHoc'],
        thu: slot.thu,
        tietBatDau: slot.tietBatDau,
        soTiet: slot.soTiet,
        phongHoc: slot.phongHoc,
        tuanBatDau: slot.tuanBatDau,
        tuanKetThuc: slot.tuanKetThuc,
        ...khungGio(slot.tietBatDau, slot.soTiet),
      },
    ]
  })
}

/* --- Sinh danh sách sinh viên tất định ------------------------------------ */

const HO = ['Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Huỳnh', 'Phan', 'Vũ', 'Võ', 'Đặng', 'Bùi', 'Đỗ']
const DEM = ['Văn', 'Thị', 'Minh', 'Hoàng', 'Ngọc', 'Quốc', 'Thanh', 'Gia', 'Đức', 'Bảo']
const TEN = ['An', 'Bình', 'Châu', 'Dũng', 'Giang', 'Hà', 'Hải', 'Khánh', 'Linh', 'Long',
  'Mai', 'Nam', 'Phúc', 'Quân', 'Sơn', 'Trang', 'Tú', 'Vy', 'Yến', 'Huy']

/** Cùng cách sinh mã/tên với danh sách lớp ở màn quản trị, để hai màn khớp nhau. */
export function roster(maLopHP: string, soLuong: number) {
  const seed = [...maLopHP].reduce((s, ch) => s + ch.charCodeAt(0), 0)
  return Array.from({ length: soLuong }, (_, i) => {
    const k = seed + i * 7
    return {
      maSinhVien: `B26DC${['CN', 'AT', 'DT', 'VT'][k % 4]}${String(100 + ((seed * 3 + i * 13) % 800)).padStart(3, '0')}`,
      hoTen: `${HO[k % HO.length]} ${DEM[(k >> 1) % DEM.length]} ${TEN[(k * 3) % TEN.length]}`,
    }
  }).sort((a, b) => a.maSinhVien.localeCompare(b.maSinhVien))
}

/** Điểm giả lập tất định trong khoảng 4.0–9.5, bước 0.5. */
function diem(maSinhVien: string, mon: number): number {
  const k = [...maSinhVien].reduce((s, ch) => s + ch.charCodeAt(0), 0) + mon * 31
  return 4 + (k % 12) * 0.5
}

const NGAY_CONG_BO_KY_TRUOC = '2026-06-18T03:00:00Z'

/**
 * Bảng điểm ban đầu của từng lớp. `version` bắt đầu từ `0` như dòng mới tạo ở
 * DB; mỗi lần lưu thành công server tăng lên 1.
 */
export function initialSheet(lopHP: TeachingClass): GradeEntry[] {
  return roster(lopHP.maLopHP, lopHP.soLuongDaDangKy).map((sv, i) => {
    const day = cach(lopHP.maLopHP)
    const cc = day.chuyenCan ? diem(sv.maSinhVien, 1) : null
    const gk = day.giuaKy ? diem(sv.maSinhVien, 2) : null
    // Lớp "thiếu cuối kỳ": vài sinh viên đã có, phần còn lại chưa — đúng cảnh dở dang.
    const ck = day.cuoiKy === 'DU' || (day.cuoiKy === 'MOT_PHAN' && i % 4 === 0)
      ? diem(sv.maSinhVien, 3)
      : null
    return {
      maSinhVien: sv.maSinhVien,
      hoTen: sv.hoTen,
      diemChuyenCan: cc,
      diemGiuaKy: gk,
      diemCuoiKy: ck,
      // Tổng kết và kết quả do server tính — mock tính lại khi trả bảng.
      diemTongKet: null,
      ketQua: null,
      version: 0,
      ngayCongBo: day.daCongBo ? NGAY_CONG_BO_KY_TRUOC : null,
    }
  })
}

/** Mức độ đã nhập của từng lớp demo. */
function cach(maLopHP: string): {
  chuyenCan: boolean
  giuaKy: boolean
  cuoiKy: 'DU' | 'MOT_PHAN' | 'CHUA'
  daCongBo: boolean
} {
  switch (maLopHP) {
    case 'INT1313-2026-1-HCM01':
      return { chuyenCan: true, giuaKy: true, cuoiKy: 'DU', daCongBo: false }
    case 'INT1306-2026-1-HCM02':
      return { chuyenCan: true, giuaKy: true, cuoiKy: 'MOT_PHAN', daCongBo: false }
    case 'INT1306-2025-2-HCM01':
      return { chuyenCan: true, giuaKy: true, cuoiKy: 'DU', daCongBo: true }
    default:
      return { chuyenCan: false, giuaKy: false, cuoiKy: 'CHUA', daCongBo: false }
  }
}
