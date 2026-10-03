import { LoginPanel, ROLE_LABELS, useAuth } from '@/features/auth'
import type { SessionUser } from '@/features/auth'
import {
  FeatureLinks,
  fetchProfile,
  fetchResults,
  fetchSummary,
  GradeChart,
  InfoTable,
  STUDENT_FEATURES,
  StatTile,
  StudentInfoPanel,
} from '@/features/ho-so'
import { StudyProgress, useTimetables } from '@/features/lich-hoc'
import { AccessStats } from '@/features/thong-ke'
import { ROUTES } from '@/shared/constants'
import { useAsyncData } from '@/shared/lib'
import { Panel } from '@/shared/ui'

import styles from './UserInfoPage.module.scss'

/**
 * Trang Thông tin sau khi đăng nhập: hồ sơ · số liệu nhanh · kết quả học tập ·
 * tài khoản và lối tắt.
 *
 * Bốn nguồn dữ liệu tải ĐỘC LẬP nhau: một phần lỗi thì ba phần kia vẫn hiện.
 * Đây là trang chủ của sinh viên nên trắng cả trang vì một con số là quá đắt.
 */
export function UserInfoPage() {
  const { user } = useAuth()

  if (!user) return null
  if (user.role !== 'SINH_VIEN') {
    return <AccountOnly user={user} />
  }
  return <StudentHome />
}

function StudentHome() {
  const profile = useAsyncData(fetchProfile)
  const summary = useAsyncData(fetchSummary)
  const results = useAsyncData(fetchResults)
  const { terms, timetableOf, loading: loadingLich, error: errorLich } = useTimetables()

  const tinChi = summary.data?.tinChiHocKy

  return (
    <div className={styles.page}>
      <div className={styles.columns}>
        <div className={styles.profileColumn}>
          {profile.loading ? <p>Đang tải hồ sơ…</p> : null}
          {profile.error ? <p role="alert">{profile.error}</p> : null}
          {profile.data ? <StudentInfoPanel profile={profile.data} /> : null}
        </div>

        <div className={styles.summaryColumn}>
          {/* Chưa có số thì hiện dấu gạch, không hiện 0 — 0 là một con số có
              nghĩa, còn "chưa biết" thì không. */}
          <StatTile
            label="Thông báo mới, chưa xem"
            value={summary.data?.thongBaoChuaDoc ?? '—'}
            icon="bell"
            href={ROUTES.home}
          />
          <div className={styles.tilePair}>
            <StatTile
              label="Lịch học trong tuần"
              value={summary.data?.buoiHocTrongTuan ?? '—'}
              icon="calendar"
              href={ROUTES.svLichHoc}
              tone="brand"
            />
            <StatTile
              label="Tín chỉ học kỳ này"
              value={tinChi ? `${tinChi.daDangKy}/${tinChi.tran}` : '—'}
              icon="book"
              href={ROUTES.svDangKy}
              tone="warm"
            />
          </div>
          {results.data ? <GradeChart terms={results.data} /> : null}
        </div>

        <aside className={styles.asideColumn}>
          <LoginPanel />
          <FeatureLinks links={STUDENT_FEATURES} />
        </aside>
      </div>

      <Panel title="TIẾN TRÌNH HỌC TẬP" icon="graduate">
        {loadingLich ? <p>Đang tải tiến trình…</p> : null}
        {errorLich ? <p role="alert">{errorLich}</p> : null}
        {!loadingLich && !errorLich ? (
          <StudyProgress terms={terms} timetableOf={timetableOf} />
        ) : null}
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
