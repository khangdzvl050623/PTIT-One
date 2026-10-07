import type { ReactNode } from 'react'

import { LoginPanel, ROLE_LABELS, useAuth } from '@/features/auth'
import type { SessionUser } from '@/features/auth'
import {
  CAMPUS_ADMIN_FEATURES,
  CAMPUS_ADMIN_RIGHTS,
  DEMO_CAMPUS_ADMIN,
  DEMO_CAMPUS_REPORTS,
  DEMO_MASTER_ADMIN,
  DEMO_SYSTEM_REPORT,
  DEMO_TEACHER,
  DEMO_TEACHER_SUMMARY,
  DEMO_TEACHING,
  DEMO_TEN_HOC_KY_HIEN_TAI,
  FeatureLinks,
  fetchProfile,
  fetchResults,
  fetchSummary,
  GradeChart,
  MASTER_ADMIN_FEATURES,
  MASTER_ADMIN_RIGHTS,
  ProfilePanel,
  RightsPanel,
  STUDENT_FEATURES,
  StatTile,
  StudentInfoPanel,
  TEACHER_FEATURES,
  TeachingClasses,
  TermReportCard,
} from '@/features/ho-so'
import type { FeatureLink } from '@/features/ho-so'
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
 * Phần sinh viên lấy từ API (`/api/me/profile`, `/api/me/transcript`), hoặc dữ
 * liệu mẫu khi `VITE_API_MODE` chưa là `api`. Giảng viên và quản trị vẫn dùng
 * dữ liệu mẫu — xem ghi chú ở `TeacherInfo` và `AdminInfo`.
 */
export function UserInfoPage() {
  const { user } = useAuth()
  if (!user) return null

  if (user.role === 'GIANG_VIEN') return <TeacherInfo user={user} />
  if (user.role === 'ADMIN_CO_SO' || user.role === 'ADMIN_MASTER') return <AdminInfo user={user} />
  /* Tách thành component riêng để các hook bên dưới KHÔNG chạy ở nhánh giảng
     viên và quản trị — gọi hook sau một câu return là vi phạm quy tắc hook. */
  return <StudentHome user={user} />
}

