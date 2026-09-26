/**
 * CSRF kiểu double-submit: backend đặt cookie `XSRF-TOKEN` (đọc được bằng JS),
 * client gửi lại qua header `X-XSRF-TOKEN`.
 *
 * Cần vì trình duyệt tự đính cookie phiên vào mọi request — chỉ riêng cookie
 * không chứng minh được request do chính trang này phát ra.
 */

const COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_ENDPOINT = '/api/auth/csrf'

export const CSRF_HEADER = 'X-XSRF-TOKEN'

export function readCsrfToken(): string | null {
  const prefix = `${COOKIE_NAME}=`
  for (const part of document.cookie.split(';')) {
    const entry = part.trim()
    if (entry.startsWith(prefix)) {
      // Cookie được mã hoá URL khi giá trị có ký tự đặc biệt.
      return decodeURIComponent(entry.slice(prefix.length))
    }
  }
  return null
}

/**
 * Bảo đảm có token trước request ghi đầu tiên. Backend đổi token khi đăng nhập
 * nên mỗi request đọc lại cookie, không cache giá trị trong bộ nhớ.
 */
export async function ensureCsrfToken(): Promise<string | null> {
  const existing = readCsrfToken()
  if (existing) return existing

  await fetch(CSRF_ENDPOINT, { credentials: 'include' })
  return readCsrfToken()
}
