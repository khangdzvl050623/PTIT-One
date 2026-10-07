import { Dialog } from '@/shared/ui'

import type { ScheduleSlot } from '../types'
import styles from './ScheduleDialog.module.scss'

export interface ScheduleDialogClass {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  tenGiangVien?: string | null
  lich: readonly ScheduleSlot[]
}

export interface ScheduleDialogProps {
  /** `null` = đóng. */
  lop: ScheduleDialogClass | null
  /** `HocKy.NgayBatDau` (ISO) — để đổi tuần ra ngày. */
  ngayBatDau: string
  onClose: () => void
}

/** Popup thời khoá biểu một lớp: mỗi buổi trong tuần một dòng, kèm khoảng ngày. */
export function ScheduleDialog({ lop, ngayBatDau, onClose }: ScheduleDialogProps) {
  return (
    <Dialog
      open={lop !== null}
      onClose={onClose}
      ariaLabel={lop ? `Thời khoá biểu ${lop.tenMonHoc}` : 'Thời khoá biểu'}
      header={
        lop ? (
          <dl className={styles.info}>
            <dt>Mã môn học:</dt>
            <dd>{lop.maMonHoc}</dd>
            <dt>Tên môn học:</dt>
            <dd>{lop.tenMonHoc}</dd>
            <dt>Lớp:</dt>
            <dd>
              {lop.maLopHP} · {lop.soTinChi} tín chỉ
            </dd>
            <dt>Giảng viên:</dt>
            <dd>{lop.tenGiangVien ?? 'Chưa phân công'}</dd>
          </dl>
        ) : null
      }
    >
      <h3 className={styles.title}>Thời khoá biểu</h3>
      {lop && lop.lich.length > 0 ? (
        <ul className={styles.slots}>
          {lop.lich.map((s) => (
            <li key={`${s.thu}-${s.tietBatDau}-${s.tuanBatDau}`}>
              <b>{s.thu === 8 ? 'Chủ nhật' : `Thứ ${s.thu}`}</b>, tiết {s.tietBatDau}→
              {s.tietBatDau + s.soTiet - 1}
              {s.phongHoc ? <>, phòng {s.phongHoc}</> : null},{' '}
              <span className={styles.dates}>
                {shortDate(dayOf(ngayBatDau, s.tuanBatDau, s.thu))} đến{' '}
                {shortDate(dayOf(ngayBatDau, s.tuanKetThuc, s.thu))}
              </span>
            </li>
          ))}
        </ul>
      ) : (
        <p className={styles.none}>Lớp chưa được xếp lịch.</p>
      )}
    </Dialog>
  )
}

/** Ngày của `thu` (2 = thứ Hai … 8 = CN) trong tuần `tuan` — quy ước của API. */
function dayOf(ngayBatDau: string, tuan: number, thu: number): Date {
  const [y, m, d] = ngayBatDau.split('-').map(Number)
  return new Date(y ?? 1970, (m ?? 1) - 1, (d ?? 1) + 7 * (tuan - 1) + (thu - 2))
}

/** `13/08/26` như cổng gốc. */
function shortDate(date: Date): string {
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: '2-digit' })
}
