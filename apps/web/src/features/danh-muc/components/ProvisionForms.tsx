import { useState } from 'react'
import type { ReactNode } from 'react'

import { ApiError } from '@/shared/api'
import { Icon, Select } from '@/shared/ui'

import * as api from '../api/directoryApi'
import type { Campus, Faculty, ProvisionResult, StudyProgram } from '../types'
import styles from './AccountDirectory.module.scss'

/**
 * Ràng buộc gõ vào ô mã — trùng `@Pattern` của `CreateStudentRequest` và
 * `CreateTeacherRequest`. Mã cũng **chính là tên đăng nhập**, nên chỉ chữ hoa
 * và số.
 */
const MA_PATTERN = /^[A-Z0-9]{4,20}$/
const HO_TEN_MAX = 150
const EMAIL_MAX = 254
const HOC_VI_MAX = 50

/**
 * ⚠️ CHỈ DEMO. Mật khẩu mặc định nhóm thống nhất dùng khi cấp tài khoản nhanh.
 * Xem `DemoInitialPassword` trong `../types`: API thật không nhận mật khẩu lúc
 * cấp hồ sơ, trường này phải gỡ hoặc được backend bổ sung trước khi nối thật.
 */
const MAT_KHAU_MAC_DINH = 'PTIT@123'
/** Theo `ActivateAccountRequest` của backend. */
const MAT_KHAU_MIN = 8
const MAT_KHAU_MAX = 128

/** `null` khi để trống — nghĩa là dùng đúng luồng mã kích hoạt. */
function chuanHoaMatKhau(raw: string): string | null {
  return raw === '' ? null : raw
}

/** Câu lỗi tại chỗ; `null` là hợp lệ. Trùng quy tắc của bản giả và của A0. */
function loiMatKhau(matKhau: string, tenDangNhap: string): string | null {
  if (matKhau === '') return null
  if (matKhau.length < MAT_KHAU_MIN || matKhau.length > MAT_KHAU_MAX) {
    return `Mật khẩu dài từ ${MAT_KHAU_MIN} đến ${MAT_KHAU_MAX} ký tự.`
  }
  if (tenDangNhap !== '' && matKhau.toLowerCase().includes(tenDangNhap.toLowerCase())) {
    return 'Mật khẩu không được chứa tên đăng nhập.'
  }
  return null
}

/** Gọi API, gom lỗi server thành câu hiển thị ngay trong form. */
function useSubmit<T>(onDone: (value: T) => void) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  async function run(action: () => Promise<T>) {
    setBusy(true)
    setError(null)
    try {
      onDone(await action())
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không cấp được. Vui lòng thử lại.')
    } finally {
      setBusy(false)
    }
  }
  return { busy, error, run }
}

interface FormShellProps {
  busy: boolean
  error: string | null
  submitLabel: string
  submitDisabled?: boolean
  /** Admin đang đặt sẵn mật khẩu — ghi chú ở đáy form phải nói đúng việc đó. */
  datSanMatKhau: boolean
  onSubmit: () => void
  children: ReactNode
}

/**
 * Khung form nhúng thẳng vào cột bên phải (trước đây là hộp thoại). Nhờ vậy
 * Admin vừa nhìn danh bạ vừa nhập, không phải đóng mở popup mỗi lần cấp.
 */
function FormShell(props: FormShellProps) {
  return (
    <div className={`${styles.form} ${styles.inlineForm}`}>
      {props.children}
      <p className={styles.hintBox}>
        Tạo trong <b>một giao dịch</b>: lỗi ở bước nào thì không để lại gì.{' '}
        {props.datSanMatKhau ? (
          <>
            Tài khoản <b>đăng nhập được ngay</b>, không có mã kích hoạt.
          </>
        ) : (
          <>
            Tài khoản <b>chưa có mật khẩu</b> tới khi kích hoạt, mã hạn 7 ngày.
          </>
        )}
      </p>
      {props.error ? (
        <p className={`${styles.notice} ${styles.error}`} role="alert">
          {props.error}
        </p>
      ) : null}
      <button
        type="button"
        className={`${styles.primary} ${styles.submit}`}
        onClick={props.onSubmit}
        disabled={props.busy || props.submitDisabled}
      >
        {props.busy ? 'Đang cấp…' : props.submitLabel}
      </button>
    </div>
  )
}

/**
 * ⚠️ CHỈ DEMO. Ô đặt sẵn mật khẩu, kèm nút điền mật khẩu mặc định.
 *
 * Bỏ trống là giữ đúng luồng hợp đồng: tài khoản chưa có mật khẩu, người dùng
 * tự đặt bằng mã kích hoạt. Điền vào là đi nhánh chỉ có ở bản giả — xem
 * `DemoInitialPassword` trong `../types`.
 */
