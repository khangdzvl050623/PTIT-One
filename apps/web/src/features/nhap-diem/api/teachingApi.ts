import { pickApi } from '@/shared/api'

import * as httpTeachingApi from './httpTeachingApi'
import * as mockTeachingApi from './mockTeachingApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const {
  classRoster,
  publishGrades,
  resetDemo,
  saveGrades,
  sheet,
  teachingClasses,
  teachingSchedule,
} = pickApi<typeof httpTeachingApi>(httpTeachingApi, mockTeachingApi)
