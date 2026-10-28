import { pickApi } from '@/shared/api'

import * as httpInboxApi from './httpInboxApi'
import * as mockInboxApi from './mockInboxApi'

/** Điểm vào duy nhất của hộp thư. Công tắc: `VITE_API_MODE`. */
export const { fetchInbox, fetchUnreadCount, markAllRead, markRead } = pickApi<
  typeof httpInboxApi
>(httpInboxApi, mockInboxApi)
