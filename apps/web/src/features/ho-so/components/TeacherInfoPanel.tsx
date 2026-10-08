import { useState } from 'react'

import { Icon, Panel } from '@/shared/ui'

import {
  removeTeacherAvatar,
  updateMyTeacherProfile,
  uploadTeacherAvatar,
} from '../api/profileApi'
import type { TeacherProfile } from '../types'
import { IdPhoto } from './IdPhoto'
import { InfoTable } from './InfoTable'
import { ProfileEditDialog } from './ProfileEditDialog'
import styles from './StudentInfoPanel.module.scss'

const GIOI_TINH_LABELS = { NAM: 'Nam', NU: 'Nữ' } as const

export interface TeacherInfoPanelProps {
  profile: TeacherProfile
  /** Tên đăng nhập và vai trò, lấy từ phiên — không nằm trong hồ sơ giảng viên. */
  tenDangNhap: string
  vaiTro: string
  /** Xem ghi chú cùng tên ở `StudentInfoPanel`. */
  emailDaXacMinh?: boolean
  onProfileSaved?: (profile: TeacherProfile) => void
}

/**
 * Khung "Thông tin giảng viên" — đối xứng với {@link StudentInfoPanel}.
 *
 * Dùng chung `StudentInfoPanel.module.scss` vì hai khung là một thiết kế: cùng
 * bảng thông tin, cùng ảnh thẻ, cùng hàng nút. Tách hai bộ style thì chúng sẽ
 * trôi khỏi nhau sau vài lần sửa.
 *
 * Bốn dòng đầu là dữ liệu hành chính (mã, họ tên, học vị, khoa) — Phòng Đào tạo
 * quản, nên không có trong biểu mẫu sửa.
 */
export function TeacherInfoPanel({
  profile,
  tenDangNhap,
  vaiTro,
  emailDaXacMinh = false,
  onProfileSaved,
}: TeacherInfoPanelProps) {
  const [editing, setEditing] = useState(false)

  return (
    <>
      <Panel title="Thông tin giảng viên" icon="user">
        <div className={styles.personal}>
          <InfoTable
            rows={[
              { label: 'Mã GV', value: profile.maGiangVien },
              { label: 'Họ tên', value: profile.hoTen },
              { label: 'Học vị', value: profile.hocVi },
              { label: 'Khoa', value: profile.tenKhoa },
              { label: 'Cơ sở', value: profile.tenCoSo },
              { label: 'Email', value: profile.email },
              { label: 'Giới tính', value: profile.gioiTinh && GIOI_TINH_LABELS[profile.gioiTinh] },
              { label: 'Điện thoại', value: profile.dienThoai },
              { label: 'Số CMND/ CCCD', value: profile.soCCCD },
              { label: 'Email 2', value: profile.emailCaNhan },
              { label: 'Nơi sinh', value: profile.noiSinh },
              { label: 'Dân tộc', value: profile.danToc },
              { label: 'Tôn giáo', value: profile.tonGiao },
              { label: 'Hộ khẩu', value: profile.hoKhau },
              { label: 'Tài khoản', value: tenDangNhap },
              { label: 'Vai trò', value: vaiTro },
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
              <p className={styles.gate}>Cần xác minh email trước khi tự sửa hồ sơ.</p>
            )}
          </div>
        ) : null}
      </Panel>

      {editing && onProfileSaved ? (
        <ProfileEditDialog
          open
          onClose={() => setEditing(false)}
          profile={profile}
          onSaved={onProfileSaved}
          save={updateMyTeacherProfile}
          upload={uploadTeacherAvatar}
          remove={removeTeacherAvatar}
        />
      ) : null}
    </>
  )
}
