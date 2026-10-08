/**
 * Email và mã xác minh của bản giả, lưu theo tên đăng nhập.
 *
 * Tách khỏi `mockAuthApi` và `mockCredentialApi` để hai file đó cùng đọc được
 * mà không tạo vòng import: store không import của ai.
 */

export interface MockCredential {
  email: string | null
  daXacMinh: boolean
  /** Email đang chờ xác minh có mã 6 số; `null` khi không chờ gì. */
  dangCho: boolean
}

const STORAGE_KEY = 'ptitone:mock:thong-tin-dang-nhap'

/**
 * Mã xác minh cố định của bản giả.
 *
 * Bản thật gửi mã ngẫu nhiên qua Brevo; không có thư thì mã ngẫu nhiên sẽ
 * khiến màn hình không thử được. Màn email hiện mã này ra khi `VITE_API_MODE`
 * là `mock`, cùng cách ô đăng nhập liệt kê tài khoản demo.
 */
export const MA_DEMO = '123456'

export function readCredential(username: string): MockCredential | null {
  return readAll()[username] ?? null
}

export function writeCredential(username: string, value: MockCredential): void {
  const all = readAll()
  all[username] = value
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  } catch {
    /* Chế độ riêng tư hoặc hết chỗ: mất khi F5, không làm hỏng màn hình. */
  }
}

function readAll(): Record<string, MockCredential> {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Record<string, MockCredential>) : {}
  } catch {
    return {}
  }
}
