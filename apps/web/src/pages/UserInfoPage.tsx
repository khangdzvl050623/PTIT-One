import { LoginPanel, ROLE_LABELS, useAuth } from '@/features/auth'
import type { SessionUser } from '@/features/auth'
import {
  DEMO_PROFILE,
  DEMO_RESULTS,
  DEMO_SUMMARY,
  FeatureLinks,
  GradeChart,
  InfoTable,
  STUDENT_FEATURES,
  StatTile,
  StudentInfoPanel,
} from '@/features/ho-so'
import { DEMO_TERMS, StudyProgress, demoTimetableOf } from '@/features/lich-hoc'
import { AccessStats } from '@/features/thong-ke'
import { ROUTES } from '@/shared/constants'
import { Panel } from '@/shared/ui'

import styles from './UserInfoPage.module.scss'

/**
 * Trang Thông tin sau khi đăng nhập: hồ sơ · số liệu nhanh · kết quả học tập ·
 * tài khoản và lối tắt. Dữ liệu sinh viên hiện là bản demo trong
 * `features/ho-so/data` — backend chưa có endpoint hồ sơ của chính mình.
 */
export function UserInfoPage() {
  const { user } = useAuth()
  if (!user) return null

  if (user.role !== 'SINH_VIEN') {
    return <AccountOnly user={user} />
  }

  const { tinChiHocKy } = DEMO_SUMMARY

  return (
    <div className={styles.page}>
      <div className={styles.columns}>
        <div className={styles.profileColumn}>
          <StudentInfoPanel profile={DEMO_PROFILE} />
        </div>

        <div className={styles.summaryColumn}>
          <StatTile
            label="Thông báo mới, chưa xem"
            value={DEMO_SUMMARY.thongBaoChuaDoc}
            icon="bell"
            href={ROUTES.home}
          />
          <div className={styles.tilePair}>
            <StatTile
              label="Lịch học trong tuần"
              value={DEMO_SUMMARY.buoiHocTrongTuan}
              icon="calendar"
              href={ROUTES.svLichHoc}
              tone="brand"
            />
            <StatTile
              label="Tín chỉ học kỳ này"
              value={`${tinChiHocKy.daDangKy}/${tinChiHocKy.tran}`}
              icon="book"
              href={ROUTES.svDangKy}
              tone="warm"
            />
          </div>
          <GradeChart terms={DEMO_RESULTS} />
        </div>

        <aside className={styles.asideColumn}>
          <LoginPanel />
          <FeatureLinks links={STUDENT_FEATURES} />
        </aside>
      </div>

      <Panel title="TIẾN TRÌNH HỌC TẬP" icon="graduate">
        <StudyProgress terms={DEMO_TERMS} timetableOf={demoTimetableOf} />
      </Panel>

      <div className={styles.stats}>
        <AccessStats />
      </div>
    </div>
  )
}

/** Giảng viên và quản trị: chưa có hồ sơ riêng, chỉ hiện danh tính phiên. */
function AccountOnly({ user }: { user: SessionUser }) {
  return (
    <div className={styles.accountOnly}>
      <Panel title="Thông tin tài khoản" icon="user">
        <InfoTable
          rows={[
            { label: 'Tài khoản', value: user.username },
            { label: 'Vai trò', value: ROLE_LABELS[user.role] },
            { label: 'Mã hồ sơ', value: user.entityId },
            { label: 'Cơ sở', value: user.homeCampus },
          ]}
        />
      </Panel>
      <LoginPanel />
    </div>
  )
}
