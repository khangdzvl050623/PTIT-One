import { useState } from 'react'

import { CAMPUSES, FACULTIES, PROGRAMS } from '../data/demo'
import type { ProvisionResult } from '../types'
import { StudentForm, TeacherForm } from './ProvisionForms'
import { ProvisionResultCard } from './ProvisionResultCard'
import styles from './AccountDirectory.module.scss'

type Loai = 'SINH_VIEN' | 'GIANG_VIEN'

export interface ProvisionPanelProps {
  /** Báo cho danh bạ tải lại sau khi cấp xong. */
  onCreated: () => void
}

/**
 * Cột cấp hồ sơ: chọn sinh viên hay giảng viên rồi nhập thẳng, không qua popup.
 *
 * Hai form giữ state riêng, nên chuyển qua lại không mất những gì đang gõ dở.
 * Kết quả (mã kích hoạt hoặc mật khẩu đặt sẵn) vẫn hiện bằng hộp thoại vì đó
 * là thứ Admin phải đọc và lưu ngay, không nên để lẫn vào form.
 */
export function ProvisionPanel({ onCreated }: ProvisionPanelProps) {
  const [loai, setLoai] = useState<Loai>('SINH_VIEN')
  const [result, setResult] = useState<ProvisionResult | null>(null)

  function done(next: ProvisionResult) {
    setResult(next)
    onCreated()
  }

  return (
    <div className={styles.provision}>
      <div className={styles.segmented} role="tablist" aria-label="Loại hồ sơ cần cấp">
        {/* Khối đỏ trượt giữa hai tab. Tách khỏi nút để chỉ một phần tử chạy
            `transform` — đổi nền của hai nút thì không trượt được. */}
        <span className={styles.segThumb} data-loai={loai} aria-hidden="true" />
        <button
          type="button"
          role="tab"
          aria-selected={loai === 'SINH_VIEN'}
          className={styles.seg}
          onClick={() => setLoai('SINH_VIEN')}
        >
          Sinh viên
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={loai === 'GIANG_VIEN'}
          className={styles.seg}
          onClick={() => setLoai('GIANG_VIEN')}
        >
          Giảng viên
        </button>
      </div>

      {/* Cả hai cùng gắn trên cây, chỉ ẩn đi — chuyển tab không xoá dữ liệu
          đang gõ. `hidden` là `display: none`, nên mỗi lần hiện lại trình duyệt
          chạy lại hiệu ứng `tabIn` của form vừa mở. */}
      <div className={styles.tabPanel} hidden={loai !== 'SINH_VIEN'}>
        <StudentForm campuses={CAMPUSES} programs={PROGRAMS} onDone={done} />
      </div>
      <div className={styles.tabPanel} hidden={loai !== 'GIANG_VIEN'}>
        <TeacherForm campuses={CAMPUSES} faculties={FACULTIES} onDone={done} />
      </div>

      {result ? (
        <ProvisionResultCard
          result={result}
          title={
            result.loai === 'SINH_VIEN'
              ? `Đã cấp hồ sơ sinh viên ${result.ma}`
              : `Đã cấp hồ sơ giảng viên ${result.ma}`
          }
          onClose={() => setResult(null)}
        />
      ) : null}
    </div>
  )
}
