// Barrel export cho feature "ho-so" — trang Thông tin của người dùng.
export { FeatureLinks } from './components/FeatureLinks'
export type { FeatureLinksProps } from './components/FeatureLinks'

export { GradeChart } from './components/GradeChart'
export type { GradeChartProps } from './components/GradeChart'

export { InfoTable } from './components/InfoTable'
export type { InfoRow, InfoTableProps } from './components/InfoTable'

export { StatTile } from './components/StatTile'
export type { StatTileProps } from './components/StatTile'

export { StudentInfoPanel } from './components/StudentInfoPanel'
export type { StudentInfoPanelProps } from './components/StudentInfoPanel'

export { DEMO_PROFILE, DEMO_RESULTS, DEMO_SUMMARY, STUDENT_FEATURES } from './data/profile'
export type {
  CourseResult,
  FeatureLink,
  StudentProfile,
  StudentSummary,
  TermResults,
} from './types'
export { fetchProfile, fetchResults, fetchSummary } from './api/profileApi'
