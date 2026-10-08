/** Bốn vai trò, trùng enum `Role` của backend. */
export const ROLES = ['SINH_VIEN', 'GIANG_VIEN', 'ADMIN_CO_SO', 'ADMIN_MASTER'] as const

export type Role = (typeof ROLES)[number]

export const ROLE_LABELS: Record<Role, string> = {
  SINH_VIEN: 'Sinh viên',
  GIANG_VIEN: 'Giảng viên',
  ADMIN_CO_SO: 'Quản trị đào tạo',
  ADMIN_MASTER: 'Quản trị danh mục',
}

/**
 * Khớp `SessionUserResponse`. **Không có token** — token chỉ nằm trong cookie
 * `HttpOnly`, JS không đọc được và cũng không cần đọc.
 */
export interface SessionUser {
  username: string
  role: Role
  /** Mã SV hoặc mã GV; `null` với tài khoản quản trị. */
  entityId: string | null
  /** `null` với `ADMIN_MASTER` — Master không thuộc cơ sở nào. */
  homeCampus: string | null
  /**
   * Họ tên hiển thị. **Backend chưa trả** (`SessionUserResponse` thiếu, dù
   * `TaiKhoan.HoTen` có trong DB) — chỉ bản auth giả điền. Thiếu thì UI hiện
   * vai trò thay thế.
   */
  hoTen?: string | null
  /** Email trường cấp, đã lưu ở tài khoản; `null` khi chưa đặt. */
  email: string | null
  /**
   * Đã xác minh email hay chưa. Là **điều kiện** của `PUT /api/me/profile`
   * (`409 EMAIL_NOT_VERIFIED`), nên UI đọc cờ này để chặn sẵn biểu mẫu sửa hồ
   * sơ thay vì để người dùng gõ xong rồi mới bị từ chối.
   */
  emailDaXacMinh: boolean
  /** Hạn tuyệt đối của phiên, ISO-8601. */
  expiresAt: string
  /** Hạn access hiện tại; làm mới trước mốc này. */
  accessExpiresAt: string
}

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

/**
 * Khớp `AccountEmail` — email của tài khoản đang đăng nhập.
 *
 * Chưa xác minh thì **không tự khôi phục mật khẩu được**: mã khôi phục chỉ gửi
 * tới địa chỉ đã xác minh, nên người chưa xác minh phải nhờ Admin cấp lại.
 */
export interface AccountEmail {
  /** `null` khi tài khoản chưa đặt email bao giờ. */
  email: string | null
  daXacMinh: boolean
}
