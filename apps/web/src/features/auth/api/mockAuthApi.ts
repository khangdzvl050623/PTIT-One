import { ApiError } from '@/shared/api'

import type { Role, SessionUser } from '../model/types'
import { readCredential } from './mockCredentialStore'

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
  email: string | null
  emailDaXacMinh: boolean
}

/**
 * Sinh viên bắt đầu **chưa có email**: đó là trạng thái thật của tài khoản vừa
 * được cấp, và là điểm xuất phát của luồng A1 (kích hoạt → thêm email → xác
 * minh → mở khoá sửa hồ sơ). Để sẵn "đã xác minh" thì không thử được luồng đó.
 *
 * Giảng viên và Admin cơ sở để sẵn đã xác minh, cho khỏi màn nào cũng vướng.
 */
export const MOCK_ACCOUNTS: Readonly<Record<string, MockAccount>> = {
  B26DCCN001: {
    hoTen: 'Nguyễn Văn An',
    role: 'SINH_VIEN',
    entityId: 'B26DCCN001',
    homeCampus: 'HCM',
    email: null,
    emailDaXacMinh: false,
  },
  GVHCM001: {
    hoTen: 'Đặng Quốc Việt',
    role: 'GIANG_VIEN',
    entityId: 'GVHCM001',
    homeCampus: 'HCM',
    email: 'dangquocviet@ptithcm.edu.vn',
    emailDaXacMinh: true,
  },
  'admin.hcm': {
    hoTen: 'Quản trị Phòng Đào tạo (demo)',
    role: 'ADMIN_CO_SO',
    entityId: null,
    homeCampus: 'HCM',
    email: 'daotao.hcm@ptithcm.edu.vn',
    emailDaXacMinh: true,
  },
  'admin.master': {
    hoTen: 'Quản trị danh mục (demo)',
    role: 'ADMIN_MASTER',
    entityId: null,
    homeCampus: null,
    email: null,
    emailDaXacMinh: false,
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
  const ten = username.trim()
  /* Email đã thêm/xác minh ở phiên trước phải còn sau khi đăng xuất rồi vào
     lại — bản thật lưu ở DB, bản giả lưu ở store riêng. */
  const credential = readCredential(ten)
  const user: SessionUser = {
    username: ten,
    ...account,
    ...(credential
      ? { email: credential.email, emailDaXacMinh: credential.daXacMinh }
      : {}),
    expiresAt: new Date(now + SESSION_MS).toISOString(),
    accessExpiresAt: new Date(now + ACCESS_MS).toISOString(),
  }
  write(user)
  return user
}

/**
 * Vá vài trường của phiên đang mở. `mockCredentialApi` dùng sau khi xác minh
 * email: cờ `emailDaXacMinh` nằm trong phiên nên không vá thì giao diện vẫn
 * tưởng chưa xác minh cho tới lần đăng nhập sau.
 */
export function patchMockSession(patch: Partial<SessionUser>): void {
  const user = read()
  if (user) write({ ...user, ...patch })
}

/**
 * Phiên giả đang đăng nhập, hoặc `null`. Dùng cho các bản giả KHÁC cần biết
 * "mình là ai" — ở API thật server biết qua cookie, bản giả thì phải tự đọc.
 */
export function currentMockUser(): SessionUser | null {
  return read()
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
    if (!raw) return null
    const saved = JSON.parse(raw) as SessionUser
    /* Phiên lưu từ TRƯỚC khi thêm trường mới sẽ thiếu trường đó. Lấy nền từ
       MOCK_ACCOUNTS rồi phủ bản lưu lên: khoá nào bản lưu có thì bản lưu
       thắng, khoá nào thiếu thì lấy theo tài khoản mẫu. Không có bước này,
       người đang mở tab phải đăng nhập lại mới thấy tính năng mới. */
    const account = MOCK_ACCOUNTS[saved.username]
    return account ? { ...account, ...saved } : saved
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