function PasswordField(props: {
  value: string
  tenDangNhap: string
  onChange: (v: string) => void
}) {
  const [hien, setHien] = useState(false)
  const loi = loiMatKhau(props.value, props.tenDangNhap)

  return (
    <label>
      <span>Mật khẩu đặt sẵn (tuỳ chọn)</span>
      <span className={styles.passwordRow}>
        <span className={styles.passwordInput}>
          <input
            type={hien ? 'text' : 'password'}
            value={props.value}
            maxLength={MAT_KHAU_MAX}
            autoComplete="new-password"
            onChange={(e) => props.onChange(e.target.value)}
            placeholder="Bỏ trống để dùng mã kích hoạt"
            aria-invalid={loi !== null}
          />
          {/* Icon theo trạng thái hiện tại như màn đăng nhập: đang ẩn thì mắt gạch chéo. */}
          <button
            type="button"
            className={styles.reveal}
            onClick={() => setHien((v) => !v)}
            aria-label={hien ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            aria-pressed={hien}
          >
            <Icon name={hien ? 'eye' : 'eyeOff'} size="16px" />
          </button>
        </span>
        <button
          type="button"
          className={styles.ghost}
          onClick={() => {
            props.onChange(MAT_KHAU_MAC_DINH)
            setHien(true)
          }}
        >
          Dùng {MAT_KHAU_MAC_DINH}
        </button>
      </span>
      {loi ? (
        <small className={styles.fieldError}>{loi}</small>
      ) : (
        <small className={styles.sub}>
          Bỏ trống: dùng mã kích hoạt — <b>luồng chuẩn</b>. Đặt sẵn: nhắc người dùng đổi ngay sau
          lần đăng nhập đầu.
        </small>
      )}
    </label>
  )
}

/** Ô nhập email dùng chung — quyết định mã đi qua thư hay trao tay. */
function EmailField(props: { value: string; onChange: (v: string) => void }) {
  return (
    <label>
      <span>Email (tuỳ chọn)</span>
      <input
        type="email"
        value={props.value}
        maxLength={EMAIL_MAX}
        onChange={(e) => props.onChange(e.target.value)}
        placeholder="vidu@ptithcm.edu.vn"
      />
      <small className={styles.sub}>
        Có email: mã <b>chỉ gửi qua thư</b>, bạn không thấy mã. Bỏ trống: mã hiện{' '}
        <b>một lần duy nhất</b> để trao tay.
      </small>
    </label>
  )
}

/* --- Cấp hồ sơ sinh viên -------------------------------------------------- */

export interface StudentFormProps {
  campuses: readonly Campus[]
  programs: readonly StudyProgram[]
  onDone: (result: ProvisionResult) => void
}

export function StudentForm({ campuses, programs, onDone }: StudentFormProps) {
  const [maSinhVien, setMaSinhVien] = useState('')
  const [hoTen, setHoTen] = useState('')
  const [ngaySinh, setNgaySinh] = useState('')
  const [maCoSoNha, setMaCoSoNha] = useState(campuses[0]?.maCoSo ?? '')
  const [maCTDT, setMaCTDT] = useState(programs[0]?.maCTDT ?? '')
  const [email, setEmail] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const { busy, error, run } = useSubmit(onDone)

  const maHopLe = MA_PATTERN.test(maSinhVien)
  // Ngày sinh tuỳ chọn, nhưng đã nhập thì phải ở quá khứ (`@Past`).
  const ngayHopLe = ngaySinh === '' || ngaySinh < new Date().toISOString().slice(0, 10)
  const sanSang =
    maHopLe &&
    hoTen.trim() !== '' &&
    ngayHopLe &&
    maCoSoNha !== '' &&
    maCTDT !== '' &&
    loiMatKhau(matKhau, maSinhVien) === null

  function xoaTrang() {
    setMaSinhVien('')
    setHoTen('')
    setNgaySinh('')
    setEmail('')
    setMatKhau('')
  }

  return (
    <FormShell
      busy={busy}
      error={error}
      submitLabel="Cấp hồ sơ sinh viên"
      submitDisabled={!sanSang}
      datSanMatKhau={matKhau !== ''}
      onSubmit={() =>
        void run(async () => {
          const result = await api.createStudent({
            maSinhVien: maSinhVien.trim(),
            hoTen: hoTen.trim(),
            ngaySinh: ngaySinh || null,
            maCoSoNha,
            maCTDT,
            email: email.trim() || null,
            matKhauBanDau: chuanHoaMatKhau(matKhau),
          })
          // Form ở lại trên màn, nên phải tự dọn để cấp người kế tiếp.
          xoaTrang()
          return result
        })
      }
    >
      <div className={styles.formRow}>
        <label>
          <span>Mã sinh viên</span>
          <input
            value={maSinhVien}
            maxLength={20}
            autoCapitalize="characters"
            onChange={(e) => setMaSinhVien(e.target.value.toUpperCase())}
            placeholder="B26DCCN001"
            aria-invalid={maSinhVien !== '' && !maHopLe}
          />
          <small className={maSinhVien !== '' && !maHopLe ? styles.fieldError : styles.sub}>
            Cũng là tên đăng nhập · 4–20 ký tự, chỉ chữ hoa và số.
          </small>
        </label>
        <label>
          <span>Họ và tên</span>
          <input
            value={hoTen}
            maxLength={HO_TEN_MAX}
            onChange={(e) => setHoTen(e.target.value)}
            placeholder="Nguyễn Văn An"
          />
        </label>
      </div>

      <div className={styles.formRow}>
        <label>
          <span>Ngày sinh (tuỳ chọn)</span>
          <input type="date" value={ngaySinh} onChange={(e) => setNgaySinh(e.target.value)} />
          {ngayHopLe ? null : <small className={styles.fieldError}>Ngày sinh phải ở quá khứ.</small>}
        </label>
        <label>
          <span>Cơ sở nhà</span>
          <Select
            ariaLabel="Cơ sở nhà"
            value={maCoSoNha}
            options={campuses.map((c) => ({ value: c.maCoSo, label: c.tenCoSo }))}
            onChange={setMaCoSoNha}
          />
        </label>
      </div>

      <label>
        <span>Chương trình đào tạo</span>
        <Select
          ariaLabel="Chương trình đào tạo"
          value={maCTDT}
          options={programs.map((p) => ({
            value: p.maCTDT,
            label: `${p.tenCTDT} (${p.maCTDT} · ${p.tongTinChi} TC)`,
          }))}
          onChange={setMaCTDT}
        />
      </label>

      <EmailField value={email} onChange={setEmail} />
      <PasswordField value={matKhau} tenDangNhap={maSinhVien} onChange={setMatKhau} />
    </FormShell>
  )
}

/* --- Cấp hồ sơ giảng viên ------------------------------------------------- */

export interface TeacherFormProps {
  campuses: readonly Campus[]
  faculties: readonly Faculty[]
  onDone: (result: ProvisionResult) => void
}

export function TeacherForm({ campuses, faculties, onDone }: TeacherFormProps) {
  const [maGiangVien, setMaGiangVien] = useState('')
  const [hoTen, setHoTen] = useState('')
  const [maCoSo, setMaCoSo] = useState(campuses[0]?.maCoSo ?? '')
  const [maKhoa, setMaKhoa] = useState(faculties[0]?.maKhoa ?? '')
  const [hocVi, setHocVi] = useState('')
  const [email, setEmail] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const { busy, error, run } = useSubmit(onDone)

  const maHopLe = MA_PATTERN.test(maGiangVien)
  const sanSang =
    maHopLe &&
    hoTen.trim() !== '' &&
    maCoSo !== '' &&
    maKhoa !== '' &&
    loiMatKhau(matKhau, maGiangVien) === null

  function xoaTrang() {
    setMaGiangVien('')
    setHoTen('')
    setHocVi('')
    setEmail('')
    setMatKhau('')
  }

  return (
    <FormShell
      busy={busy}
      error={error}
      submitLabel="Cấp hồ sơ giảng viên"
      submitDisabled={!sanSang}
      datSanMatKhau={matKhau !== ''}
      onSubmit={() =>
        void run(async () => {
          const result = await api.createTeacher({
            maGiangVien: maGiangVien.trim(),
            hoTen: hoTen.trim(),
            maCoSo,
            maKhoa,
            hocVi: hocVi.trim() || null,
            email: email.trim() || null,
            matKhauBanDau: chuanHoaMatKhau(matKhau),
          })
          xoaTrang()
          return result
        })
      }
    >
      <div className={styles.formRow}>
        <label>
          <span>Mã giảng viên</span>
          <input
            value={maGiangVien}
            maxLength={20}
            autoCapitalize="characters"
            onChange={(e) => setMaGiangVien(e.target.value.toUpperCase())}
            placeholder="GVHCM001"
            aria-invalid={maGiangVien !== '' && !maHopLe}
          />
          <small className={maGiangVien !== '' && !maHopLe ? styles.fieldError : styles.sub}>
            Cũng là tên đăng nhập · 4–20 ký tự, chỉ chữ hoa và số.
          </small>
        </label>
        <label>
          <span>Họ và tên</span>
          <input
            value={hoTen}
            maxLength={HO_TEN_MAX}
            onChange={(e) => setHoTen(e.target.value)}
            placeholder="Đặng Quốc Việt"
          />
        </label>
      </div>

      <div className={styles.formRow}>
        <label>
          <span>Cơ sở</span>
          <Select
            ariaLabel="Cơ sở"
            value={maCoSo}
            options={campuses.map((c) => ({ value: c.maCoSo, label: c.tenCoSo }))}
            onChange={setMaCoSo}
          />
        </label>
        <label>
          <span>Khoa</span>
          <Select
            ariaLabel="Khoa"
            value={maKhoa}
            options={faculties.map((f) => ({ value: f.maKhoa, label: f.tenKhoa }))}
            onChange={setMaKhoa}
          />
        </label>
      </div>

      <label>
        <span>Học vị (tuỳ chọn)</span>
        <input
          value={hocVi}
          maxLength={HOC_VI_MAX}
          onChange={(e) => setHocVi(e.target.value)}
          placeholder="Thạc sĩ"
        />
      </label>

      <EmailField value={email} onChange={setEmail} />
      <PasswordField value={matKhau} tenDangNhap={maGiangVien} onChange={setMatKhau} />
    </FormShell>
  )
}
