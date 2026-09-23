import { ptitLogo } from '@/assets'
import { SITE } from '@/shared/config'

import styles from './Logo.module.scss'

/** Chiều cao logo — ánh xạ tới token trong `styles/_tokens.scss`. */
export type LogoSize = 'sm' | 'md' | 'lg'

/** `tile`: đặt logo trên nền trắng bo góc, dùng khi nền phía sau là màu đậm. */
export type LogoVariant = 'plain' | 'tile'

export interface LogoProps {
  size?: LogoSize
  variant?: LogoVariant
  /** Mô tả ảnh cho trình đọc màn hình; mặc định là tên sản phẩm. */
  alt?: string
  className?: string
}

export function Logo({
  size = 'md',
  variant = 'plain',
  alt = SITE.appName,
  className,
}: LogoProps) {
  const classes = [styles.logo, styles[size], className].filter(Boolean).join(' ')
  const img = <img className={classes} src={ptitLogo} alt={alt} />

  if (variant === 'plain') {
    return img
  }

  return <span className={`${styles.tile} ${styles[size]}`}>{img}</span>
}
