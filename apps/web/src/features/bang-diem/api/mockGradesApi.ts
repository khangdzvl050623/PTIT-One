import { currentMockUser } from '@/features/auth/api/mockAuthApi'
import { publishedGradeOf } from '@/features/nhap-diem/api/mockTeachingApi'
import { NGUONG_DAT } from '@/shared/lib'

import { DEMO_GRADES } from '../data/grades'
import { summarize } from '../lib/grading'
import type { StudentGrade, TermSummary } from '../types'

/**
 * Bản giả; cùng chữ ký với `httpGradesApi`.
 *
 * `DEMO_GRADES` là **nền** (lịch sử các kỳ trước, đã công bố sẵn). Lên trên nền
 * đó, phủ điểm mà giảng viên vừa công bố ở màn nhập điểm — nếu không thì công
 * bố điểm xong màn sinh viên không đổi gì và trông như lỗi, dù chỉ là hai bản
 * giả không gặp nhau.
 *
 * Ở API thật không cần ghép gì: cả hai màn đọc cùng bảng `Diem`.
 */

const LATENCY_MS = 300

/**
 * Phủ điểm đã công bố của sinh viên đang đăng nhập lên các dòng của `DEMO_GRADES`.
 *
 * Chỉ phủ dòng ĐÃ CÔNG BỐ — điểm nháp không lộ cho sinh viên, giống server.
 * Không phải sinh viên (GV, admin) thì trả nền nguyên vẹn.
 */
function withPublishedGrades(): StudentGrade[] {
  const user = currentMockUser()
  const maSinhVien = user?.role === 'SINH_VIEN' ? user.entityId : null
  if (!maSinhVien) {
    return [...DEMO_GRADES]
  }

  return DEMO_GRADES.map((row) => {
    const published = publishedGradeOf(row.maLopHP, maSinhVien)
    if (!published || published.diemTongKet === null) {
      return row
    }
    return {
      ...row,
      diemChuyenCan: published.diemChuyenCan,
      diemGiuaKy: published.diemGiuaKy,
      diemCuoiKy: published.diemCuoiKy,
      diemTongKet: published.diemTongKet,
      ketQua: published.diemTongKet >= NGUONG_DAT ? 'DAT' : 'KHONG_DAT',
      daCongBo: true,
      ngayCongBo: published.ngayCongBo,
    }
  })
}

export async function fetchGrades(): Promise<readonly StudentGrade[]> {
  await delay()
  return withPublishedGrades()
}

/* Bản giả phải tự tính vì không có backend — dùng `lib/grading.ts`, nơi giữ
   bản sao quy tắc cho chế độ mock. */
export async function fetchTranscript(): Promise<readonly TermSummary[]> {
  await delay()
  return summarize(withPublishedGrades())
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}
