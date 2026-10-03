import { useAuth } from '@/features/auth'
import { PrerequisiteView } from '@/features/dang-ky'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Xem môn tiên quyết (F03, chỉ đọc). Cùng nguồn tiên quyết với màn đăng ký —
 * môn nào báo "Chưa đạt" ở đây thì đăng ký sẽ bị `PREREQUISITE_NOT_MET`.
 */
export function PrerequisitePage() {
  const { user } = useAuth()

  return (
    <StudentShell>
      <Panel title="XEM MÔN TIÊN QUYẾT" icon="book">
        <PrerequisiteView exportName={`mon-tien-quyet-${user?.username ?? 'sinh-vien'}`} />
      </Panel>
    </StudentShell>
  )
}
