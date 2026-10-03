import { ApiError } from '@/shared/api'

import type { Role, SessionUser } from '../model/types'

/**
 * Auth giả để dựng UI khi chưa chạy backend. Cùng chữ ký với `httpAuthApi`,
 * ném cùng `ApiError` như server — màn hình không phân biệt được hai bản.
 *
 * Tài khoản lấy theo seed `db/central/seed/10-auth-seed.sql`, mật khẩu nhập
 * gì cũng được (trừ để trống). Phiên lưu ở localStorage để F5 không mất.
 *
 * ⚠️ Chỉ dùng khi phát triển. Bật API thật: `VITE_API_MODE=api` — xem
 * `shared/api/mode.ts`.
 */

interface MockAccount {
  hoTen: string
  role: Role
  entityId: string | null
  homeCampus: string | null
}

export const MOCK_ACCOUNTS: Readonly<Record<string, MockAccount>> = {
  B26DCCN001: {
    hoTen: 'Nguyễn Văn An',
    role: 'SINH_VIEN',
    entityId: 'B26DCCN001',
    homeCampus: 'HCM',
  },
  GVHCM001: {
    hoTen: 'Trần Thị Bình',
    role: 'GIANG_VIEN',
    entityId: 'GVHCM001',
    homeCampus: 'HCM',
  },
  'admin.hcm': {
    hoTen: 'Quản trị Phòng Đào tạo (demo)',
    role: 'ADMIN_CO_SO',
    entityId: null,
    homeCampus: 'HCM',
  },
  'admin.master': {
    hoTen: 'Quản trị danh mục (demo)',
    role: 'ADMIN_MASTER',
    entityId: null,
    homeCampus: null,
  },
}

const STORAGE_KEY = 'ptitone:auth:mock-user'
/** Đủ lâu để thấy trạng thái "Đang đăng nhập…" khi thiết kế. */
const LATENCY_MS = 400
const SESSION_MS = 8 * 60 * 60 * 1000
/* Access dài bằng phiên: không có server thì làm mới định kỳ chỉ là tiếng ồn. */
const ACCESS_MS = SESSION_MS

export async function login(username: string, password: string): Promise<SessionUser> {
  await delay()
  const account = MOCK_ACCOUNTS[username.trim()]
  if (!account || password.length === 0) {
    /* Cùng mã và thông báo với backend (AuthenticationService.invalidCredentials). */
    throw new ApiError(401, {
      code: 'AUTH_INVALID_CREDENTIALS',
      message: 'Tên đăng nhập hoặc mật khẩu không đúng.',
    })
  }

  const now = Date.now()
  const user: SessionUser = {
    username: username.trim(),
    ...account,
    expiresAt: new Date(now + SESSION_MS).toISOString(),
    accessExpiresAt: new Date(now + ACCESS_MS).toISOString(),
  }
  write(user)
  return user
}

export async function fetchCurrentUser(): Promise<SessionUser> {
  await delay()
  const user = read()
  if (!user || Date.parse(user.expiresAt) <= Date.now()) {
    write(null)
    throw unauthenticated()
  }
  return user
}

/* Không có token để rotate: còn phiên thì trả lại đúng phiên đó. */
export const refreshSession = fetchCurrentUser

export async function logout(): Promise<void> {
  await delay()
  write(null)
}

export const logoutAll = logout

function unauthenticated(): ApiError {
  return new ApiError(401, {
    code: 'AUTH_UNAUTHENTICATED',
    message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  })
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}

/* localStorage có thể ném lỗi ở chế độ riêng tư — khi đó chỉ mất phiên khi F5. */
function read(): SessionUser | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as SessionUser) : null
  } catch {
    return null
  }
}

function write(user: SessionUser | null): void {
  try {
    if (user) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    else window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* Bỏ qua: xem ghi chú ở read. */
  }
}
