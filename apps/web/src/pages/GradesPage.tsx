import { useAuth } from '@/features/auth'
import { DEMO_GRADES, GradeTable } from '@/features/bang-diem'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Xem điểm (F07). Dữ liệu demo trong `features/bang-diem/data`; nối API thì
 * thay `DEMO_GRADES` bằng `GET /api/me/grades` (không gửi `maHocKy`).
 */
export function GradesPage() {
  const { user } = useAuth()

  return (
    <StudentShell>
      <Panel title="XEM ĐIỂM" icon="graduate">
        <GradeTable
          grades={DEMO_GRADES}
          exportName={`bang-diem-${user?.username ?? 'sinh-vien'}`}
        />
      </Panel>
    </StudentShell>
  )
}
