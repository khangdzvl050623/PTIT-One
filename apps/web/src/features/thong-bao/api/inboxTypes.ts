/**
 * Hộp thư người nhận — mirror của `Inbox`/`InboxItem` bên backend (module
 * `notification`). Chỉ sinh viên và giảng viên có hộp thư; admin bị `403`.
 *
 * `daDoc` là của RIÊNG người đọc. Mở hộp thư không tự đánh dấu đã đọc — chỉ
 * `markRead` / `markAllRead` mới đổi.
 */
export interface InboxItem {
  maThongBao: string
  /** `SOAN` (soạn tay) · `TU_DONG` (hệ thống tự sinh). */
  loai: string
  /** `null` với thông báo soạn tay. Không hardcode danh sách ở UI. */
  suKien: string | null
  mucDo: string
  tieuDe: string
  noiDung: string
  /** Đường dẫn nội bộ (`/…`) hoặc `null`. Không bao giờ là link ngoài. */
  lienKet: string | null
  /** Vai trò người soạn; `null` là hệ thống tự sinh. */
  vaiTroNguoiGui: string | null
  ngayGui: string
  daDoc: boolean
  ngayDoc: string | null
}

export interface InboxPageData {
  /** Số chưa đọc của CẢ hộp thư (không chỉ trang đang xem). */
  soChuaDoc: number
  trang: number
  kichThuoc: number
  thongBao: InboxItem[]
}

export interface UnreadCount {
  soChuaDoc: number
}
