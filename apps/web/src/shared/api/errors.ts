/** Khớp `ApiError` của backend: `{ code, message, fieldErrors?, traceId? }`. */
export interface ApiErrorBody {
  code: string
  message: string
  fieldErrors?: Record<string, string>
  traceId?: string
}

/**
 * Lỗi API đã được phân giải. Luôn có `code` và `message` đọc được — kể cả khi
 * response không phải JSON (proxy chết, 502 trả HTML), để chỗ gọi không phải
 * tự đoán hình dạng lỗi.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors?: Record<string, string>
  readonly traceId?: string

  constructor(status: number, body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code
    this.fieldErrors = body.fieldErrors
    this.traceId = body.traceId
  }

  /** Chưa đăng nhập hoặc phiên đã mất hiệu lực. */
  get isUnauthenticated(): boolean {
    return this.status === 401
  }
}

const FALLBACK_MESSAGE = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/** Không tin server luôn trả đúng JSON lỗi — mạng hỏng thì body là gì cũng có. */
export async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as Partial<ApiErrorBody>
    if (body && typeof body.code === 'string' && typeof body.message === 'string') {
      return new ApiError(response.status, body as ApiErrorBody)
    }
  } catch {
    /* Body rỗng hoặc không phải JSON — rơi xuống thông báo chung bên dưới. */
  }
  return new ApiError(response.status, {
    code: `HTTP_${response.status}`,
    message: FALLBACK_MESSAGE,
  })
}
