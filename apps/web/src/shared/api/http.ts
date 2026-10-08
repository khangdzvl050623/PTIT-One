import { CSRF_HEADER, ensureCsrfToken } from './csrf'
import { toApiError } from './errors'

/**
 * Lớp gọi API dùng chung.
 *
 * Token nằm trong cookie `HttpOnly` nên ở đây không có chỗ nào đụng tới token;
 * việc duy nhất cần làm là `credentials: 'include'` và gửi header CSRF cho
 * request ghi.
 */

export interface ApiRequest extends Omit<RequestInit, 'body'> {
  /** Body dạng object — tự `JSON.stringify` và đặt `Content-Type`. */
  json?: unknown
  /**
   * Body dạng `multipart/form-data` — dùng khi tải file lên.
   *
   * **Không** đặt `Content-Type` cho loại body này: trình duyệt phải tự sinh,
   * vì header còn phải kèm `boundary` ngăn cách các phần. Đặt tay là server
   * không tách được phần nào ra phần nào.
   */
  form?: FormData
}

type Reauthenticator = () => Promise<boolean>

let reauthenticate: Reauthenticator | null = null

/**
 * Module `auth` đăng ký hàm làm mới phiên tại đây.
 *
 * Đăng ký ngược chiều như vậy để `shared` không import module nghiệp vụ —
 * `shared/api` không cần biết phiên được làm mới bằng cách nào.
 */
export function setReauthenticator(fn: Reauthenticator | null): void {
  reauthenticate = fn
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

/** Endpoint auth tự xác định phiên qua cookie; gặp 401 là kết luận thật, không thử làm mới. */
function isAuthEndpoint(path: string): boolean {
  return path.startsWith('/api/auth/')
}

/**
 * Như {@link apiFetch} nhưng trả kèm mã trạng thái.
 *
 * Cần khi 200 và 201 có nghĩa khác nhau — ví dụ `POST /api/me/enrollments`:
 * 201 là vừa đăng ký, 200 là bấm lại đúng lớp đang giữ.
 */
export async function apiRequest<T>(
  path: string,
  request: ApiRequest = {},
): Promise<{ data: T; status: number }> {
  let response = await send(path, request)

  if (response.status === 401 && reauthenticate && !isAuthEndpoint(path)) {
    /* Access hết hạn. 401 nghĩa là server CHƯA xử lý gì, nên thử lại an toàn
       kể cả với POST. Chỉ thử đúng một lần — thất bại lần hai là phiên đã mất,
       không phải access hết hạn. */
    const renewed = await reauthenticate()
    if (renewed) {
      response = await send(path, request)
    }
  }

  return { data: await parse<T>(response, path), status: response.status }
}

export async function apiFetch<T>(path: string, request: ApiRequest = {}): Promise<T> {
  return (await apiRequest<T>(path, request)).data
}

async function send(path: string, request: ApiRequest): Promise<Response> {
  const method = (request.method ?? 'GET').toUpperCase()
  const headers = new Headers(request.headers)
  headers.set('Accept', 'application/json')

  if (!SAFE_METHODS.has(method)) {
    const token = await ensureCsrfToken()
    if (token) headers.set(CSRF_HEADER, token)
  }

  let body: BodyInit | undefined
  if (request.json !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(request.json)
  } else if (request.form) {
    body = request.form
  }

  return fetch(path, {
    ...request,
    method,
    headers,
    body,
    // Bắt buộc: không có dòng này thì cookie phiên không được gửi kèm.
    credentials: 'include',
  })
}

async function parse<T>(response: Response, path: string): Promise<T> {
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  try {
    return (await response.json()) as T
  } catch {
    throw await toApiError(
      new Response(null, { status: 502, statusText: `Phản hồi không hợp lệ từ ${path}` }),
    )
  }
}
