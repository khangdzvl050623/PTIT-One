// Barrel export cho feature "lich-hoc" — thời khoá biểu hợp nhất.
export { SemesterTimetable } from './components/SemesterTimetable'
export type { SemesterTimetableProps } from './components/SemesterTimetable'

export { StudyProgress } from './components/StudyProgress'
export type { StudyProgressProps } from './components/StudyProgress'

export { WeekGrid } from './components/WeekGrid'
export type { WeekGridProps } from './components/WeekGrid'

export { WeekTimetable } from './components/WeekTimetable'
export type { WeekTimetableProps } from './components/WeekTimetable'

export { DEMO_TERMS, DEMO_TIMETABLES, PERIODS, demoTimetableOf } from './data/timetable'
export type { HinhThucHoc, Period, Term, Timetable, TimetableEntry } from './types'
export { fetchTerms, fetchTimetable } from './api/timetableApi'
export { useTimetables } from './model/useTimetables'
export type { Timetables } from './model/useTimetables'
