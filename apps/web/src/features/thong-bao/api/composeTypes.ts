/**
 * Soạn thông báo — mirror `SaveNotificationRequest` và `RecipientCount` của
 * backend (module `notification`).
 *
 * Đích gửi tách riêng khỏi nội dung: giảng viên cứng `LOP_HOC_PHAN` theo lớp
 * đã chọn, admin chọn `TOAN_TRUONG` / `CO_SO` / `LOP_HOC_PHAN`.
 */
export interface ComposeTarget {
  phamVi: 'TOAN_TRUONG' | 'CO_SO' | 'LOP_HOC_PHAN'
  /** `null` với toàn trường; admin cơ sở luôn là cơ sở nhà mình. */
  maCoSo: string | null
  /** Chỉ có với phạm vi lớp. */
  maLopHP: string | null
}
export interface ComposeInput {
  tieuDe: string
  noiDung: string
  /** `THONG_THUONG` · `QUAN_TRONG`. */
  mucDo: string
  /** `SINH_VIEN` · `TAT_CA` (cả lớp, trừ chính người gửi). */
  doiTuong: string
  /** Rỗng hoặc đường dẫn nội bộ (`/…`). */
  lienKet: string
}

/** Số người nhận nếu gửi ngay — `POST /preview` không lưu gì. */
export interface PreviewCount {
  soSinhVien: number
  soGiangVien: number
}

/** Tối thiểu cần giữ sau khi tạo nháp để gọi `send`: mã bản nháp. */
export interface DraftRef {
  maThongBao: string
}

export const TIEU_DE_MAX = 200
export const NOI_DUNG_MAX = 4000
