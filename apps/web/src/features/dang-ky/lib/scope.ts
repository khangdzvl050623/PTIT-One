import type { BestResults, ClassOffer, CourseSummary, StudentProgram } from '../types'

/**
 * Phạm vi danh sách lớp mở — ô chọn phía trên bảng, như cổng gốc. Hệ thống
 * KHÔNG có "lớp sinh viên" (lớp hành chính); thay bằng kế hoạch học kỳ của CTĐT.
 */
export type Scope =
  | 'MON_HOC'
  | 'KE_HOACH'
  | 'CTDT'
  | 'CHUA_HOC'
  | 'HOC_LAI'
  | 'KHOA'
  | 'TAT_CA'

/** Phạm vi cần chọn thêm một giá trị (môn hoặc khoa). */
export const NEEDS_PARAM: ReadonlySet<Scope> = new Set(['MON_HOC', 'KHOA'])

export interface ScopeContext {
  program: StudentProgram | null
  catalog: ReadonlyMap<string, CourseSummary>
  results: BestResults
}

export function scopeOptions(program: StudentProgram | null): { value: Scope; label: string }[] {
  /* KE_HOACH cần biết sinh viên đang ở học kỳ thứ mấy. API thật CHƯA có số đó
     (hocKyHienTai = 0), nên ẩn hẳn mục này thay vì hiện một bộ lọc luôn rỗng —
     bộ lọc không ra kết quả nào trông như lỗi dữ liệu. Có số thật thì mục tự
     hiện lại, không phải sửa gì ở đây. */
  const coKeHoach = (program?.hocKyHienTai ?? 0) > 0
  const keHoach: { value: Scope; label: string }[] = coKeHoach
    ? [
        {
          value: 'KE_HOACH',
          label: `Môn theo kế hoạch học kỳ ${program?.hocKyHienTai} - ${program?.maCTDT}`,
        },
      ]
    : []
  return [
    { value: 'MON_HOC', label: 'Lọc theo môn học' },
    ...keHoach,
    { value: 'CTDT', label: 'Môn trong chương trình đào tạo kế hoạch' },
    { value: 'CHUA_HOC', label: 'Môn chưa học trong CTĐT kế hoạch' },
    { value: 'HOC_LAI', label: 'Môn sinh viên cần học lại (đã rớt)' },
    { value: 'KHOA', label: 'Lọc theo khoa quản lý môn học' },
    { value: 'TAT_CA', label: 'Tất cả môn mở trong học kỳ' },
  ]
}

/**
 * Lớp có thuộc phạm vi không. `param` là mã môn (`MON_HOC`) hoặc mã khoa
 * (`KHOA`); rỗng nghĩa là chưa chọn — hiện tất cả để người dùng thấy có gì.
 */
export function inScope(scope: Scope, param: string, lop: ClassOffer, ctx: ScopeContext): boolean {
  const trongCtdt = ctx.program?.monHoc.find((m) => m.maMonHoc === lop.maMonHoc)
  switch (scope) {
    case 'MON_HOC':
      return !param || lop.maMonHoc === param
    case 'KE_HOACH':
      return trongCtdt?.hocKyGoiY === ctx.program?.hocKyHienTai
    case 'CTDT':
      return trongCtdt !== undefined
    case 'CHUA_HOC':
      // Chưa có kết quả công bố nào — môn đang học dở trong kỳ cũng tính là chưa học.
      return trongCtdt !== undefined && ctx.results[lop.maMonHoc] === undefined
    case 'HOC_LAI':
      return ctx.results[lop.maMonHoc] === 'KHONG_DAT'
    case 'KHOA':
      return !param || ctx.catalog.get(lop.maMonHoc)?.maKhoa === param
    case 'TAT_CA':
      return true
  }
}

/** Giá trị cho ô chọn thứ hai, chỉ lấy từ các lớp đang mở. */
export function paramOptions(
  scope: Scope,
  classes: readonly ClassOffer[],
  catalog: ReadonlyMap<string, CourseSummary>,
): { value: string; label: string }[] {
  if (scope === 'MON_HOC') {
    const courses = new Map(classes.map((c) => [c.maMonHoc, c.tenMonHoc]))
    return [
      { value: '', label: '— Tất cả môn —' },
      ...[...courses].sort(([a], [b]) => a.localeCompare(b)).map(([ma, ten]) => ({
        value: ma,
        label: `${ma} - ${ten}`,
      })),
    ]
  }
  if (scope === 'KHOA') {
    const faculties = new Map<string, string>()
    for (const c of classes) {
      const mon = catalog.get(c.maMonHoc)
      if (mon) faculties.set(mon.maKhoa, mon.tenKhoa)
    }
    return [
      { value: '', label: '— Tất cả khoa —' },
      ...[...faculties]
        .sort(([, a], [, b]) => a.localeCompare(b, 'vi'))
        .map(([ma, ten]) => ({ value: ma, label: ten })),
    ]
  }
  return []
}
