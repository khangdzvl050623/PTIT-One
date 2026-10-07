import { apiFetch } from '@/shared/api'

import type {
  ClassRoster,
  GradeSheet,
  SaveGradeRow,
  TeachingClass,
  TeachingSchedule,
} from '../types'

/**
 * Lớp phụ trách (F05) và nhập điểm (F06).
 *
 * Giảng viên lấy từ JWT — không endpoint nào nhận mã giảng viên, nên không có
 * đường xem lớp của người khác. Quyền nhập điểm chặt hơn quyền xem: phải là
 * giảng viên **đang được phân công** lớp đó.
 */

export function teachingClasses(maHocKy?: string): Promise<TeachingClass[]> {
  const query = maHocKy ? `?maHocKy=${encodeURIComponent(maHocKy)}` : ''
  return apiFetch<TeachingClass[]>(`/api/me/teaching-classes${query}`)
}

/**
 * Danh sách sinh viên của lớp.
 *
 * ⚠️ `lop.soLuongDaDangKy` là **bộ đếm**, `sinhVien.length` là số dòng ghi
 * danh. Hai số phải bằng nhau — lệch là lỗi dữ liệu, đừng tự chọn một bên.
 */
export function classRoster(maLopHP: string): Promise<ClassRoster> {
  return apiFetch<ClassRoster>(`/api/classes/${encodeURIComponent(maLopHP)}/students`)
}

export function teachingSchedule(maHocKy: string): Promise<TeachingSchedule> {
  return apiFetch<TeachingSchedule>(
    `/api/me/teaching-schedule?maHocKy=${encodeURIComponent(maHocKy)}`,
  )
}

export function sheet(maLopHP: string): Promise<GradeSheet> {
  return apiFetch<GradeSheet>(`/api/classes/${encodeURIComponent(maLopHP)}/grades`)
}

/**
 * Lưu điểm nháp. Thân là `{ diem: [...] }`, mỗi dòng mang `version` của chính nó.
 *
 * Server so `version` để phát hiện hai người sửa cùng lúc và trả
 * `409 GRADE_VERSION_CONFLICT` — khi đó phải tải lại bảng, KHÔNG ghi đè.
 */
export function saveGrades(
  maLopHP: string,
  rows: readonly SaveGradeRow[],
): Promise<GradeSheet> {
  return apiFetch<GradeSheet>(`/api/classes/${encodeURIComponent(maLopHP)}/grades`, {
    method: 'PUT',
    json: { diem: rows },
  })
}

/**
 * Công bố cả lớp. Sau khi công bố vẫn sửa được (sinh viên thấy ngay) cho tới
 * khi Admin cơ sở khoá điểm — khoá là endpoint khác, của vai trò khác.
 */
export function publishGrades(maLopHP: string): Promise<GradeSheet> {
  return apiFetch<GradeSheet>(`/api/classes/${encodeURIComponent(maLopHP)}/grades/publish`, {
    method: 'POST',
  })
}

/** Chỉ có nghĩa ở bản giả. Dữ liệu thật không có gì để đặt lại. */
export function resetDemo(): void {
  /* Không làm gì — nút đặt lại chỉ hiện khi chạy bản giả. */
}
