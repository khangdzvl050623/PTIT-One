import { apiFetch } from '@/shared/api'

import { GRADE_BUCKETS } from '../types'
import type { Campus, CourseReport, CourseRow, ReportSummary, ReportTerm } from '../types'

/**
 * Thống kê (`GET /api/reports/summary` và `/courses`).
 *
 * Phân quyền do SERVER làm: Admin Master xem mọi cơ sở, Admin cơ sở bị ép về
 * cơ sở mình và nhận `403 AUTH_FORBIDDEN` nếu truyền cơ sở khác. Tham số
 * `viewerCampus` chỉ có ở bản giả để mô phỏng chuyện đó — ở đây nhận để khớp
 * chữ ký rồi bỏ qua, KHÔNG kiểm lại phía client.
 */

/** Một dòng như server trả: `phanBoDiem` là mảng đối tượng, không phải mảng số. */
interface CourseStatResponse extends Omit<CourseRow, 'phanBoDiem'> {
  phanBoDiem: readonly { khoang: string; soLuong: number }[]
}

interface CourseReportResponse extends Omit<CourseReport, 'monHoc'> {
  monHoc: readonly CourseStatResponse[]
}

function query(maHocKy: string, maCoSo: string | null): string {
  const params = new URLSearchParams({ maHocKy })
  if (maCoSo) params.set('maCoSo', maCoSo)
  return params.toString()
}

/**
 * Đổi `phanBoDiem` từ `[{khoang, soLuong}]` sang mảng số theo thứ tự
 * {@link GRADE_BUCKETS}.
 *
 * Nhãn khoảng của server trùng từng ký tự với `GRADE_BUCKETS`, nên tra theo
 * nhãn chứ không theo chỉ số — nếu một ngày server đổi thứ tự thì cách này vẫn
 * đúng, còn lấy theo chỉ số thì sẽ lệch cột mà không báo gì.
 */
function toCounts(phanBo: readonly { khoang: string; soLuong: number }[]): number[] {
  const byLabel = new Map(phanBo.map((b) => [b.khoang, b.soLuong]))
  return GRADE_BUCKETS.map((label) => byLabel.get(label) ?? 0)
}

export async function getCourseReport(
  maHocKy: string,
  maCoSo: string | null,
  viewerCampus: string | null,
): Promise<CourseReport> {
  void viewerCampus
  const report = await apiFetch<CourseReportResponse>(
    `/api/reports/courses?${query(maHocKy, maCoSo)}`,
  )
  return {
    ...report,
    monHoc: report.monHoc.map((row) => ({ ...row, phanBoDiem: toCounts(row.phanBoDiem) })),
  }
}

export function getSummary(
  maHocKy: string,
  maCoSo: string | null,
  viewerCampus: string | null,
): Promise<ReportSummary> {
  void viewerCampus
  return apiFetch<ReportSummary>(`/api/reports/summary?${query(maHocKy, maCoSo)}`)
}

/** Học kỳ cho ô chọn — `GET /api/terms`, kỳ mới nhất trước. */
export async function listTerms(): Promise<readonly ReportTerm[]> {
  const terms = await apiFetch<readonly ReportTerm[]>('/api/terms')
  return [...terms].sort((a, b) => b.maHocKy.localeCompare(a.maHocKy))
}

/** Cơ sở cho ô chọn của Admin Master — `GET /api/campuses`. */
export function listCampuses(): Promise<readonly Campus[]> {
  return apiFetch<readonly Campus[]>('/api/campuses')
}
