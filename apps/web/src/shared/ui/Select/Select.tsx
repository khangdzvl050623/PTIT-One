import { useEffect, useId, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'

import { Icon } from '../Icon'

import styles from './Select.module.scss'

export interface SelectOption<T extends string | number> {
  value: T
  label: string
}

export interface SelectProps<T extends string | number> {
  value: T
  options: readonly SelectOption<T>[]
  onChange: (value: T) => void
  /** Tên cho trình đọc màn hình khi không có nhãn hiển thị. */
  ariaLabel: string
  className?: string
  disabled?: boolean
}

/**
 * Ô chọn tự vẽ danh sách — thay `<select>` để danh sách xổ ra theo giao diện
 * web thay vì menu của hệ điều hành.
 *
 * Theo mẫu "select-only combobox" của WAI-ARIA: focus luôn ở nút, mục đang trỏ
 * báo qua `aria-activedescendant`. Bàn phím: ↑ ↓ Home End PageUp PageDown,
 * Enter/Space chọn, Esc đóng, Tab đóng không đổi giá trị.
 */
export function Select<T extends string | number>({
  value,
  options,
  onChange,
  ariaLabel,
  className,
  disabled = false,
}: SelectProps<T>) {
  const baseId = useId()
  const listId = `${baseId}-list`
  const rootRef = useRef<HTMLDivElement>(null)
  const listRef = useRef<HTMLUListElement>(null)
  const [open, setOpen] = useState(false)
  const selectedIndex = Math.max(
    0,
    options.findIndex((o) => o.value === value),
  )
  const [activeIndex, setActiveIndex] = useState(selectedIndex)
  const selected = options[selectedIndex]

  // Bấm ra ngoài thì đóng.
  useEffect(() => {
    if (!open) return
    function onPointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [open])

  // Mục đang trỏ luôn nằm trong vùng nhìn thấy của danh sách.
  useEffect(() => {
    if (!open) return
    const item = listRef.current?.children[activeIndex] as HTMLElement | undefined
    item?.scrollIntoView({ block: 'nearest' })
  }, [open, activeIndex])

  function openList() {
    setActiveIndex(selectedIndex)
    setOpen(true)
  }

  function choose(index: number) {
    const option = options[index]
    if (option && option.value !== value) onChange(option.value)
    setOpen(false)
  }

  function move(next: number) {
    setActiveIndex(Math.min(options.length - 1, Math.max(0, next)))
  }

  function onKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (!open) {
      if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) {
        event.preventDefault()
        openList()
      }
      return
    }

    const keys: Record<string, () => void> = {
      ArrowDown: () => move(activeIndex + 1),
      ArrowUp: () => move(activeIndex - 1),
      Home: () => move(0),
      End: () => move(options.length - 1),
      PageDown: () => move(activeIndex + 10),
      PageUp: () => move(activeIndex - 10),
      Enter: () => choose(activeIndex),
      ' ': () => choose(activeIndex),
      Escape: () => setOpen(false),
    }
    const action = keys[event.key]
    if (action) {
      event.preventDefault()
      action()
    } else if (event.key === 'Tab') {
      setOpen(false)
    }
  }

  return (
    <div ref={rootRef} className={[styles.root, className].filter(Boolean).join(' ')}>
      <button
        type="button"
        role="combobox"
        className={styles.trigger}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        aria-activedescendant={open ? `${baseId}-${activeIndex}` : undefined}
        disabled={disabled}
        onClick={() => (open ? setOpen(false) : openList())}
        onKeyDown={onKeyDown}
      >
        <span className={styles.value}>{selected?.label ?? ''}</span>
        <Icon name="chevronDown" size="12px" className={open ? styles.caretOpen : styles.caret} />
      </button>

      {open ? (
        <ul ref={listRef} id={listId} role="listbox" aria-label={ariaLabel} className={styles.list}>
          {options.map((option, index) => (
            <li
              key={String(option.value)}
              id={`${baseId}-${index}`}
              role="option"
              aria-selected={index === selectedIndex}
              className={[
                styles.option,
                index === activeIndex ? styles.active : '',
                index === selectedIndex ? styles.selected : '',
              ].join(' ')}
              /* Giữ focus ở nút: mousedown trên mục không được cướp focus. */
              onMouseDown={(event) => event.preventDefault()}
              onMouseEnter={() => setActiveIndex(index)}
              onClick={() => choose(index)}
            >
              {option.label}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
