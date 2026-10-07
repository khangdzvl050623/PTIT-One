import { pickApi } from '@/shared/api'

import * as httpAuthApi from './httpAuthApi'
import * as mockAuthApi from './mockAuthApi'

/**
 * Điểm vào duy nhất của auth. Công tắc mock/API dùng chung cho cả app —
 * `VITE_API_MODE=api` để gọi backend thật; xem `shared/api/mode.ts`.
 *
 * Hai bản cùng chữ ký nên phần còn lại của app không phải sửa.
 */
export const { login, fetchCurrentUser, refreshSession, logout, logoutAll } =
  pickApi(httpAuthApi, mockAuthApi)
