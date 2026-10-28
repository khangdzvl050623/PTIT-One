import type { ComposeTarget, DraftRef, PreviewCount } from './composeTypes'

/** Demo: đếm theo phạm vi cho giống thật, tạo/gửi luôn thành công. */
export function previewNotice(target: ComposeTarget): Promise<PreviewCount> {
  const count =
    target.phamVi === 'TOAN_TRUONG'
      ? { soSinhVien: 15200, soGiangVien: 640 }
      : target.phamVi === 'CO_SO'
        ? { soSinhVien: 8100, soGiangVien: 210 }
        : { soSinhVien: 45, soGiangVien: 0 }
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(count), 400)
  })
}

export function createDraft(): Promise<DraftRef> {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve({ maThongBao: 'mock-draft-0001' }), 400)
  })
}

export function sendDraft(maThongBao: string): Promise<DraftRef> {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve({ maThongBao }), 400)
  })
}
