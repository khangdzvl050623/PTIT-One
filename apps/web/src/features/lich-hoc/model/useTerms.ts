import { useCallback, useMemo } from 'react'

import { useAsyncData } from '@/shared/lib'

import { fetchTerms } from '../api/timetableApi'
import type { Term } from '../types'

export interface Terms {
  /** Mới nhất trước — thứ tự dùng cho ô chọn. */
  terms: readonly Term[]
  /**
   * Học kỳ chọn sẵn: kỳ chứa hôm nay, không có thì kỳ mới nhất. Rỗng khi chưa
   * tải xong — màn hình phải chờ giá trị này thay vì tự viết cứng một mã.
   */
  defaultTerm: string
  loading: boolean
  error: string | null
}

/**
 * Danh mục học kỳ cho mọi ô chọn kỳ.
 *
 * ⚠️ Đừng viết cứng mã học kỳ trong component. Mã của dữ liệu mẫu
 * (`2026-2027-HK1`) KHÁC mã trong database thật (`2026-1`), nên hằng số viết
 * cứng sẽ làm màn hình rỗng im lặng khi bật `VITE_API_MODE=api` — không lỗi,
 * không thông báo, chỉ là không có dữ liệu.
 */
export function useTerms(): Terms {
  const load = useCallback(async () => {
    const terms = await fetchTerms()
    return [...terms].sort((a, b) => b.maHocKy.localeCompare(a.maHocKy))
  }, [])

  const { data, loading, error } = useAsyncData(load)

  const defaultTerm = useMemo(() => {
    const terms = data ?? []
    const today = new Date().toISOString().slice(0, 10)
    const current = terms.find((t) => t.ngayBatDau <= today && today <= t.ngayKetThuc)
    return current?.maHocKy ?? terms[0]?.maHocKy ?? ''
  }, [data])

  return { terms: data ?? [], defaultTerm, loading, error }
}
