import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
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

/** Chiều cao tối đa của danh sách — cũng là mốc để quyết định xổ lên hay xuống. */
const MAX_LIST_HEIGHT = 280
/** Chiều cao tối thiểu còn chấp nhận được khi cả trên lẫn dưới đều chật. */
const MIN_LIST_HEIGHT = 96
/** Khoảng hở giữa nút và danh sách. */
const GAP = 4

/** Vị trí danh sách, tính theo khung nhìn vì danh sách dùng `position: fixed`. */
interface ListPosition {
  left: number
  width: number
  maxHeight: number
  /** Xổ xuống thì neo `top`, xổ lên thì neo `bottom` — chỉ một trong hai. */
  top?: number
  bottom?: number
}

/**
 * Đưa danh sách lên lớp trên cùng của trình duyệt. Trình duyệt chưa hỗ trợ
 * popover thì bỏ qua: danh sách vẫn hiện đúng chỗ, chỉ không thoát được khung
 * cha bị cắt.
 */
function togglePopover(element: HTMLElement | null, show: boolean): void {
  /* `isConnected`: React có thể gỡ danh sách khỏi DOM trước khi cleanup chạy,
     và trình duyệt ném `InvalidStateError` nếu gọi trên phần tử đã rời DOM —
     lúc đó popover cũng đã tự rời lớp trên cùng, không còn gì để đóng. */
  if (element?.isConnected) element.togglePopover?.(show)
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
  const triggerRef = useRef<HTMLButtonElement>(null)
  const listRef = useRef<HTMLUListElement>(null)
  const [open, setOpen] = useState(false)
  const [position, setPosition] = useState<ListPosition | null>(null)
  const selectedIndex = Math.max(
    0,
    options.findIndex((o) => o.value === value),
  )
  const [activeIndex, setActiveIndex] = useState(selectedIndex)
  const selected = options[selectedIndex]

  // Bấm ra ngoài thì đóng. Danh sách nằm ở lớp trên cùng nên phải hỏi cả hai.
  useEffect(() => {
    if (!open) return
    function onPointerDown(event: PointerEvent) {
      const target = event.target as Node
      if (!rootRef.current?.contains(target) && !listRef.current?.contains(target)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [open])

  /* Vị trí danh sách tính theo nút. Nằm ở lớp trên cùng nghĩa là danh sách
     không còn trôi theo khung cha, nên phải tính lại mỗi khi cuộn hoặc đổi cỡ
     cửa sổ. */
  const place = useCallback(() => {
    const rect = triggerRef.current?.getBoundingClientRect()
    if (!rect) return
    const below = window.innerHeight - rect.bottom - GAP
    const above = rect.top - GAP
    // Xổ lên khi dưới không đủ một danh sách đầy mà trên lại rộng hơn.
    const up = below < MAX_LIST_HEIGHT && above > below
    setPosition({
      left: rect.left,
      width: rect.width,
      maxHeight: Math.max(MIN_LIST_HEIGHT, Math.min(MAX_LIST_HEIGHT, up ? above : below)),
      top: up ? undefined : rect.bottom + GAP,
      bottom: up ? window.innerHeight - rect.top + GAP : undefined,
    })
  }, [])

  /* Lớp trên cùng là cách duy nhất để danh sách không bị khung cha có
     `overflow: auto` cắt — ví dụ thân hộp thoại. Danh sách vẫn là con của hộp
     thoại trong DOM, nên hộp thoại dạng modal không làm nó bị `inert`. */
  useLayoutEffect(() => {
    if (!open) return
    place()
    const list = listRef.current
    togglePopover(list, true)
    // `true`: bắt cả cuộn của khung cha (thân hộp thoại), không riêng cuộn trang.
    window.addEventListener('scroll', place, true)
    window.addEventListener('resize', place)
    return () => {
      window.removeEventListener('scroll', place, true)
      window.removeEventListener('resize', place)
      togglePopover(list, false)
    }
  }, [open, place])

  /* Mục đang trỏ luôn nằm trong vùng nhìn thấy. Tự cuộn danh sách thay vì
     `scrollIntoView` — hàm đó cuộn luôn cả khung cha, làm nút xê dịch. */
  useEffect(() => {
    if (!open) return
    const list = listRef.current
    const item = list?.children[activeIndex] as HTMLElement | undefined
    if (!list || !item) return
    const top = item.offsetTop
    const bottom = top + item.offsetHeight
    if (top < list.scrollTop) list.scrollTop = top
    else if (bottom > list.scrollTop + list.clientHeight) list.scrollTop = bottom - list.clientHeight
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
      /* Chặn mặc định: Esc ở đây chỉ đóng danh sách, không đóng luôn hộp thoại
         đang chứa nó. */
      event.preventDefault()
      action()
    } else if (event.key === 'Tab') {
      setOpen(false)
    }
  }

  return (
    <div ref={rootRef} className={[styles.root, className].filter(Boolean).join(' ')}>
      <button
        ref={triggerRef}
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
        <ul
          ref={listRef}
          id={listId}
          role="listbox"
          aria-label={ariaLabel}
          className={styles.list}
          /* `manual`: tự đóng bằng Esc và bấm ra ngoài như trên, không để
             trình duyệt đóng hộ nửa vời. */
          popover="manual"
          /* Giữ focus ở nút: nhấn chuột trong danh sách không được cướp focus. */
          onMouseDown={(event) => event.preventDefault()}
          /* Select hay được đặt trong `<label>`. Không chặn hành vi mặc định
             thì trình duyệt chuyển tiếp cú click này sang chính nút combobox —
             danh sách vừa đóng vì đã chọn xong lại bật mở ngay. */
          onClick={(event) => event.preventDefault()}
          style={{
            left: position?.left,
            width: position?.width,
            maxHeight: position?.maxHeight,
            top: position?.top,
            bottom: position?.bottom,
            // Chưa đo xong thì chưa vẽ, tránh nháy một nhịp ở góc màn hình.
            visibility: position ? undefined : 'hidden',
          }}
        >
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
