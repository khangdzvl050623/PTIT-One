import { useState } from 'react'

import { ApiError } from '@/shared/api'
import { Dialog, Select } from '@/shared/ui'

import { removeAvatar, updateMyProfile, uploadAvatar } from '../api/profileApi'
import type { StudentProfile, UpdateMyProfileInput } from '../types'
import { IdPhoto } from './IdPhoto'
import styles from './ProfileEditDialog.module.scss'

/** Khớp `spring.servlet.multipart.max-file-size` của API. */
const MAX_MB = 5

/** Năm định dạng `ImageKind` của API nhận; trình duyệt nào cũng hiện được. */
const ACCEPT = 'image/jpeg,image/png,image/gif,image/webp,image/bmp'

export interface ProfileEditDialogProps {
  open: boolean
  onClose: () => void
  profile: StudentProfile
  /** Gọi sau khi lưu xong, kèm hồ sơ server vừa trả — cha dùng để hiện ngay. */
  onSaved: (profile: StudentProfile) => void
}

/** Chín ô lý lịch, đúng thân của `PUT /api/me/profile`. */
type Form = { [K in keyof UpdateMyProfileInput]: string }

const GIOI_TINH = [
  { value: '', label: '— Chưa chọn —' },
  { value: 'NAM', label: 'Nam' },
  { value: 'NU', label: 'Nữ' },
]

/**
 * Biểu mẫu sinh viên tự điền phần lý lịch và ảnh đại diện.
 *
 * Chỉ chín ô server nhận: họ tên, ngày sinh, cơ sở, chương trình và trạng thái
 * do Phòng Đào tạo quản nên **không** xuất hiện ở đây — đặt chúng vào form chỉ
 * để disabled sẽ khiến người dùng tưởng là sửa được ở đâu đó.
 *
 * Form gửi lại **cả chín ô** mỗi lần lưu vì endpoint thay toàn bộ phần lý lịch.
 * Xoá nội dung một ô rồi lưu là cách xoá dữ liệu cũ.
 *
 * ⚠️ Cha chỉ **mount** component này khi đang mở. Nhờ vậy state form khởi tạo
 * lại từ hồ sơ mới nhất mỗi lần mở, không cần effect đồng bộ — mà effect đó
 * cũng dễ reset form ngay giữa lúc người dùng đang gõ.
 */
