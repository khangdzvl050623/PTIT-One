import { ClassEnrollments, PeriodManager } from '@/features/dang-ky'
import { Panel } from '@/shared/ui'

import styles from './AdminRegistrationPage.module.scss'

/**
 * Quản lý đăng ký học phần (ADMIN_CO_SO): đợt đăng ký của cơ sở và ghi danh
 * theo lớp. Không duyệt từng lượt — đăng ký có hiệu lực ngay theo thiết kế
 * Phần 1; quản trị điều khiển qua đợt (mở/đóng/gia hạn) và huỷ lớp.
 */
export function AdminRegistrationPage() {
  return (
    <div className={styles.page}>
      <Panel title="ĐỢT ĐĂNG KÝ HỌC PHẦN" icon="calendar">
        <PeriodManager />
      </Panel>
      <Panel title="GHI DANH THEO LỚP — HỌC KỲ 1 - NĂM HỌC 2026 - 2027" icon="users">
        <ClassEnrollments />
      </Panel>
    </div>
  )
}
