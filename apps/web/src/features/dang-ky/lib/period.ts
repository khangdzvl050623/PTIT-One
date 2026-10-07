import type { EnrollmentPeriod } from '../types'

/** Trạng thái hiển thị — tính theo giờ thực, không chỉ theo cột `trangThai`. */
export type PeriodPhase = 'DANG_MO' | 'SAP_MO' | 'HET_GIO' | 'CHUA_MO' | 'DA_DONG'

export const PHASE_LABEL: Record<PeriodPhase, string> = {
  DANG_MO: 'Đang mở',
  SAP_MO: 'Sắp mở',
  HET_GIO: 'Đã hết giờ',
  CHUA_MO: 'Chưa mở',
  DA_DONG: 'Đã đóng',
}

/**
 * "Đang mở" cần CẢ `trangThai = DANG_MO` VÀ giờ hiện tại trong
 * [thoiGianMo, thoiGianDong] — đúng `EnrollmentPeriod.dangMo` ở backend.
 */
export function isOpen(period: EnrollmentPeriod, now = Date.now()): boolean {
  return phaseOf(period, now) === 'DANG_MO'
}

/**
 * - `DANG_MO` nhưng chưa tới giờ → **Sắp mở** (server sẽ tự cho đăng ký khi tới giờ).
 * - `DANG_MO` nhưng quá giờ đóng → **Đã hết giờ** (quên đóng — vẫn KHÔNG đăng ký được).
 */
export function phaseOf(period: EnrollmentPeriod, now = Date.now()): PeriodPhase {
  if (period.trangThai === 'DA_DONG') return 'DA_DONG'
  if (period.trangThai === 'CHUA_MO') return 'CHUA_MO'
  if (now < Date.parse(period.thoiGianMo)) return 'SAP_MO'
  if (now > Date.parse(period.thoiGianDong)) return 'HET_GIO'
  return 'DANG_MO'
}

/** Giờ Việt Nam bất kể máy người xem đặt múi giờ nào — lịch là của trường. */
export function vnDateTime(iso: string, withSeconds = false): string {
  return new Date(iso).toLocaleString('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...(withSeconds ? { second: '2-digit' } : {}),
  })
}
