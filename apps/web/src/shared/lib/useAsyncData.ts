import { useCallback, useEffect, useRef, useState } from 'react'

import { ApiError } from '@/shared/api'

/**
 * Tải dữ liệu một lần và theo dõi trạng thái. Đủ cho các màn chỉ đọc của
 * Phần 1; chưa cần thư viện cache nào.
 *
 * ⚠️ `load` phải ổn định giữa các lần render — bọc `useCallback` ở chỗ gọi,
 * nếu không hook sẽ tải lại vô hạn.
 */
export interface AsyncData<T> {
  data: T | null
  /** `true` cho tới khi lần tải đầu xong, kể cả khi thất bại. */
  loading: boolean
  /** Câu hiển thị được cho người dùng; `null` khi không lỗi. */
  error: string | null
  reload: () => void
}

export function useAsyncData<T>(load: () => Promise<T>): AsyncData<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [nonce, setNonce] = useState(0)
  /* Bỏ kết quả của lần tải đã cũ: đổi học kỳ nhanh hai lần thì phản hồi có thể
     về sai thứ tự và ghi đè dữ liệu mới bằng dữ liệu cũ. */
  const latest = useRef(0)

  useEffect(() => {
    const ticket = ++latest.current
    let cancelled = false
    setLoading(true)

    void load()
      .then((result) => {
        if (cancelled || ticket !== latest.current) return
        setData(result)
        setError(null)
      })
      .catch((cause: unknown) => {
        if (cancelled || ticket !== latest.current) return
        setData(null)
        setError(message(cause))
      })
      .finally(() => {
        if (!cancelled && ticket === latest.current) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [load, nonce])

  const reload = useCallback(() => setNonce((n) => n + 1), [])

  return { data, loading, error, reload }
}

/** `ApiError` đã mang câu tiếng Việt từ server; còn lại là lỗi mạng. */
function message(cause: unknown): string {
  if (cause instanceof ApiError) {
    return cause.message
  }
  return 'Không kết nối được tới máy chủ. Kiểm tra API đang chạy chưa.'
}
