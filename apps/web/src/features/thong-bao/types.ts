export interface Notice {
  id: string
  title: string
  /** Chuỗi hiển thị sẵn (dd/mm/yyyy hh:mm) theo định dạng cổng thông tin. */
  publishedAt: string
  isNew: boolean
  /** Trích đoạn ngắn, chỉ dùng cho thông báo tiêu điểm. */
  excerpt?: string
  /** Đích đến của thông báo; tạm để `#` cho tới khi có route chi tiết. */
  href: string
}
