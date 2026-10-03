import { useEffect, useMemo, useState } from 'react'

import { Icon, Select } from '@/shared/ui'

import { useEnrollment } from '../hooks/useEnrollment'
import { isOpen, vnDateTime } from '../lib/period'
import { NEEDS_PARAM, inScope, paramOptions, scopeOptions } from '../lib/scope'
import type { Scope } from '../lib/scope'
import type { ClassOffer, EnrolledCourse, EnrollmentPeriod, ScheduleSlot } from '../types'
import styles from './EnrollmentBoard.module.scss'
import { ScheduleDialog } from './ScheduleDialog'
import type { ScheduleDialogClass } from './ScheduleDialog'

type FilterKey = 'maMonHoc' | 'tenMonHoc' | 'maLopHP' | 'tenGiangVien'

const FILTERS: readonly { key: FilterKey; label: string }[] = [
  { key: 'maMonHoc', label: 'Mã MH' },
  { key: 'tenMonHoc', label: 'Tên môn học' },
  { key: 'maLopHP', label: 'Lớp' },
  { key: 'tenGiangVien', label: 'Giảng viên' },
]

const HINH_THUC: Record<string, string> = {
  TRUC_TIEP: 'Trực tiếp',
  TRUC_TUYEN: 'Trực tuyến',
  KET_HOP: 'Kết hợp',
}

/** Thông báo thành công tự ẩn sau chừng này; lỗi giữ tới khi người dùng đóng. */
const SUCCESS_MS = 6000

export interface EnrollmentBoardProps {
  maHocKy: string
  tenHocKy: string
  /** `HocKy.NgayBatDau` (ISO) — popup thời khoá biểu đổi tuần ra ngày. */
  ngayBatDau: string
  /** Dòng định danh trên phiếu in, ví dụ "B26DCCN001 · Nguyễn Văn An". */
  studentLabel: string
  /** Hiện nút đặt lại dữ liệu — chỉ khi đang chạy bản giả. */
  demo?: boolean
}

