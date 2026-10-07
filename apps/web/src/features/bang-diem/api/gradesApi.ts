import { pickApi } from '@/shared/api'

import * as httpGradesApi from './httpGradesApi'
import * as mockGradesApi from './mockGradesApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const { fetchGrades, fetchTranscript } = pickApi<typeof httpGradesApi>(
  httpGradesApi,
  mockGradesApi,
)
