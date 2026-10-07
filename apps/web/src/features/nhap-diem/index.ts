// Barrel export cho feature "nhap-diem" — màn hình giảng viên (F05 · F06).
export { GradeBook } from './components/GradeBook'
export type { GradeBookProps } from './components/GradeBook'

export { TeachingClassList } from './components/TeachingClassList'
export type { TeachingClassListProps } from './components/TeachingClassList'

export { teachingSchedule } from './api/teachingApi'
export { DEMO_MA_HOC_KY, TERM_NAMES } from './data/demo'
export { formatLich } from './lib/format'
export type {
  ClassRoster,
  GradeEntry,
  GradeSheet,
  GradeSheetStatus,
  RosterEntry,
  SaveGradeRow,
  TeachingClass,
  TeachingSchedule,
  TeachingScheduleEntry,
} from './types'
