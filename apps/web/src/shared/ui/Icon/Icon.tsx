import styles from './Icon.module.scss'

/** Bộ icon dùng chung — vẽ SVG trực tiếp, không phụ thuộc thư viện icon ngoài. */
export type IconName =
  | 'user'
  | 'lock'
  | 'signIn'
  | 'bullhorn'
  | 'users'
  | 'graduate'
  | 'chalkboard'

/** Mỗi icon gồm các subpath vẽ theo khung 24×24, tô bằng `currentColor`. */
const PATHS: Record<IconName, readonly string[]> = {
  user: [
    'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z',
    'M12 14c-4.42 0-8 2.51-8 5.6V21h16v-1.4c0-3.09-3.58-5.6-8-5.6Z',
  ],
  lock: [
    'M7 10V8a5 5 0 0 1 10 0v2h1.5A1.5 1.5 0 0 1 20 11.5v8A1.5 1.5 0 0 1 18.5 21h-13A1.5 1.5 0 0 1 4 19.5v-8A1.5 1.5 0 0 1 5.5 10H7Zm2 0h6V8a3 3 0 1 0-6 0v2Z',
  ],
  signIn: [
    'M3 11h8.17l-3.58-3.59L9 6l6 6-6 6-1.41-1.41L11.17 13H3v-2Z',
    'M16 3h5v18h-5v-2h3V5h-3V3Z',
  ],
  bullhorn: [
    'M3 9.5h3.2L15.5 4.7v14.6L6.2 14.5H3v-5Z',
    'M8.2 15.9 9.5 21h2.6l-1.2-5.1H8.2Z',
    'M17.5 9.3a1 1 0 0 1 1.4.4 6.4 6.4 0 0 1 0 4.6 1 1 0 0 1-1.8-.9 4.4 4.4 0 0 0 0-3.2 1 1 0 0 1 .4-.9Z',
  ],
  users: [
    'M9 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z',
    'M9 13.5c-3.9 0-7 2.2-7 5V20h14v-1.5c0-2.8-3.1-5-7-5Z',
    'M17 11.6a3.3 3.3 0 1 0 0-6.6 3.3 3.3 0 0 0 0 6.6Z',
    'M17.4 12.8c2.7.4 4.6 1.9 4.6 3.7V20h-4.2v-1.5c0-1.5-.7-2.8-1.8-3.7.4-.2 1-.2 1.4 0Z',
  ],
  graduate: [
    'M12 4 1.5 9 12 14l10.5-5L12 4Z',
    'M6.8 13.6V16c0 1.7 2.3 3 5.2 3s5.2-1.3 5.2-3v-2.4l-5.2 2.5-5.2-2.5Z',
  ],
  chalkboard: ['M3 5h18v12H3V5Zm2 2v8h14V7H5Z', 'M6 18.4h12V20H6Z'],
}

export interface IconProps {
  name: IconName
  /**
   * Cỡ icon (ví dụ `'12px'`, `'1.5em'`). Bỏ trống thì icon ăn theo `font-size`
   * của chỗ đặt (mặc định CSS là `1em`).
   */
  size?: string
  className?: string
}

export function Icon({ name, size, className }: IconProps) {
  const classes = [styles.icon, className].filter(Boolean).join(' ')

  return (
    <svg
      className={classes}
      style={size ? { width: size, height: size } : undefined}
      viewBox="0 0 24 24"
      fill="currentColor"
      fillRule="evenodd"
      aria-hidden="true"
      focusable="false"
    >
      {PATHS[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  )
}
