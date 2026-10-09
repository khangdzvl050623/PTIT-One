import { ApiError } from '@/shared/api'

import { CAMPUSES, DEMO_ACCOUNTS, EMAILS, FACULTIES, PROGRAMS } from '../data/demo'
import type {
  AccountEmailState,
  AccountSummary,
  ActivationCode,
  CreateStudentInput,
  CreateTeacherInput,
  LoaiNguoiDung,
  ProvisionResult,
  TrangThaiTaiKhoan,
} from '../types'

/**
 * Hồ sơ và tài khoản GIẢ (F02) — dựng UI khi chưa chạy backend. Kiểm theo đúng
 * thứ tự của `StudentProvisioningService` / `TeacherProvisioningService` /
 * `AccountService` và ném **cùng mã lỗi, cùng câu báo**:
 *
 *   mã hợp lệ → cơ sở có thật → CTĐT (hoặc khoa) có thật → chưa có hồ sơ
 *   → chưa có tài khoản trùng tên
 *
 * Trạng thái lưu localStorage để F5 không mất; `resetDemo()` trả về ban đầu.
 *
 * ⚠️ **MỘT CHỖ CỐ Ý LỆCH HỢP ĐỒNG**: `matKhauBanDau`. API thật không nhận mật
 * khẩu lúc cấp hồ sơ — tài khoản sinh ra chưa có mật khẩu và người dùng tự đặt
 * khi kích hoạt. Nhóm chọn thêm ô này cho nhanh lúc demo (quyết định
 * 03/10/2026). Trước khi bật `VITE_AUTH_MODE=api` phải gỡ nó khỏi form hoặc bổ
 * sung vào DTO ở `apps/api`. Xem `DemoInitialPassword` trong `../types`.
 *
 * Nối API thật: `GET /api/accounts`, `POST /api/students`, `POST /api/teachers`,
 * `POST /api/accounts/{tenDangNhap}/activation-code`,
 * `PUT /api/accounts/{tenDangNhap}/status`, `GET /api/faculties`, `/api/programs`.
 */

const STORAGE_KEY = 'ptitone:mock:ho-so-tai-khoan'
const LATENCY_MS = 350
/** Hạn mã kích hoạt — trùng 7 ngày của backend. */
const HAN_MA_MS = 7 * 24 * 60 * 60 * 1000

interface State {
  accounts: AccountSummary[]
  /** Email đã lưu theo tài khoản. Admin đổi được nên không thể là hằng số. */
  emails: Record<string, string>
}

function initial(): State {
  return { accounts: DEMO_ACCOUNTS.map((a) => ({ ...a })), emails: { ...EMAILS } }
}

/* localStorage có thể ném lỗi ở chế độ riêng tư — khi đó mỗi lần F5 là một phiên mới. */
function load(): State {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    /* Hợp nhất với initial(): bản lưu từ trước khi thêm `emails` sẽ thiếu khoá
       đó, và `state.emails[...]` sẽ ném. */
    return raw ? { ...initial(), ...(JSON.parse(raw) as Partial<State>) } : initial()
  } catch {
    return initial()
  }
}

function save(state: State): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {
    /* Bỏ qua: xem ghi chú ở load. */
  }
}

export function resetDemo(): void {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* Bỏ qua. */
  }
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}

/* --- Danh bạ -------------------------------------------------------------- */

/** `GET /api/accounts?maCoSo=&loaiNguoiDung=` — bỏ trống là không lọc. */
export async function listAccounts(
  maCoSo?: string | null,
  loaiNguoiDung?: LoaiNguoiDung | null,
): Promise<AccountSummary[]> {
  await delay()
  return load().accounts.filter(
    (a) =>
      (!maCoSo || a.maCoSo === maCoSo) && (!loaiNguoiDung || a.loaiNguoiDung === loaiNguoiDung),
  )
}

function requireAccount(state: State, tenDangNhap: string): AccountSummary {
  const account = state.accounts.find((a) => a.tenDangNhap === tenDangNhap)
  if (!account) {
    throw new ApiError(404, {
      code: 'ACCOUNT_NOT_FOUND',
      message: `Không tìm thấy tài khoản ${tenDangNhap}.`,
    })
  }
  return account
}

/**
 * `POST /api/accounts/{tenDangNhap}/activation-code`. Mã cũ mất hiệu lực.
 * `guiEmail=false`: Admin nhận mã trao tay dù tài khoản có email.
 */
