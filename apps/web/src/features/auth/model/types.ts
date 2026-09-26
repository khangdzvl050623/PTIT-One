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
  /** Hạn tuyệt đối của phiên, ISO-8601. */
  expiresAt: string
  /** Hạn access hiện tại; làm mới trước mốc này. */
  accessExpiresAt: string
}

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'
