import { pickApi } from '@/shared/api'

import * as httpCatalogApi from './httpCatalogApi'
import * as mockCatalogApi from './mockCatalogApi'

/** Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`. */
export const { deleteCourse, loadCatalog, resetCatalog, saveCourse, setPrerequisites } =
  pickApi<typeof httpCatalogApi>(httpCatalogApi, mockCatalogApi)

export type { Catalog, CourseInput } from './httpCatalogApi'
