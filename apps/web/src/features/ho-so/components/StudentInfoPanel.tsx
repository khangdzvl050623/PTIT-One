import { useState } from 'react'

import { Icon, Panel } from '@/shared/ui'

import type { StudentProfile } from '../types'
import { IdPhoto } from './IdPhoto'
import { InfoTable } from './InfoTable'
import { ProfileEditDialog } from './ProfileEditDialog'
import styles from './StudentInfoPanel.module.scss'

const TRANG_THAI_LABELS: Record<string, string> = {
  DANG_HOC: 'Đang học',
  BAO_LUU: 'Bảo lưu',
  THOI_HOC: 'Thôi học',
  TOT_NGHIEP: 'Tốt nghiệp',
}

const GIOI_TINH_LABELS = { NAM: 'Nam', NU: 'Nữ' } as const

export interface StudentInfoPanelProps {
  profile: StudentProfile
  /**
   * Đã xác minh email chưa (`SessionUser.emailDaXacMinh`). Chưa thì chặn sẵn
   * nút sửa — server cũng từ chối bằng `409 EMAIL_NOT_VERIFIED`, nhưng để người
   * dùng gõ hết biểu mẫu rồi mới báo là tệ.
   */
  emailDaXacMinh?: boolean
  /** Gọi sau khi lưu hồ sơ; bỏ trống thì panel chỉ đọc, không có nút sửa. */
  onProfileSaved?: (profile: StudentProfile) => void
}

/** Hai khung bên trái: thông tin cá nhân (kèm ảnh) và thông tin khoá học. */
export function StudentInfoPanel({
  profile,
  emailDaXacMinh = false,
  onProfileSaved,
}: StudentInfoPanelProps) {
  const [editing, setEditing] = useState(false)

  return (
    <>
      <Panel title="Thông tin sinh viên" icon="user">
        <div className={styles.personal}>
          <InfoTable
            rows={[
              { label: 'Mã SV', value: profile.maSinhVien },
              { label: 'Họ tên', value: profile.hoTen },
              { label: 'Ngày sinh', value: formatDate(profile.ngaySinh) },
              { label: 'Giới tính', value: profile.gioiTinh && GIOI_TINH_LABELS[profile.gioiTinh] },
              { label: 'Điện thoại', value: profile.dienThoai },
              { label: 'Số CMND/ CCCD', value: profile.soCCCD },
              { label: 'Email', value: profile.email },
              { label: 'Email 2', value: profile.emailCaNhan },
              { label: 'Nơi sinh', value: profile.noiSinh },
              { label: 'Dân tộc', value: profile.danToc },
              { label: 'Tôn giáo', value: profile.tonGiao },
              {
                label: 'Hiện diện',
                value: TRANG_THAI_LABELS[profile.trangThai] ?? profile.trangThai,
              },
              { label: 'Hộ khẩu', value: profile.hoKhau },
              // Không có trong cổng gốc nhưng là khoá định tuyến của đồ án — giữ lại.
              { label: 'Cơ sở', value: profile.tenCoSo },
            ]}
          />
          <IdPhoto src={profile.anhDaiDien} hoTen={profile.hoTen} />
        </div>

        {onProfileSaved ? (
          <div className={styles.actions}>
            <button
              type="button"
              className={styles.edit}
              onClick={() => setEditing(true)}
              disabled={!emailDaXacMinh}
            >
              <Icon name="user" size="13px" />
              Cập nhật thông tin
            </button>
            {emailDaXacMinh ? null : (
              <p className={styles.gate}>
                Cần xác minh email trước khi tự sửa hồ sơ.
              </p>
            )}
          </div>
        ) : null}
      </Panel>

      {/* Mount khi mở để biểu mẫu luôn khởi tạo từ hồ sơ mới nhất. */}
      {editing && onProfileSaved ? (
        <ProfileEditDialog
          open
          onClose={() => setEditing(false)}
          profile={profile}
          onSaved={onProfileSaved}
        />
      ) : null}

      <Panel title="Thông tin khoá học" icon="graduate">
        <InfoTable
          rows={[
            { label: 'Chương trình', value: `${profile.tenCTDT} (${profile.maCTDT})` },
            { label: 'Khoa', value: profile.tenKhoa },
            { label: 'Bậc đào tạo', value: 'Đại học chính quy' },
            { label: 'Tín chỉ tích luỹ', value: `${profile.soTinChiTichLuy} / ${profile.tongTinChiCTDT}` },
          ]}
        />
        <div
          className={styles.progress}
          role="progressbar"
          aria-label="Tiến độ tín chỉ tích luỹ"
          aria-valuemin={0}
          aria-valuemax={profile.tongTinChiCTDT}
          aria-valuenow={profile.soTinChiTichLuy}
        >
          <span
            className={styles.progressFill}
            style={{
              width: `${Math.min(100, (profile.soTinChiTichLuy / profile.tongTinChiCTDT) * 100)}%`,
            }}
          />
        </div>
      </Panel>
    </>
  )
}

function formatDate(iso: string | null): string | null {
  if (!iso) return null
  const [y, m, d] = iso.split('-')
  return d && m && y ? `${d}/${m}/${y}` : iso
}
