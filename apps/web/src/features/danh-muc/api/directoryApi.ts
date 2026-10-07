import { pickApi } from '@/shared/api'

import * as httpDirectoryApi from './httpDirectoryApi'
import * as mockDirectoryApi from './mockDirectoryApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const {
  changeStatus,
  createStudent,
  createTeacher,
  listAccounts,
  reissueActivationCode,
  resetDemo,
} = pickApi<typeof httpDirectoryApi>(httpDirectoryApi, mockDirectoryApi)
