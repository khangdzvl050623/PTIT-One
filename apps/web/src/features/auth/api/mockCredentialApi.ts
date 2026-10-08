import { ApiError } from '@/shared/api'

import type { AccountEmail } from '../model/types'
import { currentMockUser, MOCK_ACCOUNTS, patchMockSession } from './mockAuthApi'
import { MA_DEMO, readCredential, writeCredential } from './mockCredentialStore'

/**
 * A1 giả: kích hoạt, email và xác minh, đổi mật khẩu, quên mật khẩu.
 *
 * Giữ đúng những ràng buộc khiến màn hình phải xử lý trạng thái, chứ không chỉ
 * trả `ok`:
 *
 * - đổi email là **mất** trạng thái đã xác minh, phải xác minh lại
 * - mã sai trả `400 EMAIL_CODE_INVALID`, không im lặng cho qua
 * - `forgot-password` và `activate/resend` luôn thành công, kể cả tài khoản
 *   không có — nếu bản giả báo "không có tài khoản" thì màn hình sẽ được dựng
 *   quanh một hành vi mà bản thật cố ý không có
 *
 * Mật khẩu hiện tại: nhập gì cũng đúng miễn không rỗng, như `mockAuthApi`.
 */

/** Đủ lâu để thấy trạng thái "Đang gửi…" khi thiết kế. */
const LATENCY_MS = 400

export async function activate(
  tenDangNhap: string,
  maKichHoat: string,
  matKhauMoi: string,
): Promise<void> {
  await delay()
  if (!MOCK_ACCOUNTS[tenDangNhap.trim()] || maKichHoat.trim().length < 4) {
    /* Cùng mã với backend: mã sai, hết hạn, đã dùng và tài khoản không kích
       hoạt được đều trả một lỗi — không nói rõ cái nào. */
    throw new ApiError(400, {
      code: 'ACTIVATION_INVALID',
      message: 'Mã kích hoạt không đúng hoặc đã hết hạn.',
    })
  }
  requirePassword(matKhauMoi)
}

export async function resendActivation(tenDangNhap: string): Promise<void> {
  await delay()
  requireNotBlank(tenDangNhap, 'Nhập tên đăng nhập.')
  /* Không kiểm gì thêm: tài khoản không có, đã kích hoạt, hay không có email
     đều trả cùng một kết quả — đó là điều giữ cho form không dò được tài khoản. */
}

export async function fetchEmail(): Promise<AccountEmail> {
  await delay()
  const username = requireSession()
  const saved = readCredential(username)
  if (saved) return { email: saved.email, daXacMinh: saved.daXacMinh }

  const account = MOCK_ACCOUNTS[username]
  return { email: account?.email ?? null, daXacMinh: account?.emailDaXacMinh ?? false }
}

export async function changeEmail(email: string, matKhauHienTai: string): Promise<AccountEmail> {
  await delay()
  const username = requireSession()
  if (matKhauHienTai.length === 0) {
    throw new ApiError(400, {
      code: 'PASSWORD_INCORRECT',
      message: 'Mật khẩu hiện tại không đúng.',
    })
  }
  // Đổi email là mất trạng thái đã xác minh — y như bản thật.
  return save(username, { email: email.trim(), daXacMinh: false, dangCho: true })
}

export async function resendEmailCode(): Promise<void> {
  await delay()
  const username = requireSession()
  const saved = await fetchEmail()
  if (!saved.email) {
    throw new ApiError(409, {
      code: 'EMAIL_NOT_SET',
      message: 'Chưa có email để gửi mã. Thêm email trước.',
    })
  }
  if (saved.daXacMinh) {
    throw new ApiError(409, {
      code: 'EMAIL_ALREADY_VERIFIED',
      message: 'Email này đã xác minh.',
    })
  }
  save(username, { email: saved.email, daXacMinh: false, dangCho: true })
}

export async function verifyEmail(maXacThuc: string): Promise<AccountEmail> {
  await delay()
  const username = requireSession()
  const current = await fetchEmail()
  if (!current.email) {
    throw new ApiError(409, { code: 'EMAIL_NOT_SET', message: 'Chưa có email để xác minh.' })
  }
  if (maXacThuc.trim() !== MA_DEMO) {
    throw new ApiError(400, {
      code: 'EMAIL_CODE_INVALID',
      message: 'Mã xác minh không đúng hoặc đã hết hạn.',
    })
  }
  return save(username, { email: current.email, daXacMinh: true, dangCho: false })
}

export async function changePassword(
  matKhauHienTai: string,
  matKhauMoi: string,
): Promise<void> {
  await delay()
  requireSession()
  if (matKhauHienTai.length === 0) {
    throw new ApiError(400, {
      code: 'PASSWORD_INCORRECT',
      message: 'Mật khẩu hiện tại không đúng.',
    })
  }
  if (matKhauMoi === matKhauHienTai) {
    throw new ApiError(400, {
      code: 'PASSWORD_UNCHANGED',
      message: 'Mật khẩu mới trùng mật khẩu hiện tại.',
    })
  }
}

export async function forgotPassword(tenDangNhap: string, email: string): Promise<void> {
  await delay()
  requireNotBlank(tenDangNhap, 'Nhập tên đăng nhập.')
  requireNotBlank(email, 'Nhập email.')
  /* Luôn thành công từ đây: email sai, tài khoản không có hay chưa xác minh
     đều cho cùng một kết quả. */
}

export async function resetPassword(
  tenDangNhap: string,
  maXacThuc: string,
  matKhauMoi: string,
): Promise<void> {
  await delay()
  requireNotBlank(tenDangNhap, 'Nhập tên đăng nhập.')
  requirePassword(matKhauMoi)
  if (maXacThuc.trim() !== MA_DEMO) {
    throw new ApiError(400, {
      code: 'RESET_CODE_INVALID',
      message: 'Mã khôi phục không đúng hoặc đã hết hạn.',
    })
  }
}

/**
 * Ghi store VÀ phiên đang mở: cờ `emailDaXacMinh` nằm trong `SessionUser`, nên
 * không đồng bộ thì banner nhắc xác minh và nút sửa hồ sơ vẫn tưởng chưa xong.
 */
function save(
  username: string,
  value: { email: string; daXacMinh: boolean; dangCho: boolean },
): AccountEmail {
  writeCredential(username, value)
  patchMockSession({ email: value.email, emailDaXacMinh: value.daXacMinh })
  return { email: value.email, daXacMinh: value.daXacMinh }
}

/** Khớp `@NotBlank` của các DTO — server chặn ở tầng validation, không vào service. */
function requireNotBlank(value: string, message: string): void {
  if (value.trim().length === 0) {
    throw new ApiError(400, { code: 'VALIDATION_ERROR', message })
  }
}

/** Khớp `@Size(min = 8, max = 128)` của `matKhauMoi`. */
function requirePassword(matKhauMoi: string): void {
  if (matKhauMoi.length < 8 || matKhauMoi.length > 128) {
    throw new ApiError(400, {
      code: 'VALIDATION_ERROR',
      message: 'Mật khẩu dài từ 8 đến 128 ký tự.',
    })
  }
}

function requireSession(): string {
  const user = currentMockUser()
  if (!user) {
    throw new ApiError(401, {
      code: 'AUTH_SESSION_INVALID',
      message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
    })
  }
  return user.username
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}
