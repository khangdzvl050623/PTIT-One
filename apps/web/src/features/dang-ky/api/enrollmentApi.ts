import { pickApi } from '@/shared/api'

import * as httpEnrollmentApi from './httpEnrollmentApi'
import * as mockEnrollmentApi from './mockEnrollmentApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const {
  currentTerm,
  listPeriods,
  currentPeriod,
  myProgram,
  courseCatalog,
  myBestResults,
  listRelations,
  listOpenClasses,
  myEnrollments,
  register,
  cancel,
  resetDemo,
} = pickApi<typeof httpEnrollmentApi>(httpEnrollmentApi, mockEnrollmentApi)