function StudentHome({ user }: { user: SessionUser }) {
  const profile = useAsyncData(fetchProfile)
  const summary = useAsyncData(fetchSummary)
  const results = useAsyncData(fetchResults)
  const { terms, timetableOf, loading: loadingLich, error: errorLich } = useTimetables()

  const tinChiHocKy = summary.data?.tinChiHocKy

  return (
    <div className={styles.page}>
      <div className={styles.columns}>
        <div className={styles.profileColumn}>
          {profile.loading ? <p>Đang tải hồ sơ…</p> : null}
          {profile.error ? <p role="alert">{profile.error}</p> : null}
          {profile.data ? (
            <StudentInfoPanel
              profile={profile.data}
              emailDaXacMinh={user.emailDaXacMinh}
              /* Hồ sơ vừa lưu đã là bản đầy đủ server trả về, nhưng gọi reload
                 để biểu đồ và các ô số liệu cùng đọc lại một lượt. */
              onProfileSaved={() => profile.reload()}
            />
          ) : null}
        </div>

        <div className={styles.summaryColumn}>
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
              value={tinChiHocKy ? `${tinChiHocKy.daDangKy}/${tinChiHocKy.tran}` : '—'}
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

/** Bố cục ba cột dùng chung cho mọi vai trò: hồ sơ · số liệu · tài khoản + lối tắt. */
function InfoLayout(props: {
  profile: ReactNode
  summary: ReactNode
  features: readonly FeatureLink[]
}) {
  return (
    <div className={styles.page}>
      <div className={styles.columns}>
        <div className={styles.profileColumn}>{props.profile}</div>
        <div className={styles.summaryColumn}>{props.summary}</div>
        <aside className={styles.asideColumn}>
          <LoginPanel />
          <FeatureLinks links={props.features} />
        </aside>
      </div>
      <div className={styles.stats}>
        <AccessStats />
      </div>
    </div>
  )
}

/** Giảng viên: hồ sơ + công tác giảng dạy, lớp phụ trách học kỳ này. */
function TeacherInfo({ user }: { user: SessionUser }) {
  const gv = DEMO_TEACHER
  const lop = DEMO_TEACHING
  const chuaCongBo = lop.filter((c) => c.trangThaiDiem === 'NHAP').length

  return (
    <InfoLayout
      features={TEACHER_FEATURES}
      profile={
        <>
          <ProfilePanel
            title="Thông tin giảng viên"
            photo
            rows={[
              { label: 'Mã GV', value: gv.maGiangVien },
              { label: 'Họ tên', value: gv.hoTen },
              { label: 'Học vị', value: gv.hocVi },
              { label: 'Khoa', value: gv.tenKhoa },
              { label: 'Cơ sở', value: gv.tenCoSo },
              { label: 'Email', value: gv.email },
              { label: 'Tài khoản', value: user.username },
              { label: 'Vai trò', value: ROLE_LABELS[user.role] },
            ]}
          />
          <ProfilePanel
            title="Công tác giảng dạy"
            icon="chalkboard"
            rows={[
              { label: 'Học kỳ', value: DEMO_TEN_HOC_KY_HIEN_TAI },
              { label: 'Lớp phụ trách', value: lop.length },
              { label: 'Sinh viên', value: lop.reduce((s, c) => s + c.soLuongDaDangKy, 0) },
              { label: 'Tín chỉ giảng dạy', value: lop.reduce((s, c) => s + c.soTinChi, 0) },
            ]}
          />
        </>
      }
      summary={
        <>
          <StatTile
            label="Thông báo mới, chưa xem"
            value={DEMO_TEACHER_SUMMARY.thongBaoChuaDoc}
            icon="bell"
            href={ROUTES.thongBao}
          />
          <div className={styles.tilePair}>
            <StatTile
              label="Buổi dạy trong tuần"
              value={DEMO_TEACHER_SUMMARY.buoiDayTrongTuan}
              icon="calendar"
              href={ROUTES.gvLopPhuTrach}
              tone="brand"
            />
            <StatTile
              label="Lớp chưa công bố điểm"
              value={chuaCongBo}
              icon="book"
              href={ROUTES.gvNhapDiem}
              tone="warm"
            />
          </div>
          <TeachingClasses
            title="Lớp phụ trách học kỳ này"
            classes={lop}
            gradeHref={ROUTES.gvNhapDiem}
          />
        </>
      }
    />
  )
}

/** Quản trị: thông tin tài khoản + phạm vi quyền, thống kê học kỳ của phạm vi mình. */
function AdminInfo({ user }: { user: SessionUser }) {
  const master = user.role === 'ADMIN_MASTER'
  const admin = master ? DEMO_MASTER_ADMIN : DEMO_CAMPUS_ADMIN
  const report = master
    ? DEMO_SYSTEM_REPORT
    : (DEMO_CAMPUS_REPORTS.find((r) => r.maCoSo === user.homeCampus) ?? DEMO_CAMPUS_REPORTS[0]!)
  // Cùng cách làm tròn (1 chữ số) với thẻ thống kê bên dưới.
  const fill = report.tongSucChua
    ? `${Math.round((report.tongDaDangKy / report.tongSucChua) * 1000) / 10}%`
    : '—'

  return (
    <InfoLayout
      features={master ? MASTER_ADMIN_FEATURES : CAMPUS_ADMIN_FEATURES}
      profile={
        <>
          <ProfilePanel
            title="Thông tin quản trị"
            rows={[
              { label: 'Tài khoản', value: user.username },
              { label: 'Họ tên', value: user.hoTen ?? admin.hoTen },
              { label: 'Vai trò', value: ROLE_LABELS[user.role] },
              // ADMIN_MASTER không thuộc cơ sở nào — ghi rõ, không để trống.
              { label: 'Phạm vi', value: master ? 'Toàn hệ thống' : admin.tenCoSo },
              { label: 'Email', value: admin.email },
              { label: 'Trạng thái', value: 'Hoạt động' },
            ]}
          />
          <RightsPanel rights={master ? MASTER_ADMIN_RIGHTS : CAMPUS_ADMIN_RIGHTS} />
        </>
      }
      summary={
        <>
          <div className={styles.tilePair}>
            <StatTile
              label="Lớp học phần"
              value={report.soLop}
              icon="chalkboard"
              href={ROUTES.qtTongQuan}
              tone="brand"
            />
            <StatTile
              label="Lượt đăng ký"
              value={report.luotDangKy}
              icon="users"
              href={ROUTES.qtTongQuan}
              tone="warm"
            />
          </div>
          <div className={styles.tilePair}>
            <StatTile
              label="Sinh viên đăng ký"
              value={report.soSinhVien}
              icon="graduate"
              href={ROUTES.qtTongQuan}
            />
            <StatTile
              label="Tỉ lệ lấp đầy"
              value={fill}
              icon="book"
              href={ROUTES.qtTongQuan}
            />
          </div>
          <TermReportCard
            title="Thống kê học kỳ hiện tại"
            report={report}
            byCampus={master ? DEMO_CAMPUS_REPORTS : undefined}
          />
        </>
      }
    />
  )
}
