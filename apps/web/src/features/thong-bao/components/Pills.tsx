import styles from './Pills.module.scss'

export interface PillOption<T extends string> {
  value: T
  label: string
  /** Hiện ` (n)` sau nhãn. Bỏ trống cho tab không đếm (hub GV). */
  count?: number
}

export interface PillsProps<T extends string> {
  options: readonly PillOption<T>[]
  value: T
  onChange: (value: T) => void
  ariaLabel: string
}

/**
 * Cụm pill lọc dùng chung cho màn thông báo (hộp thư đến/gửi, tab hub GV).
 * Viên thuốc viền mảnh, mục chọn nền đỏ — cùng một chỗ để bốn màn không lệch
 * nhau vài pixel.
 */
export function Pills<T extends string>({ options, value, onChange, ariaLabel }: PillsProps<T>) {
  return (
    <div className={styles.pills} role="group" aria-label={ariaLabel}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={`${styles.pill} ${value === option.value ? styles.active : ''}`}
          aria-pressed={value === option.value}
          onClick={() => onChange(option.value)}
        >
          {option.label}
          {option.count !== undefined ? ` (${option.count})` : null}
        </button>
      ))}
    </div>
  )
}
