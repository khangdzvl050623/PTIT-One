// Barrel export cho feature "ho-so" — trang Thông tin của người dùng.
export { FeatureLinks } from './components/FeatureLinks'
export type { FeatureLinksProps } from './components/FeatureLinks'

export { GradeChart } from './components/GradeChart'
export type { GradeChartProps } from './components/GradeChart'

export { IdPhoto } from './components/IdPhoto'
export type { IdPhotoProps } from './components/IdPhoto'

export { InfoTable } from './components/InfoTable'
export type { InfoRow, InfoTableProps } from './components/InfoTable'

export { ProfileEditDialog } from './components/ProfileEditDialog'
export type { ProfileEditDialogProps } from './components/ProfileEditDialog'

export { ProfilePanel } from './components/ProfilePanel'
export type { ProfilePanelProps } from './components/ProfilePanel'

export { RightsPanel } from './components/RightsPanel'
export type { RightsPanelProps } from './components/RightsPanel'

export { StatTile } from './components/StatTile'
export type { StatTileProps } from './components/StatTile'

export { StudentInfoPanel } from './components/StudentInfoPanel'
export type { StudentInfoPanelProps } from './components/StudentInfoPanel'

export { TeachingClasses } from './components/TeachingClasses'
export type { TeachingClassesProps } from './components/TeachingClasses'

export { TermReportCard } from './components/TermReportCard'
export type { TermReportCardProps } from './components/TermReportCard'

export { DEMO_PROFILE, DEMO_RESULTS, DEMO_SUMMARY, STUDENT_FEATURES } from './data/profile'
export {
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
  MASTER_ADMIN_FEATURES,
  MASTER_ADMIN_RIGHTS,
  TEACHER_FEATURES,
} from './data/staff'
export type {
  CourseResult,
  FeatureLink,
  StudentProfile,
  StaffProfile,
  StudentSummary,
  TeacherProfile,
  TeachingClass,
  TermReport,
  TermResults,
  UpdateMyProfileInput,
} from './types'
export { fetchProfile, fetchResults, fetchSummary, updateMyProfile } from './api/profileApi'
