import { apiFetch } from '@/shared/api'

import type { Term, Timetable } from '../types'

/** Thời khoá biểu sinh viên (F09) và danh mục học kỳ. */

export function fetchTerms(): Promise<readonly Term[]> {
  return apiFetch<readonly Term[]>('/api/terms')
}

/**
 * @param tuan bỏ trống là cả học kỳ; có tuần thì server lọc sẵn
 */
export function fetchTimetable(maHocKy: string, tuan?: number): Promise<Timetable> {
  const query = new URLSearchParams({ maHocKy })
  if (tuan !== undefined) query.set('tuan', String(tuan))
  return apiFetch<Timetable>(`/api/me/timetable?${query.toString()}`)
}
