import { useAuth } from '@/features/auth'
import {
  DEMO_MA_HOC_KY,
  DEMO_NGAY_BAT_DAU,
  DEMO_TEN_HOC_KY,
  EnrollmentBoard,
} from '@/features/dang-ky'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Đăng ký học phần (F08). Đang chạy bản giả `features/dang-ky/api/mockEnrollmentApi`
 * — kiểm tra cùng thứ tự và cùng mã lỗi với `POST /api/me/enrollments`.
 */
export function RegistrationPage() {
  const { user } = useAuth()

  return (
    <StudentShell>
      <Panel title={`ĐĂNG KÝ MÔN HỌC ${DEMO_TEN_HOC_KY.toLocaleUpperCase('vi')}`} icon="book">
        <EnrollmentBoard
          maHocKy={DEMO_MA_HOC_KY}
          tenHocKy={DEMO_TEN_HOC_KY}
          ngayBatDau={DEMO_NGAY_BAT_DAU}
          studentLabel={[user?.username, user?.hoTen].filter(Boolean).join(' · ')}
          demo
        />
      </Panel>
    </StudentShell>
  )
}
