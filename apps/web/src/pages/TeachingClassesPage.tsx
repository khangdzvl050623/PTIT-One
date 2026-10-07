import { useCallback, useEffect, useState } from 'react'

import { DEMO_TERMS, WeekTimetable } from '@/features/lich-hoc'
import type { TimetableEntry } from '@/features/lich-hoc'
import {
  DEMO_MA_HOC_KY,
  TERM_NAMES,
  TeachingClassList,
  teachingSchedule,
} from '@/features/nhap-diem'
import type { TeachingScheduleEntry } from '@/features/nhap-diem'
import { Panel } from '@/shared/ui'

import styles from './TeachingClassesPage.module.scss'

/** Học kỳ giảng viên có lớp, theo thứ tự của `DEMO_TERMS` (mới nhất trước). */
const TEACHING_TERMS = DEMO_TERMS.filter((t) => t.maHocKy in TERM_NAMES)

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
  const [maHocKy, setMaHocKy] = useState(DEMO_MA_HOC_KY)
  /**
   * Lịch của **mọi** học kỳ, nạp một lần. Lưới tuần tự chọn học kỳ của nó, độc
   * lập với bảng lớp bên trên; nếu chỉ nạp kỳ đang chọn thì đổi kỳ ở lưới sẽ
   * ra tuần trống mà không có lý do nào nhìn thấy được.
   */
  const [schedules, setSchedules] = useState<Record<string, readonly TeachingScheduleEntry[]>>({})

  useEffect(() => {
    let cancelled = false
    void Promise.all(
      TEACHING_TERMS.map(async (t) => [t.maHocKy, (await teachingSchedule(t.maHocKy)).buoiHoc] as const),
    ).then(
      (pairs) => {
        if (!cancelled) setSchedules(Object.fromEntries(pairs))
      },
      () => undefined,
    )
    return () => {
      cancelled = true
    }
  }, [])

  const timetableOf = useCallback(
    (ky: string): readonly TimetableEntry[] => schedules[ky] ?? NO_ENTRIES,
    [schedules],
  )

  return (
    <div className={styles.page}>
      <Panel title="LỚP PHỤ TRÁCH" icon="chalkboard">
        <TeachingClassList
          maHocKy={maHocKy}
          onChangeTerm={setMaHocKy}
          entries={schedules[maHocKy] ?? NO_ENTRIES}
        />
      </Panel>

      <Panel title="LỊCH DẠY THEO TUẦN" icon="calendar">
        <WeekTimetable terms={TEACHING_TERMS} timetableOf={timetableOf} />
      </Panel>
    </div>
  )
}
