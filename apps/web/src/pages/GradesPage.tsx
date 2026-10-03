import { useAuth } from '@/features/auth'
import { fetchGrades, GradeTable } from '@/features/bang-diem'
import { useAsyncData } from '@/shared/lib'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Xem điểm (F07). Dữ liệu từ `GET /api/me/grades`, hoặc dữ liệu giả khi
 * `VITE_API_MODE` chưa đặt thành `api`.
 */
export function GradesPage() {
  const { user } = useAuth()
  const { data, loading, error } = useAsyncData(fetchGrades)

  return (
    <StudentShell>
      <Panel title="XEM ĐIỂM" icon="graduate">
        {loading ? <p>Đang tải bảng điểm…</p> : null}
        {error ? <p role="alert">{error}</p> : null}
        {data ? (
          <GradeTable
            grades={data}
            exportName={`bang-diem-${user?.username ?? 'sinh-vien'}`}
          />
        ) : null}
      </Panel>
    </StudentShell>
  )
}
