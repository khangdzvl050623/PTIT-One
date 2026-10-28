import { useState } from 'react'

import { Icon } from '@/shared/ui'

import styles from './AuthForms.module.scss'

export interface PasswordFieldProps {
  label: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  autoComplete?: string
  minLength?: number
  maxLength?: number
  disabled?: boolean
  required?: boolean
}

/**
 * Ô mật khẩu có nút mắt hiện/ẩn — dùng chung cho kích hoạt, quên mật khẩu và
 * đổi mật khẩu để ba màn cùng một cảm giác bấm.
 *
 * Icon theo trạng thái hiện tại như màn đăng nhập: đang ẩn thì mắt gạch chéo.
 */
export function PasswordField({
  label,
  value,
  onChange,
  placeholder,
  autoComplete = 'new-password',
  minLength,
  maxLength,
  disabled = false,
  required = true,
}: PasswordFieldProps) {
  const [shown, setShown] = useState(false)

  return (
    <label className={styles.field}>
      {label}
      <span className={styles.passwordWrap}>
        <input
          className={`${styles.input} ${styles.passwordInput}`}
          type={shown ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          minLength={minLength}
          maxLength={maxLength}
          disabled={disabled}
          required={required}
        />
        <button
          className={styles.eye}
          type="button"
          onClick={() => setShown((current) => !current)}
          aria-label={shown ? `Ẩn ${label.toLowerCase()}` : `Hiện ${label.toLowerCase()}`}
          aria-pressed={shown}
          disabled={disabled}
        >
          <Icon name={shown ? 'eye' : 'eyeOff'} size="18px" />
        </button>
      </span>
    </label>
  )
}
