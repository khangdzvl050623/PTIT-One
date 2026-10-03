import { useAuth } from '@/features/auth'
import { SemesterTimetable, useTimetables } from '@/features/lich-hoc'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Thời khoá biểu dạng học kỳ (F09) — cùng nguồn với dạng tuần, chỉ khác cách
 * hiển thị.
 */
export function SemesterTimetablePage() {
  const { user } = useAuth()
  const { terms, timetableOf, loading, error } = useTimetables()

  return (
    <StudentShell>
      <Panel title="THỜI KHOÁ BIỂU DẠNG HỌC KỲ" icon="calendar">
        {loading ? <p>Đang tải thời khoá biểu…</p> : null}
        {error ? <p role="alert">{error}</p> : null}
        {!loading && !error ? (
          <SemesterTimetable
            terms={terms}
            timetableOf={timetableOf}
            exportName={`thoi-khoa-bieu-${user?.username ?? 'sinh-vien'}`}
          />
        ) : null}
      </Panel>
    </StudentShell>
  )
}
