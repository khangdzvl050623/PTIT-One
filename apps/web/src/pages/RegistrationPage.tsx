import { useAuth } from '@/features/auth'
import { currentTerm, EnrollmentBoard } from '@/features/dang-ky'
import { API_MODE } from '@/shared/api'
import { useAsyncData } from '@/shared/lib'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Đăng ký học phần (F08).
 *
 * Học kỳ KHÔNG viết cứng: lấy từ đợt đăng ký đang mở, vì đợt cho kỳ sau thường
 * mở trong khi kỳ hiện tại còn đang học. Bản giả trả đúng một học kỳ demo.
 */
export function RegistrationPage() {
  const { user } = useAuth()
  const { data: term, loading, error } = useAsyncData(currentTerm)

  const title = term ? `ĐĂNG KÝ MÔN HỌC ${term.tenHocKy.toLocaleUpperCase('vi')}` : 'ĐĂNG KÝ MÔN HỌC'

  return (
    <StudentShell>
      <Panel title={title} icon="book">
        {loading ? <p>Đang tải đợt đăng ký…</p> : null}
        {error ? <p role="alert">{error}</p> : null}
        {term ? (
          <EnrollmentBoard
            maHocKy={term.maHocKy}
            tenHocKy={term.tenHocKy}
            ngayBatDau={term.ngayBatDau}
            studentLabel={[user?.username, user?.hoTen].filter(Boolean).join(' · ')}
            // Nút đặt lại dữ liệu chỉ có nghĩa khi đang chạy bản giả.
            demo={API_MODE === 'mock'}
          />
        ) : null}
      </Panel>
    </StudentShell>
  )
}
