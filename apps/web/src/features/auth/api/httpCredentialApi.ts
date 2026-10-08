import { apiFetch } from '@/shared/api'

import type { AccountEmail } from '../model/types'

/**
 * Chín đường của A1: kích hoạt tài khoản, email và xác minh, đổi mật khẩu,
 * quên mật khẩu.
 *
 * Tách khỏi `httpAuthApi` (đăng nhập, phiên) vì đây là **thông tin đăng nhập**
 * chứ không phải phiên: hai nhóm đổi vì lý do khác nhau và màn hình dùng chúng
 * cũng khác nhau.
 *
 * `activate`, `resendActivation`, `forgotPassword`, `resetPassword` gọi được
 * khi CHƯA đăng nhập; bốn đường còn lại cần phiên.
 */

// --- Kích hoạt (chưa đăng nhập) --------------------------------------

/** Đặt mật khẩu đầu tiên bằng mã Admin Master cấp. Xong KHÔNG tự đăng nhập. */
export function activate(
  tenDangNhap: string,
  maKichHoat: string,
  matKhauMoi: string,
): Promise<void> {
  return apiFetch<void>('/api/auth/activate', {
    method: 'POST',
    json: { tenDangNhap, maKichHoat, matKhauMoi },
  })
}

/**
 * Xin gửi lại mã kích hoạt tới email Admin đã lưu.
 *
 * Luôn thành công kể cả khi tài khoản không có, đã kích hoạt, hay không có
 * email — để form này không thành công cụ dò tài khoản nào có thật.
 */
export function resendActivation(tenDangNhap: string): Promise<void> {
  return apiFetch<void>('/api/auth/activate/resend', {
    method: 'POST',
    json: { tenDangNhap },
  })
}

// --- Email và xác minh (cần phiên) -----------------------------------

export function fetchEmail(): Promise<AccountEmail> {
  return apiFetch<AccountEmail>('/api/auth/email')
}

/** Lưu email **chưa xác minh** và gửi mã 6 số tới chính địa chỉ đó. */
export function changeEmail(email: string, matKhauHienTai: string): Promise<AccountEmail> {
  return apiFetch<AccountEmail>('/api/auth/email', {
    method: 'PUT',
    json: { email, matKhauHienTai },
  })
}

/** Gửi lại mã tới email đang chờ; mã cũ mất hiệu lực. */
export function resendEmailCode(): Promise<void> {
  return apiFetch<void>('/api/auth/email/resend', { method: 'POST' })
}

export function verifyEmail(maXacThuc: string): Promise<AccountEmail> {
  return apiFetch<AccountEmail>('/api/auth/email/verify', {
    method: 'POST',
    json: { maXacThuc },
  })
}

// --- Mật khẩu ---------------------------------------------------------

/** Thu hồi MỌI phiên kể cả phiên này — gọi xong phải đăng nhập lại. */
export function changePassword(matKhauHienTai: string, matKhauMoi: string): Promise<void> {
  return apiFetch<void>('/api/auth/change-password', {
    method: 'POST',
    json: { matKhauHienTai, matKhauMoi },
  })
}

/**
 * Xin mã khôi phục. `email` chỉ để ĐỐI CHIẾU với email đã lưu — mã không bao
 * giờ gửi tới địa chỉ gõ ở đây, mà tới email đã xác minh của tài khoản.
 *
 * Luôn thành công, vì lý do như {@link resendActivation}.
 */
export function forgotPassword(tenDangNhap: string, email: string): Promise<void> {
  return apiFetch<void>('/api/auth/forgot-password', {
    method: 'POST',
    json: { tenDangNhap, email },
  })
}

/** Đặt lại mật khẩu bằng mã khôi phục. Thu hồi mọi phiên đang mở. */
export function resetPassword(
  tenDangNhap: string,
  maXacThuc: string,
  matKhauMoi: string,
): Promise<void> {
  return apiFetch<void>('/api/auth/reset-password', {
    method: 'POST',
    json: { tenDangNhap, maXacThuc, matKhauMoi },
  })
}
