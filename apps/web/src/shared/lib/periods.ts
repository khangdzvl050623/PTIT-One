/** Một dòng `KhungGioTiet`. */
export interface Period {
  soTiet: number
  gioBatDau: string
  gioKetThuc: string
}

/**
 * Khung giờ 12 tiết — trùng seed `KhungGioTiet` (07:00 → 19:50).
 *
 * Nằm ở `shared` vì cả thời khoá biểu sinh viên lẫn lịch dạy giảng viên đều
 * dựng lưới theo bảng này; để trong một feature thì feature kia phải chép lại
 * giờ từng tiết.
 *
 * ⚠️ API thật **đã trả sẵn** `gioBatDau`/`gioKetThuc` trong từng buổi học, nên
 * UI không phải tra bảng này để biết giờ — bảng chỉ để vẽ cột tiết và để dữ
 * liệu demo suy ra giờ cho khớp.
 */
export const PERIODS: readonly Period[] = [
  { soTiet: 1, gioBatDau: '07:00:00', gioKetThuc: '07:50:00' },
  { soTiet: 2, gioBatDau: '08:00:00', gioKetThuc: '08:50:00' },
  { soTiet: 3, gioBatDau: '09:00:00', gioKetThuc: '09:50:00' },
  { soTiet: 4, gioBatDau: '10:00:00', gioKetThuc: '10:50:00' },
  { soTiet: 5, gioBatDau: '11:00:00', gioKetThuc: '11:50:00' },
  { soTiet: 6, gioBatDau: '13:00:00', gioKetThuc: '13:50:00' },
  { soTiet: 7, gioBatDau: '14:00:00', gioKetThuc: '14:50:00' },
  { soTiet: 8, gioBatDau: '15:00:00', gioKetThuc: '15:50:00' },
  { soTiet: 9, gioBatDau: '16:00:00', gioKetThuc: '16:50:00' },
  { soTiet: 10, gioBatDau: '17:00:00', gioKetThuc: '17:50:00' },
  { soTiet: 11, gioBatDau: '18:00:00', gioKetThuc: '18:50:00' },
  { soTiet: 12, gioBatDau: '19:00:00', gioKetThuc: '19:50:00' },
]

/** Giờ bắt đầu tiết đầu và giờ kết thúc tiết cuối của một buổi `soTiet` tiết. */
export function khungGio(tietBatDau: number, soTiet: number): { gioBatDau: string; gioKetThuc: string } {
  const first = PERIODS[tietBatDau - 1]
  const last = PERIODS[tietBatDau + soTiet - 2]
  return { gioBatDau: first?.gioBatDau ?? '', gioKetThuc: last?.gioKetThuc ?? '' }
}
