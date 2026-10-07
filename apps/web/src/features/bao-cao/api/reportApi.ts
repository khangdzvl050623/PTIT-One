import { pickApi } from '@/shared/api'

import * as httpReportApi from './httpReportApi'
import * as mockReportApi from './mockReportApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const { getCourseReport, getSummary, listCampuses, listTerms } = pickApi<typeof httpReportApi>(
  httpReportApi,
  mockReportApi,
)
