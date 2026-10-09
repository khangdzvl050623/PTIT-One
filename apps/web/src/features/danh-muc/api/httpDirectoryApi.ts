import { apiFetch } from '@/shared/api'

import type {
  AccountEmailState,
  AccountSummary,
  ActivationCode,
  CreateStudentInput,
  CreateTeacherInput,
  LoaiNguoiDung,
  ProvisionResult,
  StudentProfile,
  Teacher,
  TrangThaiTaiKhoan,
} from '../types'

/**
 * Cấp hồ sơ và quản trị tài khoản (F02). **Chỉ `ADMIN_MASTER`** — danh bạ là
 * bảng Master sở hữu (B3, chốt 02/10/2026). Admin cơ sở gọi sẽ nhận `403`.
 *
 * ⚠️ `matKhauBanDau` của giao diện KHÔNG có ở API thật và sẽ luôn là `null`:
 * hệ thống thật không bao giờ phát mật khẩu ban đầu, người dùng tự đặt mật khẩu
 * bằng **mã kích hoạt**. Trường đó chỉ tồn tại trong bản giả để dựng UI.
 */

export function listAccounts(
  maCoSo?: string | null,
  loaiNguoiDung?: LoaiNguoiDung | null,
): Promise<AccountSummary[]> {
  const params = new URLSearchParams()
  if (maCoSo) params.set('maCoSo', maCoSo)
  if (loaiNguoiDung) params.set('loaiNguoiDung', loaiNguoiDung)
  const query = params.toString()
  return apiFetch<AccountSummary[]>(`/api/accounts${query ? `?${query}` : ''}`)
}

/**
 * Cấp lại mã kích hoạt. Mã cũ bị thu hồi.
 *
 * `guiEmail = false` để Admin nhận mã trao tay dù tài khoản có email — dùng khi
 * thư không tới được.
 */
export function reissueActivationCode(
  tenDangNhap: string,
  guiEmail = true,
): Promise<ActivationCode> {
  return apiFetch<ActivationCode>(
    `/api/accounts/${encodeURIComponent(tenDangNhap)}/activation-code?guiEmail=${guiEmail}`,
    { method: 'POST' },
  )
}

/**
 * Admin đặt email mới cho tài khoản — dùng khi người dùng mất quyền vào hòm
 * thư cũ nên không tự đổi được (tự đổi cần mật khẩu hiện tại).
 *
 * Email mới luôn ở trạng thái **chưa xác minh**; nó tự thành đã xác minh khi
 * chủ tài khoản dùng được mã của lần cấp lại mật khẩu gửi tới đó.
 */
export function changeAccountEmail(
  tenDangNhap: string,
  email: string,
): Promise<AccountEmailState> {
  return apiFetch<AccountEmailState>(
    `/api/accounts/${encodeURIComponent(tenDangNhap)}/email`,
    { method: 'PUT', json: { email } },
  )
}

/**
 * Cấp lại mật khẩu: thu hồi mọi phiên, xoá mật khẩu, cấp mã dùng một lần.
 *
 * Admin **không** đặt mật khẩu hộ — chủ tài khoản tự đặt ở màn kích hoạt. Sau
 * thao tác này tài khoản KHÔNG đăng nhập được cho tới lúc đó.
 *
 * Có email đã lưu thì mã chỉ đi qua thư (`maKichHoat = null`); không thì mã
 * hiện một lần để trao tay.
 */
export function forcePasswordReset(tenDangNhap: string): Promise<ActivationCode> {
  return apiFetch<ActivationCode>(
    `/api/accounts/${encodeURIComponent(tenDangNhap)}/password-reset`,
    { method: 'POST' },
  )
}

export function changeStatus(
  tenDangNhap: string,
  trangThai: TrangThaiTaiKhoan,
): Promise<AccountSummary> {
  return apiFetch<AccountSummary>(
    `/api/accounts/${encodeURIComponent(tenDangNhap)}/status`,
    { method: 'PUT', json: { trangThai } },
  )
}

interface ProvisionedStudent {
  sinhVien: StudentProfile
  kichHoat: ActivationCode | null
}

interface ProvisionedTeacher {
  giangVien: Teacher
  kichHoat: ActivationCode | null
}

/**
 * `201` kèm mã kích hoạt — **lần duy nhất mã gốc rời khỏi server**. Mất thì
 * phải cấp lại. Có `email` và đã bật gửi thư thì mã chỉ đi qua thư và
 * `kichHoat.maKichHoat` là `null`.
 */
export async function createStudent(input: CreateStudentInput): Promise<ProvisionResult> {
  const { maSinhVien, hoTen, ngaySinh, maCoSoNha, maCTDT, email } = input
  const created = await apiFetch<ProvisionedStudent>('/api/students', {
    method: 'POST',
    json: { maSinhVien, hoTen, ngaySinh, maCoSoNha, maCTDT, email },
  })
  return {
    ma: created.sinhVien.maSinhVien,
    hoTen: created.sinhVien.hoTen,
    loai: 'SINH_VIEN',
    kichHoat: created.kichHoat,
    matKhauBanDau: null,
  }
}

export async function createTeacher(input: CreateTeacherInput): Promise<ProvisionResult> {
  const { maGiangVien, hoTen, maCoSo, maKhoa, hocVi, email } = input
  const created = await apiFetch<ProvisionedTeacher>('/api/teachers', {
    method: 'POST',
    json: { maGiangVien, hoTen, maCoSo, maKhoa, hocVi, email },
  })
  return {
    ma: created.giangVien.maGiangVien,
    hoTen: created.giangVien.hoTen,
    loai: 'GIANG_VIEN',
    kichHoat: created.kichHoat,
    matKhauBanDau: null,
  }
}

/** Chỉ có nghĩa ở bản giả. */
export function resetDemo(): void {
  /* Không làm gì — nút đặt lại chỉ hiện khi chạy bản giả. */
}
