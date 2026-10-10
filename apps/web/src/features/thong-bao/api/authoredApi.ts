import { pickApi } from '@/shared/api'

import * as httpAuthoredApi from './httpAuthoredApi'
import * as mockAuthoredApi from './mockAuthoredApi'

/** Điểm vào duy nhất của hộp thư gửi. Công tắc: `VITE_API_MODE`. */
export const { fetchAuthored, fetchAuthoredDetail } = pickApi<typeof httpAuthoredApi>(
  httpAuthoredApi,
  mockAuthoredApi,
)
