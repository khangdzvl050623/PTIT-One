import { NGUONG_DAT, diemHe4, round, xepLoai } from '@/shared/lib'

import type { StudentGrade, TermSummary } from '../types'

/*
 * Tổng hợp bảng điểm theo học kỳ. Phần quy đổi thuần (thang 4, điểm chữ,
 * trọng số, ngưỡng đạt) nằm ở `shared/lib/grading` vì màn nhập điểm của giảng
 * viên (F06) cũng dùng.
 */

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
