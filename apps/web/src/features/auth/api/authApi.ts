import * as httpAuthApi from './httpAuthApi'
import * as mockAuthApi from './mockAuthApi'

/**
 * Điểm vào duy nhất của auth. Mặc định dùng bản **giả** để dựng UI không cần
 * backend; đặt `VITE_AUTH_MODE=api` (trong `apps/web/.env.local`) để gọi API
 * thật. Hai bản cùng chữ ký nên phần còn lại của app không phải sửa.
 */
export const AUTH_MODE: 'api' | 'mock' = import.meta.env.VITE_AUTH_MODE === 'api' ? 'api' : 'mock'

const impl = AUTH_MODE === 'api' ? httpAuthApi : mockAuthApi

export const { login, fetchCurrentUser, refreshSession, logout, logoutAll } = impl