export function ProfileEditDialog({ open, onClose, profile, onSaved }: ProfileEditDialogProps) {
  const [form, setForm] = useState<Form>(() => toForm(profile))
  const [anh, setAnh] = useState<string | null>(profile.anhDaiDien)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function set<K extends keyof Form>(field: K, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function fail(cause: unknown) {
    setError(
      cause instanceof ApiError ? cause.message : 'Không kết nối được tới máy chủ. Thử lại sau.',
    )
  }

  /**
   * Ảnh lưu NGAY khi chọn, không đợi bấm "Lưu thông tin": nó đi qua endpoint
   * riêng, và giữ file trong bộ nhớ chờ bấm Lưu sẽ khiến hai phần của cùng một
   * hộp thoại lưu ở hai thời điểm khác nhau — người dùng không đoán được.
   */
  async function pickFile(file: File) {
    setError(null)
    if (!file.type.startsWith('image/')) {
      setError('Tệp đã chọn không phải ảnh.')
      return
    }
    // Chặn sớm ở client cho đỡ phải tải lên rồi mới biết; server vẫn kiểm lại.
    if (file.size > MAX_MB * 1024 * 1024) {
      setError(`Ảnh quá lớn (${(file.size / 1024 / 1024).toFixed(1)}MB). Tối đa ${MAX_MB}MB.`)
      return
    }
    setUploading(true)
    try {
      const saved = await uploadAvatar(file)
      setAnh(saved.anhDaiDien)
      onSaved(saved)
    } catch (cause) {
      fail(cause)
    } finally {
      setUploading(false)
    }
  }

  async function dropFile() {
    setError(null)
    setUploading(true)
    try {
      const saved = await removeAvatar()
      setAnh(saved.anhDaiDien)
      onSaved(saved)
    } catch (cause) {
      fail(cause)
    } finally {
      setUploading(false)
    }
  }

  async function save() {
    setSaving(true)
    setError(null)
    try {
      onSaved(await updateMyProfile(toInput(form)))
      onClose()
    } catch (cause) {
      fail(cause)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      ariaLabel="Cập nhật thông tin cá nhân"
      header={<b>CẬP NHẬT THÔNG TIN CÁ NHÂN</b>}
      footer={
        <>
          <button type="button" className={styles.ghost} onClick={onClose}>
            Huỷ
          </button>
          <button
            type="button"
            className={styles.primary}
            onClick={() => void save()}
            disabled={saving}
          >
            {saving ? 'Đang lưu…' : 'Lưu thông tin'}
          </button>
        </>
      }
    >
      <div className={styles.form}>
        <p className={styles.note}>
          Phần hành chính (họ tên, ngày sinh, cơ sở, chương trình) do Phòng Đào tạo quản lý.
          Ô để trống sẽ <b>xoá</b> giá trị đang lưu.
        </p>

        <div className={styles.pair}>
          <label>
            Giới tính
            <Select
              ariaLabel="Giới tính"
              value={form.gioiTinh}
              options={GIOI_TINH}
              onChange={(value) => set('gioiTinh', value)}
            />
          </label>
          <label>
            Điện thoại
            <input
              value={form.dienThoai}
              onChange={(event) => set('dienThoai', event.target.value)}
              placeholder="Ví dụ: 0901234567"
              inputMode="tel"
            />
          </label>
        </div>

        <div className={styles.pair}>
          <label>
            Số CMND / CCCD
            <input
              value={form.soCCCD}
              onChange={(event) => set('soCCCD', event.target.value)}
              placeholder="9 hoặc 12 chữ số"
              inputMode="numeric"
            />
          </label>
          <label>
            Email cá nhân
            <input
              type="email"
              value={form.emailCaNhan}
              onChange={(event) => set('emailCaNhan', event.target.value)}
              placeholder="ngoài email trường cấp"
            />
          </label>
        </div>

        <div className={styles.pair}>
          <label>
            Nơi sinh
            <input
              value={form.noiSinh}
              onChange={(event) => set('noiSinh', event.target.value)}
              placeholder="Tỉnh / thành phố"
            />
          </label>
          <label>
            Dân tộc
            <input
              value={form.danToc}
              onChange={(event) => set('danToc', event.target.value)}
              placeholder="Ví dụ: Kinh"
            />
          </label>
        </div>

        <label>
          Tôn giáo
          <input
            value={form.tonGiao}
            onChange={(event) => set('tonGiao', event.target.value)}
            placeholder="Ví dụ: Không"
          />
        </label>

        <label>
          Hộ khẩu thường trú
          <input
            value={form.hoKhau}
            onChange={(event) => set('hoKhau', event.target.value)}
            placeholder="Số nhà, đường, phường, tỉnh/thành"
          />
        </label>

        <div className={styles.avatar}>
          <IdPhoto src={anh} hoTen={profile.hoTen} />
          <div className={styles.avatarActions}>
            <span className={styles.avatarLabel}>Ảnh đại diện</span>
            {/* Nút thật là <label>: input file gốc rất khó tạo kiểu, nên ẩn nó
                đi và để nhãn đóng vai nút — bấm vào nhãn vẫn mở hộp chọn file. */}
            <label className={styles.ghost}>
              {uploading ? 'Đang tải ảnh…' : 'Chọn ảnh…'}
              <input
                type="file"
                className={styles.fileInput}
                accept={ACCEPT}
                disabled={uploading || saving}
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  // Xoá giá trị để chọn LẠI đúng file vừa chọn vẫn kích hoạt onChange.
                  event.target.value = ''
                  if (file) void pickFile(file)
                }}
              />
            </label>
            {anh ? (
              <button
                type="button"
                className={styles.ghost}
                onClick={() => void dropFile()}
                disabled={uploading || saving}
              >
                Gỡ ảnh
              </button>
            ) : null}
            <p className={styles.note}>
              JPEG, PNG, GIF, WEBP hoặc BMP, tối đa {MAX_MB}MB. Ảnh lưu trên dịch vụ
              ảnh ngoài; hệ thống chỉ giữ liên kết.
            </p>
          </div>
        </div>

        {error ? (
          <p className={styles.formError} role="alert">
            {error}
          </p>
        ) : null}
      </div>
    </Dialog>
  )
}

/** `null` → `''`: ô trống của HTML là chuỗi rỗng, không có khái niệm null. */
function toForm(profile: StudentProfile): Form {
  return {
    gioiTinh: profile.gioiTinh ?? '',
    dienThoai: profile.dienThoai ?? '',
    soCCCD: profile.soCCCD ?? '',
    emailCaNhan: profile.emailCaNhan ?? '',
    noiSinh: profile.noiSinh ?? '',
    danToc: profile.danToc ?? '',
    tonGiao: profile.tonGiao ?? '',
    hoKhau: profile.hoKhau ?? '',
  }
}

/**
 * Gửi `null` cho ô trống. Server nhận cả `''` nhưng `null` nói rõ ý "bỏ trống"
 * hơn khi đọc payload trong tab Network.
 */
function toInput(form: Form): UpdateMyProfileInput {
  const value = (raw: string) => (raw.trim() ? raw.trim() : null)
  return {
    gioiTinh: form.gioiTinh === 'NAM' || form.gioiTinh === 'NU' ? form.gioiTinh : null,
    dienThoai: value(form.dienThoai),
    soCCCD: value(form.soCCCD),
    emailCaNhan: value(form.emailCaNhan),
    noiSinh: value(form.noiSinh),
    danToc: value(form.danToc),
    tonGiao: value(form.tonGiao),
    hoKhau: value(form.hoKhau),
  }
}
