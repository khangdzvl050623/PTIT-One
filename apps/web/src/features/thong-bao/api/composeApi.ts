import { pickApi } from '@/shared/api'

import * as httpComposeApi from './httpComposeApi'
import * as mockComposeApi from './mockComposeApi'

/** Điểm vào duy nhất của luồng soạn. Công tắc: `VITE_API_MODE`. */
export const { createDraft, previewNotice, sendDraft } = pickApi<typeof httpComposeApi>(
  httpComposeApi,
  mockComposeApi,
)
