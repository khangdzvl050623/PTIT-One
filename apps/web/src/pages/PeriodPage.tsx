import { PeriodSchedule, TERM_NAMES } from '@/features/dang-ky'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Lịch đợt đăng ký (F08) — `GET /api/enrollment-periods`, sinh viên chỉ thấy
 * đợt của cơ sở mình. Đang chạy dữ liệu demo trong `features/dang-ky/data`.
 */
export function PeriodPage() {
  return (
    <StudentShell>
      <Panel title="LỊCH ĐỢT ĐĂNG KÝ" icon="calendar">
        <PeriodSchedule termNames={TERM_NAMES} />
      </Panel>
    </StudentShell>
  )
}
