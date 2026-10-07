import { StudyProgress, useTimetables, WeekTimetable } from '@/features/lich-hoc'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Thời khoá biểu dạng tuần (F09). Dữ liệu từ `GET /api/terms` +
 * `GET /api/me/timetable`, hoặc dữ liệu giả khi `VITE_API_MODE` chưa là `api`.
 */
export function TimetablePage() {
  const { terms, timetableOf, loading, error } = useTimetables()

  if (loading) {
    return (
      <StudentShell>
        <Panel title="THỜI KHOÁ BIỂU DẠNG TUẦN" icon="calendar">
          <p>Đang tải thời khoá biểu…</p>
        </Panel>
      </StudentShell>
    )
  }

  if (error) {
    return (
      <StudentShell>
        <Panel title="THỜI KHOÁ BIỂU DẠNG TUẦN" icon="calendar">
          <p role="alert">{error}</p>
        </Panel>
      </StudentShell>
    )
  }

  return (
    <StudentShell>
      <Panel title="THỜI KHOÁ BIỂU DẠNG TUẦN" icon="calendar">
        <WeekTimetable terms={terms} timetableOf={timetableOf} />
      </Panel>

      {/* Không in: bản in chỉ cần lịch tuần. */}
      <div data-print="hide">
        <Panel title="TIẾN TRÌNH HỌC TẬP" icon="graduate">
          <StudyProgress terms={terms} timetableOf={timetableOf} />
        </Panel>
      </div>
    </StudentShell>
  )
}
