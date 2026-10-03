import { useAuth } from '@/features/auth'
import { DEMO_TERMS, SemesterTimetable, demoTimetableOf } from '@/features/lich-hoc'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Thời khoá biểu dạng học kỳ (F09) — cùng nguồn với dạng tuần:
 * `GET /api/me/timetable?maHocKy=` không gửi `tuan`.
 */
export function SemesterTimetablePage() {
  const { user } = useAuth()

  return (
    <StudentShell>
      <Panel title="THỜI KHOÁ BIỂU DẠNG HỌC KỲ" icon="calendar">
        <SemesterTimetable
          terms={DEMO_TERMS}
          timetableOf={demoTimetableOf}
          exportName={`thoi-khoa-bieu-${user?.username ?? 'sinh-vien'}`}
        />
      </Panel>
    </StudentShell>
  )
}
