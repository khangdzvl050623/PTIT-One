import { ClassManager } from '@/features/dang-ky'
import { Panel } from '@/shared/ui'

/** Quản trị lớp học phần (F04, ADMIN_CO_SO): mở lớp, sửa, phân công GV, xếp lịch. */
export function AdminClassesPage() {
  return (
    <Panel title="QUẢN LÝ LỚP HỌC PHẦN" icon="chalkboard">
      <ClassManager />
    </Panel>
  )
}
