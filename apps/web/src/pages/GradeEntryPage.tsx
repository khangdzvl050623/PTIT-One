import { DEMO_MA_HOC_KY, GradeBook } from '@/features/nhap-diem'
import { Panel } from '@/shared/ui'

/**
 * Nhập điểm (F06) — giảng viên phụ trách lớp. Đang chạy bản giả
 * `features/nhap-diem/api/mockGradeBookApi`: cùng thứ tự kiểm và cùng mã lỗi
 * với `PUT /api/classes/{maLopHP}/grades` và `POST .../grades/publish`.
 *
 * Khoá điểm không có ở đây: `POST .../grades/lock` là quyền của `ADMIN_CO_SO`.
 */
export function GradeEntryPage() {
  return (
    <Panel title="NHẬP ĐIỂM HỌC PHẦN" icon="graduate">
      <GradeBook maHocKy={DEMO_MA_HOC_KY} demo />
    </Panel>
  )
}