export async function reissueActivationCode(
  tenDangNhap: string,
  guiEmail = true,
): Promise<ActivationCode> {
  await delay()
  const state = load()
  const account = requireAccount(state, tenDangNhap)
  if (account.loaiNguoiDung === 'ADMIN_MASTER') {
    throw new ApiError(409, {
      code: 'ACCOUNT_NOT_MANAGEABLE',
      message: 'Tài khoản Admin Master không kích hoạt bằng mã.',
    })
  }
  if (account.daKichHoat) {
    throw new ApiError(409, {
      code: 'ACCOUNT_ALREADY_ACTIVATED',
      message: `Tài khoản ${tenDangNhap} đã kích hoạt.`,
    })
  }
  /* Không có email đã lưu thì dù xin gửi thư vẫn ra mã trao tay — gửi vào hư
     không rồi báo "đã gửi" là cách chắc chắn làm Admin tưởng xong việc. */
  return maKichHoat(tenDangNhap, guiEmail ? (state.emails[tenDangNhap] ?? null) : null)
}

/**
 * `PUT /api/accounts/{tenDangNhap}/status`. Khoá thu hồi mọi phiên trong cùng
 * giao dịch; Admin Master không khoá được (`LoaiNguoiDung <> 'ADMIN_MASTER'`
 * ngay trong câu `UPDATE`).
 */
export async function changeStatus(
  tenDangNhap: string,
  trangThai: TrangThaiTaiKhoan,
): Promise<AccountSummary> {
  await delay()
  const state = load()
  const account = requireAccount(state, tenDangNhap)
  if (account.trangThai === trangThai) return account
  if (account.loaiNguoiDung === 'ADMIN_MASTER') {
    throw new ApiError(409, {
      code: 'ACCOUNT_NOT_MANAGEABLE',
      message: `Không đổi được trạng thái tài khoản ${tenDangNhap} (${account.loaiNguoiDung}, ${account.trangThai}).`,
    })
  }
  const next = { ...account, trangThai }
  state.accounts = state.accounts.map((a) => (a.tenDangNhap === tenDangNhap ? next : a))
  save(state)
  return next
}

/**
 * `PUT /api/accounts/{tenDangNhap}/email` — Admin đặt email mới cho người mất
 * quyền vào hòm thư cũ.
 *
 * Luôn trả `daXacMinh: false`: admin không xác minh hộ được. Xem ghi chú ở
 * `CredentialService.adminChangeEmail`.
 */
export async function changeAccountEmail(
  tenDangNhap: string,
  email: string,
): Promise<AccountEmailState> {
  await delay()
  const state = load()
  requireAccount(state, tenDangNhap)
  const normalized = email.trim().toLowerCase()
  state.emails = { ...state.emails, [tenDangNhap]: normalized }
  save(state)
  return { email: normalized, daXacMinh: false }
}

/**
 * `POST /api/accounts/{tenDangNhap}/password-reset`.
 *
 * Giữ hai hệ quả dễ bị quên khi dựng giao diện: tài khoản quay về **chưa kích
 * hoạt** (không đăng nhập được cho tới khi đặt mật khẩu mới), và mã chỉ hiện
 * cho Admin khi tài khoản KHÔNG có email.
 */
export async function forcePasswordReset(tenDangNhap: string): Promise<ActivationCode> {
  await delay()
  const state = load()
  const account = requireAccount(state, tenDangNhap)
  if (account.loaiNguoiDung === 'ADMIN_MASTER') {
    throw new ApiError(409, {
      code: 'ACCOUNT_NOT_MANAGEABLE',
      message: 'Tài khoản Admin Master không cấp lại mật khẩu qua đây.',
    })
  }
  if (account.trangThai !== 'HOAT_DONG') {
    throw new ApiError(409, {
      code: 'ACCOUNT_NOT_MANAGEABLE',
      message: `Tài khoản ${tenDangNhap} đang ${account.trangThai}. Mở lại trước khi cấp lại mật khẩu.`,
    })
  }
  state.accounts = state.accounts.map((a) =>
    a.tenDangNhap === tenDangNhap ? { ...a, daKichHoat: false } : a,
  )
  save(state)
  return maKichHoat(tenDangNhap, state.emails[tenDangNhap] ?? null)
}

/* --- Cấp hồ sơ ------------------------------------------------------------ */

/**
 * `POST /api/students` — hồ sơ + danh bạ + tài khoản **chưa có mật khẩu** trong
 * MỘT giao dịch. Vì vậy ở đây kiểm hết rồi mới ghi.
 */
export async function createStudent(input: CreateStudentInput): Promise<ProvisionResult> {
  await delay()
  const state = load()
  const ma = input.maSinhVien.trim()
  requireCampus(input.maCoSoNha)
  if (!PROGRAMS.some((p) => p.maCTDT === input.maCTDT)) {
    throw new ApiError(400, {
      code: 'PROGRAM_NOT_FOUND',
      message: `Không có chương trình đào tạo ${input.maCTDT}.`,
    })
  }
  requireNewAccount(state, ma, 'STUDENT_EXISTS', `Đã có hồ sơ sinh viên ${ma}.`)
  requireValidPassword(ma, input.matKhauBanDau)

  const datSanMatKhau = input.matKhauBanDau !== null
  state.accounts = [
    ...state.accounts,
    {
      tenDangNhap: ma,
      loaiNguoiDung: 'SINH_VIEN',
      maCoSo: input.maCoSoNha,
      maThucThe: ma,
      trangThai: 'HOAT_DONG',
      /* Đúng hợp đồng thì tài khoản mới CHƯA có mật khẩu và `daKichHoat` chỉ
         thành true sau khi người dùng kích hoạt. Nhánh đặt sẵn mật khẩu là
         phần thêm chỉ có ở bản giả. */
      daKichHoat: datSanMatKhau,
    },
  ]
  save(state)

  return {
    ma,
    hoTen: input.hoTen.trim(),
    loai: 'SINH_VIEN',
    kichHoat: datSanMatKhau ? null : maKichHoat(ma, input.email),
    matKhauBanDau: input.matKhauBanDau,
  }
}

