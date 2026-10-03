import { useCallback, useMemo } from 'react'

import { useAsyncData } from '@/shared/lib'

import { fetchTerms, fetchTimetable } from '../api/timetableApi'
import type { Term, TimetableEntry } from '../types'

const NO_ENTRIES: readonly TimetableEntry[] = []

export interface Timetables {
  terms: readonly Term[]
  /** Tra cứu ĐỒNG BỘ — các component lịch nhận hàm này, không tự gọi API. */
  timetableOf: (maHocKy: string) => readonly TimetableEntry[]
  loading: boolean
  error: string | null
}

/**
 * Tải danh mục học kỳ và lịch của **mọi** học kỳ một lượt.
 *
 * Vì sao tải hết thay vì tải theo kỳ đang chọn: `WeekTimetable`,
 * `SemesterTimetable` và `StudyProgress` đều nhận một hàm tra cứu đồng bộ và
 * tự đổi kỳ bên trong. Giữ hợp đồng đó rẻ hơn nhiều so với sửa cả ba component
 * sang bất đồng bộ, và số học kỳ của một sinh viên chỉ vài ba cái.
 *
 * ⚠️ Đây là N+1 request có chủ ý (1 lần lấy học kỳ + 1 lần mỗi kỳ), chạy song
 * song. Nếu sau này số học kỳ lớn thì thêm một endpoint trả lịch nhiều kỳ, đừng
 * vá bằng cách gọi tuần tự.
 */
export function useTimetables(): Timetables {
  const load = useCallback(async () => {
    const terms = await fetchTerms()
    const timetables = await Promise.all(
      terms.map(async (term) => [term.maHocKy, (await fetchTimetable(term.maHocKy)).buoiHoc] as const),
    )
    return { terms, byTerm: new Map(timetables) }
  }, [])

  const { data, loading, error } = useAsyncData(load)

  const timetableOf = useCallback(
    (maHocKy: string) => data?.byTerm.get(maHocKy) ?? NO_ENTRIES,
    [data],
  )

  return useMemo(
    () => ({ terms: data?.terms ?? [], timetableOf, loading, error }),
    [data, timetableOf, loading, error],
  )
}
