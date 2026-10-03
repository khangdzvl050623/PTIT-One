import { useMemo } from 'react'

import { useAuth } from '@/features/auth'
import { DEMO_GRADES } from '@/features/bang-diem'
import { ProgramView } from '@/features/dang-ky'
import type { StudyRecord } from '@/features/dang-ky'
import { Panel } from '@/shared/ui'

import { StudentShell } from './StudentShell'

/**
 * Chương trình đào tạo — kế hoạch (`GET /api/programs/{maCTDT}`) và thực hiện
 * (bảng điểm `GET /api/me/grades`). Trang ghép hai nguồn vì hai feature không
 * import lẫn nhau.
 */
export function ProgramPage() {
  const { user } = useAuth()
  const history = useMemo<StudyRecord[]>(
    () =>
      DEMO_GRADES.map((g) => ({
        maHocKy: g.maHocKy,
        tenHocKy: g.tenHocKy,
        maMonHoc: g.maMonHoc,
        tenMonHoc: g.tenMonHoc,
        soTinChi: g.soTinChi,
        diemTongKet: g.diemTongKet,
        ketQua: g.ketQua,
      })),
    [],
  )

  return (
    <StudentShell>
      <Panel title="CHƯƠNG TRÌNH ĐÀO TẠO" icon="graduate">
        <ProgramView
          history={history}
          exportName={`chuong-trinh-dao-tao-${user?.username ?? 'sinh-vien'}`}
        />
      </Panel>
    </StudentShell>
  )
}
