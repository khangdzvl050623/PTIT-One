import { pickApi } from '@/shared/api'

import * as httpProfileApi from './httpProfileApi'
import * as mockProfileApi from './mockProfileApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const {
  fetchProfile,
  fetchResults,
  fetchSummary,
  removeAvatar,
  updateMyProfile,
  uploadAvatar,
} = pickApi<typeof httpProfileApi>(httpProfileApi, mockProfileApi)
