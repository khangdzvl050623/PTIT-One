import { apiFetch } from '@/shared/api'

import type { StudentGrade, TermSummary } from '../types'

/**
 * Bảng điểm sinh viên (F07).
 *
 * `/transcript` trả sẵn số liệu học kỳ và luỹ kế — quy đổi thang 4, chọn lần
 * điểm cao nhất và ngưỡng xếp loại là QUY TẮC, nằm ở `GradePolicy` phía
 * backend. Không tính lại ở đây, nếu không sẽ có hai bộ quy tắc lệch nhau.
 */

export function fetchGrades(): Promise<readonly StudentGrade[]> {
  return apiFetch<readonly StudentGrade[]>('/api/me/grades')
}

/** Gom theo học kỳ, kỳ mới nhất trước. */
export function fetchTranscript(): Promise<readonly TermSummary[]> {
  return apiFetch<readonly TermSummary[]>('/api/me/transcript')
}
