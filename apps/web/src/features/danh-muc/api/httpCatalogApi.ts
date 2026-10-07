import { apiFetch } from '@/shared/api'

import type { CourseRelation, CourseSummary } from '@/features/dang-ky/types'

/**
 * Danh mục môn học và tiên quyết (F03). **Chỉ `ADMIN_MASTER`** ghi — ở Phần 2
 * các bảng này được nhân bản một chiều từ Master nên site không ghi được, giới
 * hạn ở đây khớp sẵn với điều đó.
 *
 * ⚠️ API thật làm việc theo **từng thực thể**, không có "lưu cả danh mục một
 * lượt" như bản giả. Vì vậy mỗi thao tác là một lời gọi riêng, và màn hình tải
 * lại danh mục sau mỗi lần ghi — rẻ hơn nhiều so với tự suy đoán trạng thái mới
 * ở client rồi lệch với server mà không biết.
 */

export interface Catalog {
  courses: CourseSummary[]
  relations: CourseRelation[]
}

export async function loadCatalog(): Promise<Catalog> {
  const [courses, relations] = await Promise.all([
    apiFetch<CourseSummary[]>('/api/courses'),
    apiFetch<CourseRelation[]>('/api/prerequisites'),
  ])
  return { courses, relations }
}

export interface CourseInput {
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  maKhoa: string
}

/**
 * Thêm hoặc sửa một môn.
 *
 * @param moi {@code true} là thêm (`POST`), {@code false} là sửa (`PUT`).
 *            Mã môn KHÔNG đổi được khi sửa — nó là khoá chính.
 */
export function saveCourse(input: CourseInput, moi: boolean): Promise<unknown> {
  if (moi) {
    return apiFetch('/api/courses', { method: 'POST', json: input })
  }
  const { tenMonHoc, soTinChi, maKhoa } = input
  return apiFetch(`/api/courses/${encodeURIComponent(input.maMonHoc)}`, {
    method: 'PUT',
    json: { tenMonHoc, soTinChi, maKhoa },
  })
}

/**
 * Thay TOÀN BỘ tập tiên quyết của một môn. Danh sách rỗng = gỡ hết.
 *
 * Server chặn chu trình dài (`A → B → C → A`), và từ chối nếu môn có lớp trong
 * học kỳ đang mở đợt đăng ký. Lỗi thì tập cũ còn nguyên.
 */
export function setPrerequisites(maMonHoc: string, tienQuyet: string[]): Promise<unknown> {
  return apiFetch(`/api/courses/${encodeURIComponent(maMonHoc)}/prerequisites`, {
    method: 'PUT',
    json: { tienQuyet },
  })
}

/**
 * Xoá môn khỏi danh mục. Chỉ được khi môn chưa từng dùng ở đâu.
 *
 * Bị chặn thì `409` với một trong ba mã, mỗi mã nói rõ cái gì giữ môn lại:
 * `COURSE_IS_PREREQUISITE` · `COURSE_HAS_CLASSES` · `COURSE_IN_PROGRAM`.
 * Thông báo kèm theo viết sẵn cho người dùng đọc, hiện thẳng là được.
 */
export function deleteCourse(maMonHoc: string): Promise<void> {
  return apiFetch<void>(`/api/courses/${encodeURIComponent(maMonHoc)}`, { method: 'DELETE' })
}

/** Chỉ có nghĩa ở bản giả. Dữ liệu thật không có gì để đặt lại. */
export function resetCatalog(): void {
  /* Không làm gì — nút đặt lại chỉ hiện khi chạy bản giả. */
}
