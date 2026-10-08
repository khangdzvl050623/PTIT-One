import { pickApi } from '@/shared/api'

import * as httpCredentialApi from './httpCredentialApi'
import * as mockCredentialApi from './mockCredentialApi'

/** Điểm vào duy nhất cho A1. Công tắc: `VITE_API_MODE`. */
export const {
  activate,
  changeEmail,
  changePassword,
  fetchEmail,
  forgotPassword,
  resendActivation,
  resendEmailCode,
  resetPassword,
  verifyEmail,
} = pickApi<typeof httpCredentialApi>(httpCredentialApi, mockCredentialApi)
