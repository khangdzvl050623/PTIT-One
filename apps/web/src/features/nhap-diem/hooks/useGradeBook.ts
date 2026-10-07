import { useCallback, useEffect, useMemo, useState } from 'react'

import { ApiError } from '@/shared/api'

import * as api from '../api/teachingApi'
import type { DraftScores, GradeEntry, GradeSheet, SaveGradeRow, TeachingClass } from '../types'

export interface Notice {
  tone: 'success' | 'error'
  text: string
}

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/** Ô trống nghĩa là **xoá** điểm (`null`), không phải 0. */
export function parseScore(raw: string): number | null {
  const text = raw.trim().replace(',', '.')
  if (text === '') return null
  const value = Number(text)
  return Number.isFinite(value) ? value : Number.NaN
}

function toDraft(row: GradeEntry): DraftScores {
  return {
    diemChuyenCan: row.diemChuyenCan?.toString() ?? '',
    diemGiuaKy: row.diemGiuaKy?.toString() ?? '',
    diemCuoiKy: row.diemCuoiKy?.toString() ?? '',
  }
}

function draftsOf(sheet: GradeSheet): Record<string, DraftScores> {
  return Object.fromEntries(sheet.diem.map((row) => [row.maSinhVien, toDraft(row)]))
}

/**
 * Trạng thái màn nhập điểm của một lớp.
 *
 * Người dùng gõ vào `drafts` (chuỗi), không gõ thẳng vào `sheet` — nhờ vậy
 * biết được dòng nào đã đổi để chỉ gửi đúng những dòng đó, và huỷ sửa là bỏ
 * `drafts` đi. Sau mỗi lần ghi, `sheet` lấy lại từ server: `version` và điểm
 * tổng kết do server quyết, client không tự suy ra.
 */
export function useGradeBook(maHocKy: string) {
  const [classes, setClasses] = useState<TeachingClass[] | null>(null)
  const [maLopHP, setMaLopHP] = useState<string | null>(null)
  const [sheet, setSheet] = useState<GradeSheet | null>(null)
  const [drafts, setDrafts] = useState<Record<string, DraftScores>>({})
  const [loadingSheet, setLoadingSheet] = useState(false)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<Notice | null>(null)

  // Đổi học kỳ: tải lại danh sách lớp và chọn lớp đầu tiên của kỳ đó.
  useEffect(() => {
    let cancelled = false
    setClasses(null)
    void api.teachingClasses(maHocKy).then(
      (list) => {
        if (cancelled) return
        setClasses(list)
        setMaLopHP(list[0]?.maLopHP ?? null)
      },
      () => {
        if (!cancelled) setNotice({ tone: 'error', text: NETWORK })
      },
    )
    return () => {
      cancelled = true
    }
  }, [maHocKy])

  const applySheet = useCallback((next: GradeSheet) => {
    setSheet(next)
    setDrafts(draftsOf(next))
  }, [])

  const reload = useCallback(async () => {
    if (!maLopHP) return
    setLoadingSheet(true)
    try {
      applySheet(await api.sheet(maLopHP))
    } catch (cause) {
      setNotice({ tone: 'error', text: cause instanceof ApiError ? cause.message : NETWORK })
    } finally {
      setLoadingSheet(false)
    }
  }, [maLopHP, applySheet])

  useEffect(() => {
    if (!maLopHP) {
      setSheet(null)
      setDrafts({})
      return
    }
    setNotice(null)
    void reload()
  }, [maLopHP, reload])

  /** Dòng đã đổi so với bảng vừa tải — chỉ những dòng này được gửi lên. */
  const changed = useMemo(() => {
    if (!sheet) return []
    return sheet.diem.filter((row) => {
      const draft = drafts[row.maSinhVien]
      if (!draft) return false
      const goc = toDraft(row)
      return (
        draft.diemChuyenCan !== goc.diemChuyenCan ||
        draft.diemGiuaKy !== goc.diemGiuaKy ||
        draft.diemCuoiKy !== goc.diemCuoiKy
      )
    })
  }, [sheet, drafts])

  /** Ô gõ sai (không phải số, ngoài 0–10, quá 1 chữ số thập phân). */
  const invalid = useMemo(() => {
    const bad = new Set<string>()
    for (const [maSinhVien, draft] of Object.entries(drafts)) {
      for (const raw of [draft.diemChuyenCan, draft.diemGiuaKy, draft.diemCuoiKy]) {
        const value = parseScore(raw)
        if (value === null) continue
        if (Number.isNaN(value) || value < 0 || value > 10 || Math.round(value * 10) !== value * 10) {
          bad.add(maSinhVien)
        }
      }
    }
    return bad
  }, [drafts])

  const edit = useCallback((maSinhVien: string, field: keyof DraftScores, value: string) => {
    setDrafts((prev) => {
      const current = prev[maSinhVien] ?? { diemChuyenCan: '', diemGiuaKy: '', diemCuoiKy: '' }
      return { ...prev, [maSinhVien]: { ...current, [field]: value } }
    })
  }, [])

  const discard = useCallback(() => {
    if (sheet) setDrafts(draftsOf(sheet))
    setNotice(null)
  }, [sheet])

  const run = useCallback(
    async (action: () => Promise<GradeSheet>, done: (next: GradeSheet) => string) => {
      setBusy(true)
      setNotice(null)
      try {
        const next = await action()
        applySheet(next)
        setNotice({ tone: 'success', text: done(next) })
      } catch (cause) {
        setNotice({ tone: 'error', text: cause instanceof ApiError ? cause.message : NETWORK })
        /* Phiên bản lệch nghĩa là bảng trên màn đã cũ. Tải lại để người dùng
           nhập trên số liệu đúng — tuyệt đối không ghi đè bản của người kia. */
        if (cause instanceof ApiError && cause.code === 'GRADE_VERSION_CONFLICT') await reload()
      } finally {
        setBusy(false)
      }
    },
    [applySheet, reload],
  )

  const saveDraft = useCallback(() => {
    if (!maLopHP || changed.length === 0) return Promise.resolve()
    const rows: SaveGradeRow[] = changed.map((row) => {
      const draft = drafts[row.maSinhVien]!
      return {
        maSinhVien: row.maSinhVien,
        diemChuyenCan: parseScore(draft.diemChuyenCan),
        diemGiuaKy: parseScore(draft.diemGiuaKy),
        diemCuoiKy: parseScore(draft.diemCuoiKy),
        version: row.version,
      }
    })
    const soDong = rows.length
    return run(
      () => api.saveGrades(maLopHP, rows),
      () => `Đã lưu ${soDong} dòng điểm. Điểm còn ở dạng nháp — sinh viên chưa thấy.`,
    )
  }, [maLopHP, changed, drafts, run])

  const publish = useCallback(() => {
    if (!maLopHP) return Promise.resolve()
    return run(
      () => api.publishGrades(maLopHP),
      (next) => `Đã công bố điểm lớp ${next.lop.maLopHP}. Sinh viên xem được ngay trong bảng điểm.`,
    )
  }, [maLopHP, run])

  const resetDemo = useCallback(async () => {
    api.resetDemo()
    setNotice(null)
    await reload()
  }, [reload])

  return {
    classes,
    maLopHP,
    setMaLopHP,
    sheet,
    drafts,
    changed,
    invalid,
    loadingSheet,
    busy,
    notice,
    setNotice,
    edit,
    discard,
    saveDraft,
    publish,
    reload,
    resetDemo,
  }
}
