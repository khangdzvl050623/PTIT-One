import { ApiError } from '@/shared/api'

import { CAMPUSES, DEMO_UPDATED_AT, REPORT_DATA } from '../data/demo'
import type { ScopeExtras } from '../data/demo'
import type { CourseReport, CourseRow, ReportSummary } from '../types'

/**
 * Thống kê GIẢ — cùng hình dạng `GET /api/reports/summary` và `/courses`.
 * `maCoSo = null` là toàn trường (chỉ Admin Master). Phân quyền theo cơ sở
 * do SERVER làm (admin cơ sở gửi cơ sở khác → 403); bản giả nhận `viewerCampus`
 * để mô phỏng đúng lỗi đó.
 */

const LATENCY_MS = 300
const delay = () => new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))

function guard(maCoSo: string | null, viewerCampus: string | null): void {
  if (viewerCampus !== null && maCoSo !== viewerCampus) {
    throw new ApiError(403, {
      code: 'AUTH_FORBIDDEN',
      message: 'Admin cơ sở chỉ xem được thống kê của cơ sở mình.',
    })
  }
}

function datasets(maHocKy: string, maCoSo: string | null) {
  const term = REPORT_DATA[maHocKy] ?? {}
  const keys = maCoSo ? [maCoSo] : CAMPUSES.map((c) => c.maCoSo)
  return keys.map((k) => term[k]).filter((d) => d !== undefined)
}

/** Gộp các cơ sở theo môn — cộng số, tính lại tỉ lệ từ tổng (không lấy trung bình %). */
function mergeRows(groups: readonly (readonly CourseRow[])[]): CourseRow[] {
  const byCourse = new Map<string, CourseRow>()
  for (const rows of groups) {
    for (const r of rows) {
      const prev = byCourse.get(r.maMonHoc)
      if (!prev) {
        byCourse.set(r.maMonHoc, { ...r, phanBoDiem: [...r.phanBoDiem] })
        continue
      }
      byCourse.set(r.maMonHoc, {
        ...prev,
        soLop: prev.soLop + r.soLop,
        luotDangKy: prev.luotDangKy + r.luotDangKy,
        tongSucChua: prev.tongSucChua + r.tongSucChua,
        tongDaDangKy: prev.tongDaDangKy + r.tongDaDangKy,
        soDat: prev.soDat + r.soDat,
        soTruot: prev.soTruot + r.soTruot,
        chuaCoKetQua: prev.chuaCoKetQua + r.chuaCoKetQua,
        phanBoDiem: prev.phanBoDiem.map((n, i) => n + (r.phanBoDiem[i] ?? 0)),
      })
    }
  }
  return [...byCourse.values()].map((r) => ({ ...r, tiLeLapDay: ratio(r.tongDaDangKy, r.tongSucChua) }))
}

function ratio(part: number, whole: number): number | null {
  return whole ? Math.round((part / whole) * 10_000) / 10_000 : null
}

export async function getCourseReport(
  maHocKy: string,
  maCoSo: string | null,
  viewerCampus: string | null,
): Promise<CourseReport> {
  await delay()
  guard(maCoSo, viewerCampus)
  return {
    phamVi: { maHocKy, maCoSo },
    monHoc: mergeRows(datasets(maHocKy, maCoSo).map((d) => d.rows)),
    capNhatLuc: DEMO_UPDATED_AT,
  }
}

export async function getSummary(
  maHocKy: string,
  maCoSo: string | null,
  viewerCampus: string | null,
): Promise<ReportSummary> {
  await delay()
  guard(maCoSo, viewerCampus)
  const sets = datasets(maHocKy, maCoSo)
  const rows = sets.flatMap((d) => d.rows)
  const extra = sets.reduce<ScopeExtras>(
    (s, d) => ({
      soSinhVien: s.soSinhVien + d.extras.soSinhVien,
      soLopDay: s.soLopDay + d.extras.soLopDay,
      daKhoa: s.daKhoa + d.extras.daKhoa,
      daCongBo: s.daCongBo + d.extras.daCongBo,
    }),
    { soSinhVien: 0, soLopDay: 0, daKhoa: 0, daCongBo: 0 },
  )
  const soLop = rows.reduce((s, r) => s + r.soLop, 0)
  const tongSucChua = rows.reduce((s, r) => s + r.tongSucChua, 0)
  const tongDaDangKy = rows.reduce((s, r) => s + r.tongDaDangKy, 0)
  return {
    phamVi: { maHocKy, maCoSo },
    dangKy: { soLop, luotDangKy: tongDaDangKy, soSinhVien: extra.soSinhVien },
    sucChua: {
      tongSucChua,
      tongDaDangKy,
      tiLeLapDay: ratio(tongDaDangKy, tongSucChua),
      soLopDay: extra.soLopDay,
      soLopConCho: soLop - extra.soLopDay,
    },
    tienDoDiem: {
      daKhoa: extra.daKhoa,
      daCongBo: extra.daCongBo,
      chuaCongBo: soLop - extra.daKhoa - extra.daCongBo,
    },
    capNhatLuc: DEMO_UPDATED_AT,
  }
}
