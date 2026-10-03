// Barrel export cho feature "dang-ky" — đăng ký và huỷ học phần (F08).
export { EnrollmentBoard } from './components/EnrollmentBoard'
export type { EnrollmentBoardProps } from './components/EnrollmentBoard'

export { PeriodSchedule } from './components/PeriodSchedule'
export type { PeriodScheduleProps } from './components/PeriodSchedule'

export { PrerequisiteView } from './components/PrerequisiteView'
export type { PrerequisiteViewProps } from './components/PrerequisiteView'

export { ProgramView } from './components/ProgramView'
export type { ProgramViewProps } from './components/ProgramView'

export { DEMO_MA_HOC_KY, DEMO_NGAY_BAT_DAU, DEMO_TEN_HOC_KY, TERM_NAMES } from './data/demo'
export type {
  ClassOffer,
  ClassSection,
  CourseRelation,
  EnrolledCourse,
  EnrollmentPeriod,
  RegistrationResult,
  RelationKind,
  ScheduleSlot,
  StudentEnrollments,
  StudyRecord,
} from './types'
