import { pickApi } from '@/shared/api'

import * as httpEnrollmentApi from './httpEnrollmentApi'
import * as mockEnrollmentApi from './mockEnrollmentApi'

/**
 * Điểm vào duy nhất của feature. Công tắc: `VITE_API_MODE`.
 *
 * ⚠️ MỌI component phải import từ đây, không import thẳng `mockEnrollmentApi` —
 * nếu không thì bật `VITE_API_MODE=api` mà màn hình vẫn im lặng hiện dữ liệu
 * giả, và đó là loại lỗi rất khó nhận ra khi demo.
 */
export const {
  adminClasses,
  assignTeacher,
  cancel,
  cancelClass,
  classRoster,
  courseCatalog,
  createClass,
  currentPeriod,
  currentTerm,
  listOpenClasses,
  listPeriods,
  listRelations,
  listTeachers,
  myBestResults,
  myEnrollments,
  myProgram,
  openableCourses,
  register,
  resetDemo,
  savePeriod,
  setSchedule,
  updateClass,
} = pickApi<typeof httpEnrollmentApi>(httpEnrollmentApi, mockEnrollmentApi)
