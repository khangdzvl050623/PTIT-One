import { useTerms } from '@/features/lich-hoc'
import { GradeBook } from '@/features/nhap-diem'
import { API_MODE } from '@/shared/api'
import { Panel } from '@/shared/ui'

/**
 * Nhập điểm (F06) — giảng viên phụ trách lớp.
 *
 * Học kỳ KHÔNG viết cứng: mã của dữ liệu mẫu khác mã trong database thật, nên
 * hằng số sẽ làm màn hình rỗng im lặng khi bật `VITE_API_MODE=api`.
 *
 * Khoá điểm không có ở đây: `POST .../grades/lock` là quyền của `ADMIN_CO_SO`.
 */
export function GradeEntryPage() {
  const { defaultTerm, loading, error } = useTerms()

  return (
    <Panel title="NHẬP ĐIỂM HỌC PHẦN" icon="graduate">
      {loading ? <p>Đang tải học kỳ…</p> : null}
      {error ? <p role="alert">{error}</p> : null}
      {defaultTerm ? <GradeBook maHocKy={defaultTerm} demo={API_MODE === 'mock'} /> : null}
    </Panel>
  )
}
