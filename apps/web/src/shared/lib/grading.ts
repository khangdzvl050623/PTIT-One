/*
 * Quy đổi và xếp loại điểm — CHỈ phía giao diện. Backend mới trả điểm thang 10
 * và đạt/không đạt; thang 4, điểm chữ và xếp loại chưa có ở API. Bảng dưới
 * theo thang tín chỉ thường dùng (A+ … F) và mức xếp loại của quy chế đào tạo
 * tín chỉ — là GIẢ ĐỊNH demo, đổi quy chế thì sửa đúng file này.
 *
 * Nằm ở `shared` vì cả màn xem điểm của sinh viên (F07) lẫn màn nhập điểm của
 * giảng viên (F06) đều dùng; để trong một feature thì feature kia phải chép
 * lại trọng số, và hai bản sẽ lệch nhau lúc nào không biết.
 */

/** Trọng số trùng mặc định `GradePolicy` ở backend (cấu hình `ptitone.grade.*`). */
export const TRONG_SO = { chuyenCan: 0.1, giuaKy: 0.3, cuoiKy: 0.6 } as const

/** Ngưỡng đạt trùng `ptitone.grade.nguong-dat`. */
export const NGUONG_DAT = 4

/** Điểm thành phần hợp lệ: 0–10, tối đa 1 chữ số thập phân (`SaveGradesRequest`). */
export const DIEM_MIN = 0
export const DIEM_MAX = 10

/** [điểm 10 tối thiểu, điểm chữ, điểm 4] — xét từ trên xuống. */
const THANG_CHU: readonly (readonly [number, string, number])[] = [
  [9.0, 'A+', 4.0],
  [8.5, 'A', 3.7],
  [8.0, 'B+', 3.5],
  [7.0, 'B', 3.0],
  [6.5, 'C+', 2.5],
  [5.5, 'C', 2.0],
  [5.0, 'D+', 1.5],
  [4.0, 'D', 1.0],
  [0, 'F', 0],
]

/** [điểm TB hệ 4 tối thiểu, xếp loại]. */
const XEP_LOAI: readonly (readonly [number, string])[] = [
  [3.6, 'Xuất sắc'],
  [3.2, 'Giỏi'],
  [2.5, 'Khá'],
  [2.0, 'Trung bình'],
  [1.0, 'Yếu'],
  [0, 'Kém'],
]

function bac(diem10: number) {
  return THANG_CHU.find(([min]) => diem10 >= min) ?? THANG_CHU[THANG_CHU.length - 1]!
}

export function diemChu(diem10: number): string {
  return bac(diem10)[1]
}

export function diemHe4(diem10: number): number {
  return bac(diem10)[2]
}

export function xepLoai(tb4: number): string {
  return (XEP_LOAI.find(([min]) => tb4 >= min) ?? XEP_LOAI[XEP_LOAI.length - 1]!)[1]
}

/** Làm tròn nửa lên như `RoundingMode.HALF_UP` của backend. */
export function round(value: number, digits: number): number {
  const f = 10 ** digits
  return Math.round((value + Number.EPSILON) * f) / f
}

/**
 * Điểm tổng kết như `GradePolicy.tongKet`: thiếu thành phần nào thì `null` —
 * điểm thiếu KHÔNG phải 0.
 *
 * ⚠️ Chỉ để xem trước trên giao diện. Giá trị ghi vào CSDL do **server** tính;
 * client không gửi `diemTongKet` lên (`SaveGradesRequest` không có trường này).
 */
export function tongKet(cc: number | null, gk: number | null, ck: number | null): number | null {
  if (cc === null || gk === null || ck === null) return null
  /* Tính bằng số nguyên, KHÔNG dùng dấu phẩy động trực tiếp: trong JavaScript
     `9*0.1 + 8.5*0.3 + 6*0.6` ra 7.049999…, làm tròn thành 7.0, trong khi
     BigDecimal của backend ra đúng 7.05 rồi HALF_UP thành 7.1. Lệch đúng ở
     các mốc .x5 — và đó lại là mốc hay gặp nhất khi điểm có 1 chữ số thập
     phân. Điểm ≤ 1 chữ số thập phân, trọng số ≤ 2 chữ số, nên quy về phần
     nghìn là biểu diễn được chính xác. */
  const phanNghin =
    Math.round(cc * 10) * Math.round(TRONG_SO.chuyenCan * 100) +
    Math.round(gk * 10) * Math.round(TRONG_SO.giuaKy * 100) +
    Math.round(ck * 10) * Math.round(TRONG_SO.cuoiKy * 100)
  // Làm tròn nửa LÊN về 1 chữ số thập phân; điểm không âm nên không cần xét dấu.
  return Math.floor((phanNghin + 50) / 100) / 10
}

/** `DAT` khi tổng kết ≥ ngưỡng; `null` khi chưa đủ điểm — khác với trượt. */
export function ketQuaCua(diemTongKet: number | null): 'DAT' | 'KHONG_DAT' | null {
  if (diemTongKet === null) return null
  return diemTongKet >= NGUONG_DAT ? 'DAT' : 'KHONG_DAT'
}
