import { useState } from 'react'

import { Icon, Logo } from '@/shared/ui'

import styles from './IdPhoto.module.scss'

export interface IdPhotoProps {
  /** URL ảnh đại diện (`SinhVien.AnhDaiDien`); bỏ trống thì hiện khung mặc định. */
  src?: string | null
  /** Tên người trong ảnh, cho `alt`. */
  hoTen?: string
}

/**
 * Ảnh thẻ 3×4. Có `src` thì hiện ảnh đã lưu, không thì hiện khung mặc định kèm
 * logo Học viện.
 *
 * Ảnh nằm ở dịch vụ ngoài nên liên kết có thể chết (ảnh bị xoá, hết hạn chia
 * sẻ). Khi đó quay về khung mặc định chứ không để trình duyệt hiện icon ảnh
 * lỗi — trang hồ sơ trông hỏng vì một thứ không thuộc dữ liệu nghiệp vụ.
 */
export function IdPhoto({ src, hoTen }: IdPhotoProps) {
  /* Lưu chính URL đã lỗi, không phải cờ boolean: dán liên kết mới sau một lần
     lỗi thì phải thử lại URL mới, chứ không giữ nguyên khung mặc định. */
  const [failed, setFailed] = useState<string | null>(null)

  if (src && failed !== src) {
    return (
      <img
        className={styles.photo}
        src={src}
        alt={hoTen ? `Ảnh đại diện của ${hoTen}` : 'Ảnh đại diện'}
        onError={() => setFailed(src)}
      />
    )
  }

  return (
    <div className={styles.photo} role="img" aria-label="Chưa có ảnh thẻ">
      <span className={styles.logo}>
        <Logo size="md" variant="tile" alt="" />
      </span>
      <Icon name="user" className={styles.figure} />
    </div>
  )
}
