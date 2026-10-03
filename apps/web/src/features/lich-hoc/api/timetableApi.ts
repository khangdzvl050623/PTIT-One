import { pickApi } from '@/shared/api'

import * as httpTimetableApi from './httpTimetableApi'
import * as mockTimetableApi from './mockTimetableApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const { fetchTerms, fetchTimetable } = pickApi<typeof httpTimetableApi>(
  httpTimetableApi,
  mockTimetableApi,
)
