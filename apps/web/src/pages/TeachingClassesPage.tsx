import { useCallback, useEffect, useState } from 'react'

import { useTerms, WeekTimetable } from '@/features/lich-hoc'
import type { TimetableEntry } from '@/features/lich-hoc'
import { TeachingClassList, teachingSchedule } from '@/features/nhap-diem'
import type { TeachingScheduleEntry } from '@/features/nhap-diem'
import { Panel } from '@/shared/ui'

import styles from './TeachingClassesPage.module.scss'

const NO_ENTRIES: readonly TeachingScheduleEntry[] = []

/**
 * Lớp phụ trách (F05) — `GET /api/me/teaching-classes` và lịch dạy
 * `GET /api/me/teaching-schedule?maHocKy=`.
 *
 * Lịch dạy **cùng hình dạng** với thời khoá biểu sinh viên (ghi rõ trong hợp
 * đồng API), nên trang dùng lại nguyên màn lưới tuần của `features/lich-hoc`
 * thay vì dựng màn lịch thứ hai. Hai feature không import nhau — trang là nơi
 * ghép, giống `ProgramPage` ghép bảng điểm với chương trình đào tạo.
 */
export function TeachingClassesPage() {
  /* Học kỳ lấy từ API, KHÔNG viết cứng: mã của dữ liệu mẫu khác mã trong
     database thật, nên hằng số sẽ làm màn hình rỗng im lặng ở chế độ api. */
  const { terms, defaultTerm, loading, error } = useTerms()
  const [chosenTerm, setChosenTerm] = useState('')
  const maHocKy = chosenTerm || defaultTerm
  /**
   * Lịch của **mọi** học kỳ, nạp một lần. Lưới tuần tự chọn học kỳ của nó, độc
   * lập với bảng lớp bên trên; nếu chỉ nạp kỳ đang chọn thì đổi kỳ ở lưới sẽ
   * ra tuần trống mà không có lý do nào nhìn thấy được.
   */
  const [schedules, setSchedules] = useState<Record<string, readonly TeachingScheduleEntry[]>>({})

  useEffect(() => {
    if (terms.length === 0) return
    let cancelled = false
    void Promise.all(
      terms.map(async (t) => [t.maHocKy, (await teachingSchedule(t.maHocKy)).buoiHoc] as const),
    ).then(
      (pairs) => {
        if (!cancelled) setSchedules(Object.fromEntries(pairs))
      },
      () => undefined,
    )
    return () => {
      cancelled = true
    }
  }, [terms])

  const timetableOf = useCallback(
    (ky: string): readonly TimetableEntry[] => schedules[ky] ?? NO_ENTRIES,
    [schedules],
  )

  if (loading || error) {
    return (
      <div className={styles.page}>
        <Panel title="LỚP PHỤ TRÁCH" icon="chalkboard">
          {loading ? <p>Đang tải học kỳ…</p> : <p role="alert">{error}</p>}
        </Panel>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <Panel title="LỚP PHỤ TRÁCH" icon="chalkboard">
        <TeachingClassList
          maHocKy={maHocKy}
          onChangeTerm={setChosenTerm}
          entries={schedules[maHocKy] ?? NO_ENTRIES}
        />
      </Panel>

      <Panel title="LỊCH DẠY THEO TUẦN" icon="calendar">
        <WeekTimetable terms={terms} timetableOf={timetableOf} />
      </Panel>
    </div>
  )
}
