import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '@/shared/api'

import * as api from '../api/mockEnrollmentApi'
import type {
  BestResults,
  ClassOffer,
  CourseSummary,
  EnrollmentPeriod,
  StudentEnrollments,
  StudentProgram,
} from '../types'

export interface Notice {
  tone: 'success' | 'error'
  text: string
}

const LOAI_LABEL = {
  HOC_MOI: '',
  HOC_LAI: ' (học lại)',
  CAI_THIEN: ' (học cải thiện)',
} as const

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/**
 * Trạng thái màn đăng ký: lớp mở, môn đã đăng ký, đợt, lớp đang chờ server.
 * Sau mỗi lần ghi đều tải lại cả hai danh sách — sĩ số và tín chỉ lấy từ
 * server, không tự cộng trừ ở client (người khác cũng đang đăng ký cùng lúc).
 */
export function useEnrollment(maHocKy: string) {
  const [classes, setClasses] = useState<ClassOffer[]>([])
  const [mine, setMine] = useState<StudentEnrollments | null>(null)
  const [period, setPeriod] = useState<EnrollmentPeriod | null>(null)
  // Dữ liệu tham chiếu cho bộ lọc — tải một lần, không đổi khi đăng ký/huỷ.
  const [program, setProgram] = useState<StudentProgram | null>(null)
  const [catalog, setCatalog] = useState<CourseSummary[]>([])
  const [results, setResults] = useState<BestResults>({})
  const [loading, setLoading] = useState(true)
  /** `maLopHP` đang chờ — khoá đúng nút đó, tránh bấm lặp. */
  const [pending, setPending] = useState<string | null>(null)
  const [notice, setNotice] = useState<Notice | null>(null)

  const refresh = useCallback(async () => {
    const [c, m, p] = await Promise.all([
      api.listOpenClasses(maHocKy),
      api.myEnrollments(maHocKy),
      api.currentPeriod(),
    ])
    setClasses(c)
    setMine(m)
    setPeriod(p)
  }, [maHocKy])

  useEffect(() => {
    let cancelled = false
    void Promise.all([api.myProgram(), api.courseCatalog(), api.myBestResults()]).then(
      ([p, c, r]) => {
        if (cancelled) return
        setProgram(p)
        setCatalog(c)
        setResults(r)
      },
      () => undefined,
    )
    void refresh()
      .catch(() => {
        if (!cancelled) setNotice({ tone: 'error', text: NETWORK })
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [refresh])

  const run = useCallback(
    async (maLopHP: string, action: () => Promise<string>) => {
      setPending(maLopHP)
      setNotice(null)
      try {
        setNotice({ tone: 'success', text: await action() })
      } catch (cause) {
        // Server từ chối: hiện đúng câu của server (lớp đầy, trùng lịch…).
        setNotice({ tone: 'error', text: cause instanceof ApiError ? cause.message : NETWORK })
      } finally {
        await refresh().catch(() => undefined)
        setPending(null)
      }
    },
    [refresh],
  )

  const register = useCallback(
    (lop: ClassOffer) =>
      run(lop.maLopHP, async () => {
        const { result, created } = await api.register(lop.maLopHP)
        return created
          ? `Đã đăng ký lớp ${lop.maLopHP} — ${lop.tenMonHoc}${LOAI_LABEL[result.loaiDangKy]}.`
          : `Bạn đã giữ chỗ lớp ${lop.maLopHP} từ trước.`
      }),
    [run],
  )

  const cancel = useCallback(
    (maLopHP: string, tenMonHoc: string) =>
      run(maLopHP, async () => {
        await api.cancel(maLopHP, maHocKy)
        return `Đã huỷ đăng ký ${tenMonHoc}; tín chỉ và chỗ đã được trả lại.`
      }),
    [run, maHocKy],
  )

  const resetDemo = useCallback(async () => {
    api.resetDemo()
    setNotice(null)
    await refresh()
  }, [refresh])

  return {
    classes,
    mine,
    period,
    program,
    catalog,
    results,
    loading,
    pending,
    notice,
    dismiss: () => setNotice(null),
    register,
    cancel,
    resetDemo,
  }
}
