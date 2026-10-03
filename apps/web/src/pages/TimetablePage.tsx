import { DEMO_TERMS, StudyProgress, WeekTimetable, demoTimetableOf } from '@/features/lich-hoc'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Thời khoá biểu dạng tuần (F09). Dữ liệu demo trong `features/lich-hoc/data`;
 * nối API thì thay `DEMO_TERMS` bằng `GET /api/terms` và `demoTimetableOf`
 * bằng `GET /api/me/timetable?maHocKy=`.
 */
export function TimetablePage() {
  return (
    <StudentShell>
      <Panel title="THỜI KHOÁ BIỂU DẠNG TUẦN" icon="calendar">
        <WeekTimetable terms={DEMO_TERMS} timetableOf={demoTimetableOf} />
      </Panel>

      {/* Không in: bản in chỉ cần lịch tuần. */}
      <div data-print="hide">
        <Panel title="TIẾN TRÌNH HỌC TẬP" icon="graduate">
          <StudyProgress terms={DEMO_TERMS} timetableOf={demoTimetableOf} />
        </Panel>
      </div>
    </StudentShell>
  )
}
