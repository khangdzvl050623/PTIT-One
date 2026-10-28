import { apiFetch } from '@/shared/api'

import type { ComposeInput, ComposeTarget, DraftRef, PreviewCount } from './composeTypes'

/**
 * Luồng soạn: xem trước → tạo nháp → gửi luôn trong một lượt xác nhận. Không
 * để nháp lửng (nháp rồi quên gửi thì người nhận không thấy gì mà người soạn
 * tưởng đã gửi).
 *
 * Ô liên kết bỏ trống gửi `null`, không gửi chuỗi rỗng — chuỗi rỗng rớt
 * `@Pattern` của `SaveNotificationRequest` (`400 VALIDATION_ERROR`).
 */
function body(target: ComposeTarget, input: ComposeInput) {
  return {
    ...input,
    phamVi: target.phamVi,
    maCoSo: target.maCoSo,
    maLopHP: target.maLopHP,
    lienKet: input.lienKet || null,
  }
}

export function previewNotice(target: ComposeTarget, input: ComposeInput): Promise<PreviewCount> {
  return apiFetch<PreviewCount>('/api/notifications/preview', {
    method: 'POST',
    json: body(target, input),
  })
}

export function createDraft(target: ComposeTarget, input: ComposeInput): Promise<DraftRef> {
  return apiFetch<DraftRef>('/api/notifications', {
    method: 'POST',
    json: body(target, input),
  })
}

export function sendDraft(maThongBao: string): Promise<DraftRef> {
  return apiFetch<DraftRef>(`/api/notifications/${encodeURIComponent(maThongBao)}/send`, {
    method: 'POST',
  })
}
