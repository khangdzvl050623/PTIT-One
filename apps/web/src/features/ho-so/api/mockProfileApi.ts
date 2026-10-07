import { fetchGrades } from '@/features/bang-diem/api/mockGradesApi'

import { DEMO_PROFILE, DEMO_SUMMARY } from '../data/profile'
import type { CourseResult, StudentProfile, StudentSummary, TermResults } from '../types'

/**
 * Bản giả để dựng UI khi chưa chạy backend. Cùng chữ ký với `httpProfileApi`
 * nên màn hình không phân biệt được hai bản.
 */

/** Đủ lâu để thấy trạng thái "Đang tải…" khi thiết kế. */
const LATENCY_MS = 300

export async function fetchProfile(): Promise<StudentProfile> {
  await delay()
  return DEMO_PROFILE
}

export async function fetchSummary(): Promise<StudentSummary> {
  await delay()
  return DEMO_SUMMARY
}

/**
 * Biểu đồ điểm dùng CÙNG nguồn với bảng điểm, nên nó cũng thấy điểm giảng viên
 * vừa công bố. Dùng `DEMO_RESULTS` riêng thì hai màn cùng nói về một thứ mà ra
 * hai số khác nhau.
 */
export async function fetchResults(): Promise<readonly TermResults[]> {
  const grades = await fetchGrades()
  const theoKy = new Map<string, TermResults>()
  for (const g of grades) {
    const mon: CourseResult = {
      maMonHoc: g.maMonHoc,
      tenMonHoc: g.tenMonHoc,
      diemTongKet: g.diemTongKet,
    }
    const ky = theoKy.get(g.maHocKy)
    if (ky) {
      theoKy.set(g.maHocKy, { ...ky, monHoc: [...ky.monHoc, mon] })
    } else {
      theoKy.set(g.maHocKy, { maHocKy: g.maHocKy, tenHocKy: g.tenHocKy, monHoc: [mon] })
    }
  }
  // Kỳ cũ trước, cho trục thời gian đi xuôi — như bản http.
  return [...theoKy.values()].sort((a, b) => a.maHocKy.localeCompare(b.maHocKy))
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}