/** Đăng ký học phần (F08): lớp mở · môn đã đăng ký · phiếu đăng ký. */
export function EnrollmentBoard({
  maHocKy,
  tenHocKy,
  ngayBatDau,
  studentLabel,
  demo = false,
}: EnrollmentBoardProps) {
  const {
    classes,
    mine,
    period,
    program,
    catalog,
    results,
    loading,
    pending,
    notice,
    dismiss,
    register,
    cancel,
    resetDemo,
  } = useEnrollment(maHocKy)
  const [scope, setScope] = useState<Scope>('KE_HOACH')
  const [scopeParam, setScopeParam] = useState('')
  const [filters, setFilters] = useState<Record<FilterKey, string>>({
    maMonHoc: '',
    tenMonHoc: '',
    maLopHP: '',
    tenGiangVien: '',
  })
  const [onlyFree, setOnlyFree] = useState(false)
  const [desc, setDesc] = useState(false)
  const [confirming, setConfirming] = useState<string | null>(null)
  const [detail, setDetail] = useState<ScheduleDialogClass | null>(null)

  useEffect(() => {
    if (notice?.tone !== 'success') return
    const timer = window.setTimeout(dismiss, SUCCESS_MS)
    return () => window.clearTimeout(timer)
  }, [notice, dismiss])

  const open = period ? isOpen(period) : false
  const enrolled = useMemo(() => mine?.dangKy ?? [], [mine])
  const byCourse = useMemo(() => new Map(enrolled.map((e) => [e.maMonHoc, e])), [enrolled])
  const offers = useMemo(() => new Map(classes.map((c) => [c.maLopHP, c])), [classes])

  const catalogMap = useMemo(() => new Map(catalog.map((c) => [c.maMonHoc, c])), [catalog])

  const visible = useMemo(() => {
    const ctx = { program, catalog: catalogMap, results }
    const rows = classes.filter(
      (c) =>
        inScope(scope, scopeParam, c, ctx) &&
        FILTERS.every(({ key }) => matches(c[key] ?? '', filters[key])) &&
        (!onlyFree || c.soLuongDaDangKy < c.soLuongToiDa),
    )
    const sign = desc ? -1 : 1
    return rows.sort(
      (a, b) => sign * (a.maMonHoc.localeCompare(b.maMonHoc) || a.maLopHP.localeCompare(b.maLopHP)),
    )
  }, [classes, filters, onlyFree, desc, scope, scopeParam, program, catalogMap, results])

  const daDangKy = mine?.soTinChiDaDangKy ?? 0
  const tran = mine?.tranTinChi ?? null

  if (loading) return <p className={styles.muted}>Đang tải danh sách lớp…</p>

  function filterCell(key: FilterKey) {
    const label = FILTERS.find((f) => f.key === key)?.label ?? key
    return (
      <th scope="col">
        <input
          className={styles.filter}
          value={filters[key]}
          placeholder="…"
          aria-label={`Lọc theo ${label}`}
          onChange={(e) => setFilters((f) => ({ ...f, [key]: e.target.value }))}
        />
      </th>
    )
  }

  return (
    <div className={styles.board}>
      <p className={styles.printTitle}>
        PHIẾU ĐĂNG KÝ HỌC PHẦN · {tenHocKy}
        <small>
          {studentLabel} · in lúc {new Date().toLocaleString('vi-VN')}
        </small>
      </p>

      <div className={styles.status} data-print="hide">
        <PeriodBanner period={period} open={open} />
        <div className={styles.meter}>
          <span>
            Đã đăng ký <b>{daDangKy}</b>
            {tran !== null ? <> / {tran}</> : null} tín chỉ
          </span>
          {tran !== null ? (
            <span
              className={styles.track}
              role="progressbar"
              aria-label="Tín chỉ đã đăng ký"
              aria-valuemin={0}
              aria-valuemax={tran}
              aria-valuenow={daDangKy}
            >
              <span style={{ width: `${Math.min(100, (daDangKy / tran) * 100)}%` }} />
            </span>
          ) : null}
        </div>
      </div>

      {notice ? (
        <div
          className={`${styles.notice} ${styles[notice.tone]}`}
          role={notice.tone === 'error' ? 'alert' : 'status'}
          data-print="hide"
        >
          <span>{notice.text}</span>
          <button type="button" onClick={dismiss} aria-label="Đóng thông báo">
            ×
          </button>
        </div>
      ) : null}

      {/* --- Phạm vi danh sách lớp mở ------------------------------------ */}
      <div className={styles.scopeBar} data-print="hide">
        <Select
          ariaLabel="Phạm vi môn học mở"
          className={styles.scopeSelect}
          value={scope}
          options={scopeOptions(program)}
          onChange={(next) => {
            setScope(next)
            setScopeParam('')
          }}
        />
        {NEEDS_PARAM.has(scope) ? (
          <Select
            ariaLabel={scope === 'KHOA' ? 'Khoa quản lý môn học' : 'Môn học'}
            className={styles.scopeSelect}
            value={scopeParam}
            options={paramOptions(scope, classes, catalogMap)}
            onChange={setScopeParam}
          />
        ) : null}
        <span className={styles.scopeCount} aria-live="polite">
          {visible.length} lớp
        </span>
      </div>

      {/* --- Lớp mở cho đăng ký ------------------------------------------ */}
      <section className={styles.card} data-print="hide">
        <header className={styles.cardHead}>
          <h3>Danh sách môn học mở cho đăng ký</h3>
          <label className={styles.check}>
            <input
              type="checkbox"
              checked={onlyFree}
              onChange={(e) => setOnlyFree(e.target.checked)}
            />
            Chỉ lớp còn chỗ
          </label>
        </header>

        <div className={styles.scroll}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col" className={styles.action}>
                  Đăng ký
                </th>
                <th scope="col" aria-sort={desc ? 'descending' : 'ascending'}>
                  <button type="button" className={styles.sortBtn} onClick={() => setDesc((d) => !d)}>
                    Mã MH {desc ? '▼' : '▲'}
                  </button>
                </th>
                <th scope="col" className={styles.left}>
                  Tên môn học
                </th>
                <th scope="col">Lớp</th>
                <th scope="col">Số TC</th>
                <th scope="col" className={styles.left}>
                  Giảng viên
                </th>
                <th scope="col">Sĩ số</th>
                <th scope="col">Còn lại</th>
                <th scope="col" className={styles.left}>
                  Thời khoá biểu
                </th>
              </tr>
              <tr className={styles.filterRow}>
                <th scope="col">
                  <Icon name="search" size="14px" className={styles.searchIcon} />
                </th>
                {filterCell('maMonHoc')}
                {filterCell('tenMonHoc')}
                {filterCell('maLopHP')}
                <th aria-hidden="true" />
                {filterCell('tenGiangVien')}
                <th colSpan={3} aria-hidden="true" />
              </tr>
            </thead>
            <tbody>
              {visible.length === 0 ? (
                <tr>
                  <td colSpan={9} className={styles.empty}>
                    Không tìm thấy dữ liệu
                  </td>
                </tr>
              ) : (
                visible.map((c) => (
                  <OfferRow
                    key={c.maLopHP}
                    lop={c}
                    held={byCourse.get(c.maMonHoc)}
                    busy={pending !== null}
                    waiting={pending === c.maLopHP}
                    open={open}
                    onRegister={() => void register(c)}
                    onShowSchedule={() => setDetail(c)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* --- Môn đã đăng ký ---------------------------------------------- */}
      <section className={styles.card}>
        <header className={styles.cardHead}>
          <h3>
            Danh sách môn học đã đăng ký:{' '}
            <span className={styles.total}>
              {enrolled.length} môn, {daDangKy} tín chỉ
            </span>
          </h3>
        </header>

        <div className={styles.scroll}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col" className={styles.action} data-print="hide">
                  Xoá
                </th>
                <th scope="col">Mã MH</th>
                <th scope="col" className={styles.left}>
                  Tên môn học
                </th>
                <th scope="col">Lớp</th>
                <th scope="col">Số TC</th>
                <th scope="col">Ngày đăng ký</th>
                <th scope="col">Trạng thái</th>
                <th scope="col" className={styles.left}>
                  Thời khoá biểu
                </th>
              </tr>
            </thead>
            <tbody>
              {enrolled.length === 0 ? (
                <tr>
                  <td colSpan={8} className={styles.empty}>
                    Chưa đăng ký môn nào trong học kỳ này.
                  </td>
                </tr>
              ) : (
                enrolled.map((e) =>
                  confirming === e.maLopHP ? (
                    <ConfirmRow
                      key={e.maLopHP}
                      course={e}
                      waiting={pending === e.maLopHP}
                      onConfirm={() => {
                        void cancel(e.maLopHP, e.tenMonHoc).then(() => setConfirming(null))
                      }}
                      onKeep={() => setConfirming(null)}
                    />
                  ) : (
                    <EnrolledRow
                      key={e.maLopHP}
                      course={e}
                      lich={offers.get(e.maLopHP)?.lich ?? []}
                      canCancel={open && pending === null}
                      onCancel={() => setConfirming(e.maLopHP)}
                      onShowSchedule={() =>
                        setDetail({
                          ...e,
                          tenGiangVien: offers.get(e.maLopHP)?.tenGiangVien,
                          lich: offers.get(e.maLopHP)?.lich ?? [],
                        })
                      }
                    />
                  ),
                )
              )}
            </tbody>
          </table>
        </div>

        <footer className={styles.cardFoot} data-print="hide">
          {demo ? (
            <button type="button" className={styles.linkBtn} onClick={() => void resetDemo()}>
              Đặt lại dữ liệu demo
            </button>
          ) : (
            <span />
          )}
          <button
            type="button"
            className={styles.tool}
            onClick={() => window.print()}
            disabled={enrolled.length === 0}
          >
            <Icon name="printer" size="15px" />
            Xuất phiếu đăng ký
          </button>
        </footer>
      </section>

      <ScheduleDialog lop={detail} ngayBatDau={ngayBatDau} onClose={() => setDetail(null)} />
    </div>
  )
}

function PeriodBanner({ period, open }: { period: EnrollmentPeriod | null; open: boolean }) {
  if (!period) {
    return <p className={`${styles.period} ${styles.closed}`}>Chưa có đợt đăng ký cho học kỳ này.</p>
  }
  const range = `${vnDateTime(period.thoiGianMo)} → ${vnDateTime(period.thoiGianDong)}`
  return open ? (
    <p className={`${styles.period} ${styles.opened}`}>
      <Icon name="calendar" size="14px" /> Đợt đăng ký đang mở: <b>{range}</b>
    </p>
  ) : (
    <p className={`${styles.period} ${styles.closed}`}>
      <Icon name="lock" size="14px" /> Ngoài thời gian đăng ký ({range}) — chỉ xem, không đăng ký hay huỷ được.
    </p>
  )
}

interface OfferRowProps {
  lop: ClassOffer
  /** Lớp đang giữ của cùng môn, nếu có. */
  held: EnrolledCourse | undefined
  busy: boolean
  waiting: boolean
  open: boolean
  onRegister: () => void
  onShowSchedule: () => void
}

function OfferRow({ lop, held, busy, waiting, open, onRegister, onShowSchedule }: OfferRowProps) {
  const mineHere = held?.maLopHP === lop.maLopHP
  const conLai = lop.soLuongToiDa - lop.soLuongDaDangKy
  return (
    <tr className={mineHere ? styles.rowMine : undefined}>
      <td className={styles.center}>
        {mineHere ? (
          <span className={styles.registered}>
            <Icon name="check" size="12px" /> Đã ĐK
          </span>
        ) : (
          /* Lớp đầy/trùng môn vẫn bấm được: danh sách có thể cũ, server mới quyết. */
          <button
            type="button"
            className={styles.registerBtn}
            onClick={onRegister}
            disabled={!open || busy}
            aria-label={`Đăng ký lớp ${lop.maLopHP}`}
          >
            {waiting ? 'Đang ĐK…' : 'Đăng ký'}
          </button>
        )}
      </td>
      <td className={`${styles.center} ${styles.code}`}>{lop.maMonHoc}</td>
      <td>
        {lop.tenMonHoc}
        {lop.hinhThucHoc !== 'TRUC_TIEP' ? (
          <span className={styles.mode}>{HINH_THUC[lop.hinhThucHoc] ?? lop.hinhThucHoc}</span>
        ) : null}
        {held && !mineHere ? (
          <span className={styles.hint}>Đang giữ lớp {nhom(held.maLopHP)} của môn này</span>
        ) : null}
      </td>
      <td className={styles.center} title={lop.maLopHP}>
        {nhom(lop.maLopHP)}
      </td>
      <td className={styles.center}>{lop.soTinChi}</td>
      <td>{lop.tenGiangVien ?? 'Chưa phân công'}</td>
      <td className={styles.center}>
        {lop.soLuongDaDangKy}/{lop.soLuongToiDa}
      </td>
      <td className={styles.center}>
        {conLai > 0 ? (
          <span className={conLai <= 5 ? styles.few : styles.free}>{conLai}</span>
        ) : (
          <span className={styles.full}>Hết chỗ</span>
        )}
      </td>
      <td className={styles.schedule}>
        <ScheduleCell lich={lop.lich} tenMonHoc={lop.tenMonHoc} onShow={onShowSchedule} />
      </td>
    </tr>
  )
}

/** Tóm tắt lịch + nút ☰ mở popup chi tiết (có khoảng ngày học). */
function ScheduleCell(props: {
  lich: readonly ScheduleSlot[]
  tenMonHoc: string
  onShow: () => void
}) {
  return (
    <span className={styles.scheduleCell}>
      <span>{formatLich(props.lich)}</span>
      <button
        type="button"
        className={styles.listBtn}
        onClick={props.onShow}
        aria-label={`Xem thời khoá biểu ${props.tenMonHoc}`}
        aria-haspopup="dialog"
        data-print="hide"
      >
        <Icon name="list" size="18px" />
      </button>
    </span>
  )
}

interface EnrolledRowProps {
  course: EnrolledCourse
  lich: readonly ScheduleSlot[]
  canCancel: boolean
  onCancel: () => void
  onShowSchedule: () => void
}

function EnrolledRow({ course, lich, canCancel, onCancel, onShowSchedule }: EnrolledRowProps) {
  return (
    <tr>
      <td className={styles.center} data-print="hide">
        <button
          type="button"
          className={styles.removeBtn}
          onClick={onCancel}
          disabled={!canCancel}
          aria-label={`Huỷ đăng ký ${course.tenMonHoc}`}
        >
          ×
        </button>
      </td>
      <td className={`${styles.center} ${styles.code}`}>{course.maMonHoc}</td>
      <td>{course.tenMonHoc}</td>
      <td className={styles.center} title={course.maLopHP}>
        {nhom(course.maLopHP)}
      </td>
      <td className={styles.center}>{course.soTinChi}</td>
      <td className={styles.center}>{course.ngayDangKy ? vnDateTime(course.ngayDangKy, true) : '—'}</td>
      <td className={styles.center}>
        <span className={styles.statusBadge}>{TRANG_THAI[course.trangThai] ?? course.trangThai}</span>
      </td>
      <td className={styles.schedule}>
        <ScheduleCell lich={lich} tenMonHoc={course.tenMonHoc} onShow={onShowSchedule} />
      </td>
    </tr>
  )
}

interface ConfirmRowProps {
  course: EnrolledCourse
  waiting: boolean
  onConfirm: () => void
  onKeep: () => void
}

/** Xác nhận ngay trong dòng — huỷ là trả chỗ, người khác có thể lấy mất. */
function ConfirmRow({ course, waiting, onConfirm, onKeep }: ConfirmRowProps) {
  return (
    <tr className={styles.confirmRow}>
      <td colSpan={8}>
        <span>
          Huỷ đăng ký <b>{course.tenMonHoc}</b> ({course.maLopHP})? Chỗ trong lớp sẽ được trả lại
          và người khác có thể đăng ký mất.
        </span>
        <span className={styles.confirmActions}>
          <button type="button" className={styles.dangerBtn} onClick={onConfirm} disabled={waiting}>
            {waiting ? 'Đang huỷ…' : 'Huỷ đăng ký'}
          </button>
          <button type="button" className={styles.linkBtn} onClick={onKeep} disabled={waiting}>
            Giữ lại
          </button>
        </span>
      </td>
    </tr>
  )
}

const TRANG_THAI: Record<string, string> = {
  DA_DANG_KY: 'Đã đăng ký',
  DANG_XU_LY: 'Đang xử lý',
  DANG_HUY: 'Đang huỷ',
}

/** Không phân biệt hoa thường và dấu: "giai tich" khớp "Giải tích". */
function matches(value: string, query: string): boolean {
  const fold = (s: string) =>
    s.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
  return fold(value).includes(fold(query.trim()))
}

/** `INT1339-2026-1-HCM01` → `HCM01`; mã đầy đủ ở tooltip. */
function nhom(maLopHP: string): string {
  return maLopHP.slice(maLopHP.lastIndexOf('-') + 1)
}

/** `T2 (1–4) · T6 (6–9)`; Chủ nhật là `CN`. */
function formatLich(lich: readonly ScheduleSlot[]): string {
  if (lich.length === 0) return 'Chưa xếp lịch'
  return lich
    .map((s) => `${s.thu === 8 ? 'CN' : `T${s.thu}`} (${s.tietBatDau}–${s.tietBatDau + s.soTiet - 1})`)
    .join(' · ')
}
