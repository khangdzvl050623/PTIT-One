import type { StudentGrade, TermSummary } from '../types'

/*
 * Quy đổi và xếp loại — CHỈ phía giao diện. Backend mới trả điểm thang 10 và
 * đạt/không đạt; thang 4, điểm chữ, điểm trung bình và xếp loại chưa có ở API.
 * Bảng dưới theo thang tín chỉ thường dùng (A+ … F) và mức xếp loại của quy
 * chế đào tạo tín chỉ — là GIẢ ĐỊNH demo, đổi quy chế thì sửa đúng file này.
 */

/** Trọng số trùng mặc định `GradePolicy` ở backend (cấu hình `ptitone.grade.*`). */
export const TRONG_SO = { chuyenCan: 0.1, giuaKy: 0.3, cuoiKy: 0.6 } as const

/** Ngưỡng đạt trùng `ptitone.grade.nguong-dat`. */
export const NGUONG_DAT = 4

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

/** Điểm tổng kết như `GradePolicy.tongKet`: thiếu thành phần nào thì `null`. */
export function tongKet(cc: number | null, gk: number | null, ck: number | null): number | null {
  if (cc === null || gk === null || ck === null) return null
  return round(cc * TRONG_SO.chuyenCan + gk * TRONG_SO.giuaKy + ck * TRONG_SO.cuoiKy, 1)
}

function weighted<T extends StudentGrade>(grades: readonly T[], score: (g: T) => number) {
  const tc = grades.reduce((sum, g) => sum + g.soTinChi, 0)
  if (tc === 0) return null
  return round(grades.reduce((sum, g) => sum + score(g) * g.soTinChi, 0) / tc, 2)
}

const published = (g: StudentGrade): g is StudentGrade & { diemTongKet: number } =>
  g.daCongBo && g.diemTongKet !== null

/**
 * Gom theo học kỳ (mới nhất trước, như API) và tính:
 * - **Học kỳ**: mọi môn đã công bố trong kỳ, kể cả trượt, trọng số theo tín chỉ.
 * - **Tích luỹ** tới hết kỳ: mỗi môn lấy **lần điểm cao nhất** (học lại/cải
 *   thiện — đúng quy tắc backend), chỉ tính môn **đạt**.
 * Môn chưa công bố không vào bất kỳ con số nào.
 */
export function summarize(grades: readonly StudentGrade[]): TermSummary[] {
  const byTerm = new Map<string, StudentGrade[]>()
  for (const g of grades) {
    const list = byTerm.get(g.maHocKy) ?? []
    list.push(g)
    byTerm.set(g.maHocKy, list)
  }

  // Mã học kỳ dạng `yyyy-yyyy-HKn` nên so chuỗi là đúng thứ tự thời gian.
  const ascending = [...byTerm.keys()].sort()
  const best = new Map<string, StudentGrade & { diemTongKet: number }>()

  const summaries = ascending.map((maHocKy) => {
    const list = byTerm.get(maHocKy) ?? []
    const done = list.filter(published)
    for (const g of done) {
      const prev = best.get(g.maMonHoc)
      if (!prev || g.diemTongKet > prev.diemTongKet) best.set(g.maMonHoc, g)
    }
    const tichLuy = [...best.values()].filter((g) => g.diemTongKet >= NGUONG_DAT)
    const tb4 = weighted(done, (g) => diemHe4(g.diemTongKet))

    return {
      maHocKy,
      tenHocKy: list[0]?.tenHocKy ?? maHocKy,
      grades: list,
      tbHocKy10: weighted(done, (g) => g.diemTongKet),
      tbHocKy4: tb4,
      tinChiDatHocKy: done
        .filter((g) => g.diemTongKet >= NGUONG_DAT)
        .reduce((sum, g) => sum + g.soTinChi, 0),
      tbTichLuy10: weighted(tichLuy, (g) => g.diemTongKet),
      tbTichLuy4: weighted(tichLuy, (g) => diemHe4(g.diemTongKet)),
      tinChiTichLuy: tichLuy.reduce((sum, g) => sum + g.soTinChi, 0),
      xepLoaiHocKy: tb4 === null ? null : xepLoai(tb4),
    }
  })

  return summaries.reverse()
}
