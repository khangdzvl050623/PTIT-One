import styles from './Skeleton.module.scss'

export interface SkeletonProps {
  /** Bề ngang, ví dụ `'60%'` hay `'120px'`. Mặc định chiếm trọn chỗ. */
  width?: string
  /** Chiều cao. Mặc định bằng một dòng chữ. */
  height?: string
  /** Bo góc; để `'50%'` khi thay cho ảnh tròn. */
  radius?: string
  className?: string
}

/**
 * Khung xương lúc chờ dữ liệu: một khối xám có vệt sáng chạy qua.
 *
 * Dùng thay cho dòng chữ "Đang tải…" ở những chỗ đã biết trước hình dạng nội
 * dung (bảng, thẻ, dòng số liệu) — người dùng thấy ngay bố cục sắp hiện ra
 * thay vì một khoảng trống, và khi dữ liệu về thì không bị giật bố cục.
 *
 * `aria-hidden`: đây là hình trang trí. Vùng đang tải tự báo cho trình đọc màn
 * hình bằng `aria-busy` ở phần tử bao ngoài, không phải bằng khối này.
 */
export function Skeleton({ width, height, radius, className }: SkeletonProps) {
  return (
    <span
      className={[styles.skeleton, className].filter(Boolean).join(' ')}
      style={{ width, height, borderRadius: radius }}
      aria-hidden="true"
    />
  )
}

/** Bề ngang các thanh, lặp vòng theo cột cho đỡ đều tăm tắp như kẻ ô. */
const WIDTHS = ['78%', '62%', '45%', '70%', '55%', '68%', '50%']

export interface SkeletonRowsProps {
  /** Số cột của bảng — phải khớp `<thead>` để các cột không lệch nhau. */
  cols: number
  /** Số dòng giả. Lấy xấp xỉ số dòng thật hay gặp là vừa. */
  rows?: number
}

/**
 * Mấy dòng khung xương đặt thẳng vào `<tbody>` của bảng đang chờ dữ liệu.
 *
 * Nằm ở `shared` vì nhiều feature cùng dùng, và vì nó không mang style riêng:
 * `<td>` thừa hưởng padding, viền, canh lề của chính bảng gọi nó.
 */
export function SkeletonRows({ cols, rows = 5 }: SkeletonRowsProps) {
  return (
    <>
      {Array.from({ length: rows }, (_, r) => (
        <tr key={r}>
          {Array.from({ length: cols }, (_, c) => (
            <td key={c}>
              <Skeleton width={WIDTHS[(r + c) % WIDTHS.length]} height="14px" />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}
