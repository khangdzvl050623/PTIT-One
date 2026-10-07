/**
 * Một công tắc duy nhất cho cả app: dùng dữ liệu giả để dựng UI, hay gọi API thật.
 *
 * Đặt `VITE_API_MODE=api` trong `apps/web/.env.local` để gọi API thật.
 * Mặc định là `mock` nên ai chỉ làm giao diện không cần SQL Server và backend.
 *
 * ⚠️ Cố ý chỉ có MỘT cờ cho mọi feature. Nếu mỗi feature một cờ thì sẽ có lúc
 * auth là thật mà bảng điểm là giả — màn hình trông như chạy được nhưng số liệu
 * không thuộc về ai, và đó là loại lỗi rất khó nhận ra khi demo.
 */
export const API_MODE: 'api' | 'mock' =
  import.meta.env.VITE_API_MODE === 'api' ? 'api' : 'mock'

/** Chọn bản cài đặt theo cờ. Hai bản phải cùng chữ ký để màn hình không phải biết. */
export function pickApi<T>(real: T, mock: T): T {
  return API_MODE === 'api' ? real : mock
}
