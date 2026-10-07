import styles from './Icon.module.scss'

/** Bộ icon dùng chung — vẽ SVG trực tiếp, không phụ thuộc thư viện icon ngoài. */
export type IconName =
  | 'user'
  | 'lock'
  | 'signIn'
  | 'signOut'
  | 'bullhorn'
  | 'users'
  | 'graduate'
  | 'chalkboard'
  | 'bell'
  | 'calendar'
  | 'book'
  | 'gear'
  | 'chevronRight'
  | 'chevronDown'
  | 'eye'
  | 'printer'
  | 'download'
  | 'search'
  | 'check'
  | 'list'
  | 'close'
  | 'eyeOff'

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
  signOut: [
    'M10 3H4.5A1.5 1.5 0 0 0 3 4.5v15A1.5 1.5 0 0 0 4.5 21H10v-2H5V5h5V3Z',
    'M15.6 7.4 14.2 8.8l2.2 2.2H8v2h8.4l-2.2 2.2 1.4 1.4 4.6-4.6-4.6-4.6Z',
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
  bell: [
    'M12 3a6 6 0 0 0-6 6v4.2L4.3 16a1 1 0 0 0 .9 1.5h13.6a1 1 0 0 0 .9-1.5L18 13.2V9a6 6 0 0 0-6-6Z',
    'M9.5 19a2.5 2.5 0 0 0 5 0h-5Z',
  ],
  calendar: [
    'M7 2h2v2h6V2h2v2h2.5A1.5 1.5 0 0 1 21 5.5v14a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 19.5v-14A1.5 1.5 0 0 1 4.5 4H7V2Zm-2 7v10h14V9H5Z',
    'M7 11h3v3H7v-3Zm5.5 0h3v3h-3v-3Z',
  ],
  // Sách mở: hai trang cong về gáy giữa — bản hai khối chữ nhật cũ trông như nút ⏸.
  book: [
    'M12 6.4C10.2 5.1 7.6 4.5 3 4.5v14c4.6 0 7.2.6 9 1.9 1.8-1.3 4.4-1.9 9-1.9v-14c-4.6 0-7.2.6-9 1.9Zm-1 11.4c-1.6-.8-3.8-1.2-6-1.3v-10c2.6.1 4.6.5 6 1.3v10Zm2 0v-10c1.4-.8 3.4-1.2 6-1.3v10c-2.2.1-4.4.5-6 1.3Z',
  ],
  gear: [
    'M10.3 2h3.4l.5 2.6 1.8.8 2.2-1.5 2.4 2.4-1.5 2.2.8 1.8 2.6.5v3.4l-2.6.5-.8 1.8 1.5 2.2-2.4 2.4-2.2-1.5-1.8.8-.5 2.6h-3.4l-.5-2.6-1.8-.8-2.2 1.5-2.4-2.4 1.5-2.2-.8-1.8L2 13.7v-3.4l2.6-.5.8-1.8-1.5-2.2 2.4-2.4 2.2 1.5 1.8-.8.5-2.6ZM12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z',
  ],
  search: [
    'M10.5 3a7.5 7.5 0 0 1 5.96 12.05l4.25 4.24-1.42 1.42-4.24-4.25A7.5 7.5 0 1 1 10.5 3Zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11Z',
  ],
  list: [
    'M4 5h2v2H4V5Zm4 0h12v2H8V5ZM4 11h2v2H4v-2Zm4 0h12v2H8v-2Zm-4 6h2v2H4v-2Zm4 0h12v2H8v-2Z',
  ],
  close: ['M6.4 5 12 10.6 17.6 5 19 6.4 13.4 12l5.6 5.6-1.4 1.4-5.6-5.6L6.4 19 5 17.6l5.6-5.6L5 6.4 6.4 5Z'],
  check: ['M9.5 15.6 5.4 11.5 4 12.9l5.5 5.5L20 7.9l-1.4-1.4-9.1 9.1Z'],
  download: [
    'M11 3h2v9.2l3.3-3.3 1.4 1.4L12 16l-5.7-5.7 1.4-1.4 3.3 3.3V3Z',
    'M4 15h2v4h12v-4h2v6H4v-6Z',
  ],
  printer: [
    'M7 3h10v5H7V3Z',
    'M4.5 9h15A1.5 1.5 0 0 1 21 10.5V17h-4v4H7v-4H3v-6.5A1.5 1.5 0 0 1 4.5 9ZM9 15v4h6v-4H9Z',
  ],
  eye: [
    'M12 5C6.5 5 2.7 9.3 1.5 12c1.2 2.7 5 7 10.5 7s9.3-4.3 10.5-7C21.3 9.3 17.5 5 12 5Zm0 11.5a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9Z',
    'M12 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5Z',
  ],
  eyeOff: [
    'M12 5C6.5 5 2.7 9.3 1.5 12c1.2 2.7 5 7 10.5 7s9.3-4.3 10.5-7C21.3 9.3 17.5 5 12 5Zm0 11.5a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9Z',
    'M3.7 2.3 21.7 20.3 20.3 21.7 2.3 3.7Z',
  ],
  chevronDown: ['M3.9 9.3 5.3 7.9 12 14.6l6.7-6.7 1.4 1.4-8.1 8.1-8.1-8.1Z'],
  chevronRight: ['M9.3 5.3 10.7 3.9 18.8 12l-8.1 8.1-1.4-1.4 6.7-6.7-6.7-6.7Z'],
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