/** `POST /api/teachers` — cùng một giao dịch như cấp hồ sơ sinh viên. */
export async function createTeacher(input: CreateTeacherInput): Promise<ProvisionResult> {
  await delay()
  const state = load()
  const ma = input.maGiangVien.trim()
  requireCampus(input.maCoSo)
  if (!FACULTIES.some((f) => f.maKhoa === input.maKhoa)) {
    throw new ApiError(400, { code: 'FACULTY_UNKNOWN', message: `Không có khoa ${input.maKhoa}.` })
  }
  requireNewAccount(state, ma, 'TEACHER_EXISTS', `Đã có hồ sơ giảng viên ${ma}.`)
  requireValidPassword(ma, input.matKhauBanDau)

  const datSanMatKhau = input.matKhauBanDau !== null
  state.accounts = [
    ...state.accounts,
    {
      tenDangNhap: ma,
      loaiNguoiDung: 'GIANG_VIEN',
      maCoSo: input.maCoSo,
      maThucThe: ma,
      trangThai: 'HOAT_DONG',
      daKichHoat: datSanMatKhau,
    },
  ]
  save(state)

  return {
    ma,
    hoTen: input.hoTen.trim(),
    loai: 'GIANG_VIEN',
    kichHoat: datSanMatKhau ? null : maKichHoat(ma, input.email),
    matKhauBanDau: input.matKhauBanDau,
  }
}

/* --- Nội bộ --------------------------------------------------------------- */

/**
 * ⚠️ CHỈ DEMO — API thật không nhận mật khẩu lúc cấp hồ sơ.
 *
 * Quy tắc lấy theo `ActivateAccountRequest` (8–128 ký tự) và hợp đồng A0
 * ("không chứa tên đăng nhập"), để nếu sau này backend có nhận thì không lệch.
 */
function requireValidPassword(tenDangNhap: string, matKhau: string | null): void {
  if (matKhau === null) return
  if (matKhau.length < 8 || matKhau.length > 128) {
    throw new ApiError(400, {
      code: 'VALIDATION_ERROR',
      message: 'Mật khẩu dài từ 8 đến 128 ký tự.',
    })
  }
  if (matKhau.toLowerCase().includes(tenDangNhap.toLowerCase())) {
    throw new ApiError(400, {
      code: 'VALIDATION_ERROR',
      message: 'Mật khẩu không được chứa tên đăng nhập.',
    })
  }
}

function requireCampus(maCoSo: string): void {
  if (!CAMPUSES.some((c) => c.maCoSo === maCoSo)) {
    throw new ApiError(400, { code: 'CAMPUS_NOT_FOUND', message: `Không có cơ sở ${maCoSo}.` })
  }
}

/** Trùng hồ sơ báo trước; trùng tên đăng nhập báo sau — như thứ tự ở server. */
function requireNewAccount(state: State, ma: string, code: string, message: string): void {
  const trung = state.accounts.find((a) => a.tenDangNhap === ma)
  if (!trung) return
  if (trung.maThucThe === ma) throw new ApiError(409, { code, message })
  throw new ApiError(409, {
    code: 'ACCOUNT_EXISTS',
    message: `Tên đăng nhập hoặc mã ${ma} đã có tài khoản.`,
  })
}

/**
 * Sinh mã `XXXX-XXXX-XXXX-XXXX`. Có email thì mã **chỉ** đi qua thư:
 * `maKichHoat = null` và Admin không bao giờ thấy mã — đó là điều khiến việc
 * kích hoạt được chứng minh người dùng sở hữu hòm thư.
 */
function maKichHoat(tenDangNhap: string, email: string | null): ActivationCode {
  const hetHan = new Date(Date.now() + HAN_MA_MS).toISOString()
  if (email) return { tenDangNhap, maKichHoat: null, hetHan, guiToiEmail: email }

  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  const nhom = Array.from({ length: 4 }, () =>
    Array.from(
      { length: 4 },
      () => chars[Math.floor(Math.random() * chars.length)],
    ).join(''),
  )
  return { tenDangNhap, maKichHoat: nhom.join('-'), hetHan, guiToiEmail: null }
}
