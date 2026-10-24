import { apiFetch } from '@/shared/api'

import type { AuthoredNotice } from './authoredTypes'

/**
 * Bản do chính mình soạn — server lọc theo vai trò người gọi. Danh sách BE
 * trả nguyên (không phân trang); sửa/xoá/gửi nháp không thuộc màn này.
 */
export function fetchAuthored(): Promise<AuthoredNotice[]> {
  return apiFetch<AuthoredNotice[]>('/api/notifications')
}

/** Chi tiết kèm số người nhận mới nhất (nháp tính lại, đã gửi lấy số chốt). */
export function fetchAuthoredDetail(maThongBao: string): Promise<AuthoredNotice> {
  return apiFetch<AuthoredNotice>(`/api/notifications/${encodeURIComponent(maThongBao)}`)
}
