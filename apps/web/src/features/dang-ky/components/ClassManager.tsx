import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { ApiError } from '@/shared/api'
import { Dialog, Icon, Select } from '@/shared/ui'

import { useTerms } from '@/features/lich-hoc'

import * as api from '../api/enrollmentApi'
import type { ClassOffer, SlotInput, TeacherOption } from '../types'
import styles from './Admin.module.scss'

const MODES = [
  { value: 'TRUC_TIEP', label: 'Trực tiếp' },
  { value: 'TRUC_TUYEN', label: 'Trực tuyến' },
  { value: 'KET_HOP', label: 'Kết hợp' },
]
const MODE_LABEL = Object.fromEntries(MODES.map((m) => [m.value, m.label]))

/** PUT chỉ đặt được hai trạng thái này — khoá điểm và huỷ lớp đi đường riêng. */
const EDITABLE_STATUS = [
  { value: 'DU_KIEN', label: 'Dự kiến (chưa mở đăng ký)' },
  { value: 'MO', label: 'Đang mở (sinh viên đăng ký được)' },
]
const STATUS_LABEL: Record<string, string> = {
  DU_KIEN: 'Dự kiến',
  MO: 'Đang mở',
  DA_KHOA: 'Đã khoá',
  DA_HUY: 'Đã huỷ',
}
const THU = [2, 3, 4, 5, 6, 7, 8].map((t) => ({ value: t, label: t === 8 ? 'Chủ nhật' : `Thứ ${t}` }))

type Modal =
  | { kind: 'create' }
  | { kind: 'edit'; lop: ClassOffer }
  | { kind: 'teacher'; lop: ClassOffer }
  | { kind: 'schedule'; lop: ClassOffer }

function fold(s: string): string {
  return s.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
}

function formatLich(lich: readonly SlotInput[]): string {
  if (lich.length === 0) return 'Chưa xếp lịch'
  return lich
    .map((s) => `${s.thu === 8 ? 'CN' : `T${s.thu}`} (${s.tietBatDau}–${s.tietBatDau + s.soTiet - 1})${s.phongHoc ? ` · ${s.phongHoc}` : ''}`)
    .join('; ')
}

/**
 * Quản trị lớp học phần (F04, ADMIN_CO_SO): mở lớp, sửa, phân công GV, xếp
 * lịch. Mọi ràng buộc (trùng GV/phòng, sức chứa dưới sĩ số, lịch lớp đã có
 * SV…) do server kiểm — form chỉ hiện đúng câu báo server trả về.
 */
export function ClassManager() {
  /* Danh sách học kỳ lấy từ API; trước đây viết cứng hai mã nên ở chế độ api
     ô chọn trỏ vào học kỳ không tồn tại và bảng lớp rỗng mà không báo gì. */
  const { terms, defaultTerm } = useTerms()
  const [chosenTerm, setChosenTerm] = useState('')
  const maHocKy = chosenTerm || defaultTerm
  const [classes, setClasses] = useState<ClassOffer[] | null>(null)
  const [teachers, setTeachers] = useState<TeacherOption[]>([])
  const [courses, setCourses] = useState<{ maMonHoc: string; tenMonHoc: string; soTinChi: number }[]>([])
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [modal, setModal] = useState<Modal | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const reload = useCallback(async () => setClasses(await api.adminClasses(maHocKy)), [maHocKy])

  useEffect(() => {
    void reload()
  }, [reload])

  useEffect(() => {
    void Promise.all([api.listTeachers(), api.openableCourses()]).then(([t, c]) => {
      setTeachers(t)
      setCourses(c)
    })
  }, [])

  const visible = useMemo(() => {
    const q = fold(query.trim())
    return (classes ?? []).filter(
      (c) =>
        (!status || c.trangThai === status) &&
        (!q || fold(`${c.maLopHP} ${c.tenMonHoc} ${c.tenGiangVien ?? ''}`).includes(q)),
    )
  }, [classes, query, status])

  async function done(message: string) {
    setModal(null)
    setNotice(message)
    await reload()
  }

  const counts = (classes ?? []).reduce<Record<string, number>>((m, c) => {
    m[c.trangThai] = (m[c.trangThai] ?? 0) + 1
    return m
  }, {})

  return (
    <div className={styles.block}>
      <div className={styles.toolbar}>
        <Select
          ariaLabel="Học kỳ"
          className={styles.termSelect}
          value={maHocKy}
          options={terms.map((t) => ({ value: t.maHocKy, label: t.tenHocKy }))}
          onChange={(v) => {
            setChosenTerm(v)
            setClasses(null)
          }}
        />
        <Select
          ariaLabel="Lọc trạng thái"
          className={styles.statusSelect}
          value={status}
          options={[
            { value: '', label: `Mọi trạng thái (${classes?.length ?? 0})` },
            ...Object.entries(STATUS_LABEL).map(([value, label]) => ({
              value,
              label: `${label} (${counts[value] ?? 0})`,
            })),
          ]}
          onChange={setStatus}
        />
        <label className={styles.search}>
          <Icon name="search" size="14px" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo mã lớp, môn hoặc giảng viên…"
            aria-label="Tìm lớp"
          />
        </label>
        <button type="button" className={styles.primary} onClick={() => setModal({ kind: 'create' })}>
          + Mở lớp mới
        </button>
      </div>

      {notice ? (
        <p className={`${styles.notice} ${styles.success}`} role="status">
          {notice}
          <button type="button" onClick={() => setNotice(null)} aria-label="Đóng thông báo">
            ×
          </button>
        </p>
      ) : null}

      {!classes ? (
        <p className={styles.muted}>Đang tải danh sách lớp…</p>
      ) : (
        <div className={styles.scroll}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">Mã lớp</th>
                <th scope="col" className={styles.left}>
                  Môn học
                </th>
                <th scope="col" className={styles.left}>
                  Giảng viên
                </th>
                <th scope="col" className={styles.left}>
                  Lịch học
                </th>
                <th scope="col">Hình thức</th>
                <th scope="col">Sĩ số</th>
                <th scope="col">Trạng thái</th>
                <th scope="col">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {visible.length === 0 ? (
                <tr>
                  <td colSpan={8} className={styles.empty}>
                    Không tìm thấy dữ liệu
                  </td>
                </tr>
              ) : (
                visible.map((c) => {
                  const locked = c.trangThai === 'DA_HUY' || c.trangThai === 'DA_KHOA'
                  return (
                    <tr key={c.maLopHP} className={c.trangThai === 'DA_HUY' ? styles.rowCancelled : undefined}>
                      <td className={`${styles.center} ${styles.code}`}>{c.maLopHP}</td>
                      <td>
                        {c.tenMonHoc}
                        <small className={styles.sub}>
                          {c.maMonHoc} · {c.soTinChi} TC
                        </small>
                      </td>
                      <td>
                        {c.tenGiangVien ?? <span className={styles.missing}>Chưa phân công</span>}
                      </td>
                      <td className={styles.lich}>
                        {c.lich.length ? formatLich(c.lich) : <span className={styles.missing}>Chưa xếp lịch</span>}
                      </td>
                      <td className={styles.center}>
                        {MODE_LABEL[c.hinhThucHoc] ?? c.hinhThucHoc}
                        {c.choPhepLienCoSo ? <small className={styles.sub}>Liên cơ sở</small> : null}
                      </td>
                      <td className={styles.center}>
                        <span className={styles.seats}>
                          <span className={styles.meter}>
                            <span style={{ width: `${(c.soLuongDaDangKy / c.soLuongToiDa) * 100}%` }} />
                          </span>
                          {c.soLuongDaDangKy}/{c.soLuongToiDa}
                        </span>
                      </td>
                      <td className={styles.center}>
                        <span className={`${styles.badge} ${styles[`lop_${c.trangThai}`]}`}>
                          {STATUS_LABEL[c.trangThai] ?? c.trangThai}
                        </span>
                      </td>
                      <td className={styles.actions}>
                        <button type="button" className={styles.ghost} disabled={locked} onClick={() => setModal({ kind: 'edit', lop: c })}>
                          Sửa
                        </button>
                        <button type="button" className={styles.ghost} disabled={locked} onClick={() => setModal({ kind: 'teacher', lop: c })}>
                          Phân công GV
                        </button>
                        <button type="button" className={styles.ghost} disabled={locked} onClick={() => setModal({ kind: 'schedule', lop: c })}>
                          Xếp lịch
                        </button>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      {modal?.kind === 'create' ? (
        <CreateDialog
          maHocKy={maHocKy}
          terms={terms}
          courses={courses}
          teachers={teachers}
          onClose={() => setModal(null)}
          onDone={(lop) =>
            void done(`Đã mở lớp ${lop.maLopHP} ở trạng thái Dự kiến — xếp lịch rồi chuyển sang Đang mở để sinh viên đăng ký.`)
          }
        />
      ) : null}
      {modal?.kind === 'edit' ? (
        <EditDialog lop={modal.lop} onClose={() => setModal(null)} onDone={(lop) => void done(`Đã cập nhật lớp ${lop.maLopHP}.`)} />
      ) : null}
      {modal?.kind === 'teacher' ? (
        <TeacherDialog
          lop={modal.lop}
          teachers={teachers}
          onClose={() => setModal(null)}
          onDone={(lop) =>
            void done(lop.tenGiangVien ? `Đã phân công ${lop.tenGiangVien} dạy lớp ${lop.maLopHP}.` : `Đã gỡ phân công lớp ${lop.maLopHP}.`)
          }
        />
      ) : null}
      {modal?.kind === 'schedule' ? (
        <ScheduleDialog lop={modal.lop} onClose={() => setModal(null)} onDone={(lop) => void done(`Đã lưu lịch lớp ${lop.maLopHP}.`)} />
      ) : null}
    </div>
  )
}

/* --- Popup dùng chung ------------------------------------------------------ */

interface FormDialogProps {
  title: string
  subtitle?: string
  busy: boolean
  error: string | null
  submitLabel: string
  submitDisabled?: boolean
  onClose: () => void
  onSubmit: () => void
  children: ReactNode
}

function FormDialog(props: FormDialogProps) {
  return (
    <Dialog
      open
      onClose={props.onClose}
      ariaLabel={props.title}
      header={
        <p className={styles.dialogTitle}>
          {props.title}
          {props.subtitle ? <small>{props.subtitle}</small> : null}
        </p>
      }
      footer={
        <>
          <button type="button" className={styles.ghost} onClick={props.onClose} disabled={props.busy}>
            Huỷ
          </button>
          <button
            type="button"
            className={styles.primary}
            onClick={props.onSubmit}
            disabled={props.busy || props.submitDisabled}
          >
            {props.busy ? 'Đang lưu…' : props.submitLabel}
          </button>
        </>
      }
    >
      <div className={styles.form}>
        {props.children}
        {props.error ? (
          <p className={`${styles.notice} ${styles.error}`} role="alert">
            {props.error}
          </p>
        ) : null}
      </div>
    </Dialog>
  )
}

/** Gọi API, gom lỗi server thành câu hiển thị trong popup. */
function useSubmit<T>(onDone: (value: T) => void) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  async function run(action: () => Promise<T>) {
    setBusy(true)
    setError(null)
    try {
      onDone(await action())
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không lưu được. Vui lòng thử lại.')
    } finally {
      setBusy(false)
    }
  }
  return { busy, error, run }
}

function CrossCampusField(props: { mode: string; value: boolean; onChange: (v: boolean) => void }) {
  const online = props.mode === 'TRUC_TUYEN'
  return (
    <label className={styles.checkRow}>
      <input
        type="checkbox"
        checked={props.value && online}
        disabled={!online}
        onChange={(e) => props.onChange(e.target.checked)}
      />
      <span>
        Cho phép sinh viên cơ sở khác đăng ký
        <small className={styles.sub}>Chỉ lớp trực tuyến (D18) — lớp trực tiếp không kiểm trùng tiết qua 1.700 km được.</small>
      </span>
    </label>
  )
}

/* --- Mở lớp ------------------------------------------------------------------ */

function CreateDialog(props: {
  maHocKy: string
  terms: readonly { maHocKy: string; tenHocKy: string }[]
  courses: { maMonHoc: string; tenMonHoc: string; soTinChi: number }[]
  teachers: TeacherOption[]
  onClose: () => void
  onDone: (lop: ClassOffer) => void
}) {
  const [maMonHoc, setMaMonHoc] = useState(props.courses[0]?.maMonHoc ?? '')
  const [maHocKy, setMaHocKy] = useState(props.maHocKy)
  const [soLuong, setSoLuong] = useState(60)
  const [mode, setMode] = useState('TRUC_TIEP')
  const [cross, setCross] = useState(false)
  const [gv, setGv] = useState('')
  const { busy, error, run } = useSubmit(props.onDone)

  return (
    <FormDialog
      title="Mở lớp học phần mới"
      subtitle="Mã lớp do hệ thống sinh từ môn + học kỳ + cơ sở · lớp mới ở trạng thái Dự kiến"
      busy={busy}
      error={error}
      submitLabel="Mở lớp"
      submitDisabled={!maMonHoc}
      onClose={props.onClose}
      onSubmit={() =>
        void run(() =>
          api.createClass({
            maMonHoc,
            maHocKy,
            soLuongToiDa: soLuong,
            hinhThucHoc: mode,
            choPhepLienCoSo: cross && mode === 'TRUC_TUYEN',
            maGiangVien: gv || null,
          }),
        )
      }
    >
      <label>
        <span>Môn học</span>
        <Select
          ariaLabel="Môn học"
          value={maMonHoc}
          options={props.courses.map((c) => ({ value: c.maMonHoc, label: `${c.maMonHoc} — ${c.tenMonHoc} (${c.soTinChi} TC)` }))}
          onChange={setMaMonHoc}
        />
      </label>
      <div className={styles.formRow}>
        <label>
          <span>Học kỳ</span>
          <Select
            ariaLabel="Học kỳ"
            value={maHocKy}
            options={props.terms.map((t) => ({ value: t.maHocKy, label: t.tenHocKy }))}
            onChange={setMaHocKy}
          />
        </label>
        <label>
          <span>Sức chứa</span>
          <input type="number" min={1} max={500} value={soLuong} onChange={(e) => setSoLuong(Number(e.target.value))} />
        </label>
      </div>
      <label>
        <span>Hình thức học</span>
        <Select ariaLabel="Hình thức học" value={mode} options={MODES} onChange={setMode} />
      </label>
      <CrossCampusField mode={mode} value={cross} onChange={setCross} />
      <label>
        <span>Giảng viên (tuỳ chọn)</span>
        <Select
          ariaLabel="Giảng viên"
          value={gv}
          options={[{ value: '', label: '— Chưa phân công —' }, ...props.teachers.map((t) => ({ value: t.maGiangVien, label: `${t.hoTen} (${t.maGiangVien})` }))]}
          onChange={setGv}
        />
      </label>
    </FormDialog>
  )
}

/* --- Sửa lớp ------------------------------------------------------------------ */

function EditDialog(props: { lop: ClassOffer; onClose: () => void; onDone: (lop: ClassOffer) => void }) {
  const { lop } = props
  const [soLuong, setSoLuong] = useState(lop.soLuongToiDa)
  const [trangThai, setTrangThai] = useState(lop.trangThai)
  const [mode, setMode] = useState(lop.hinhThucHoc)
  const [cross, setCross] = useState(lop.choPhepLienCoSo)
  const { busy, error, run } = useSubmit(props.onDone)

  return (
    <FormDialog
      title={`Sửa lớp ${lop.maLopHP}`}
      subtitle={`${lop.tenMonHoc} · môn, học kỳ, cơ sở không đổi được (nằm trong mã lớp)`}
      busy={busy}
      error={error}
      submitLabel="Lưu thay đổi"
      onClose={props.onClose}
      onSubmit={() =>
        void run(() =>
          api.updateClass(lop.maLopHP, {
            soLuongToiDa: soLuong,
            trangThai,
            hinhThucHoc: mode,
            choPhepLienCoSo: cross && mode === 'TRUC_TUYEN',
          }),
        )
      }
    >
      <div className={styles.formRow}>
        <label>
          <span>Sức chứa</span>
          <input type="number" min={1} max={500} value={soLuong} onChange={(e) => setSoLuong(Number(e.target.value))} />
          <small className={styles.sub}>Đang có {lop.soLuongDaDangKy} sinh viên — không hạ thấp hơn số này.</small>
        </label>
        <label>
          <span>Trạng thái</span>
          <Select ariaLabel="Trạng thái lớp" value={trangThai} options={EDITABLE_STATUS} onChange={setTrangThai} />
        </label>
      </div>
      <label>
        <span>Hình thức học</span>
        <Select ariaLabel="Hình thức học" value={mode} options={MODES} onChange={setMode} />
      </label>
      <CrossCampusField mode={mode} value={cross} onChange={setCross} />
      <p className={styles.hintBox}>
        Khoá điểm và huỷ lớp không đặt ở đây — dùng thao tác khoá bảng điểm và "Huỷ lớp" ở trang Đăng ký
        học phần.
      </p>
    </FormDialog>
  )
}

/* --- Phân công GV ------------------------------------------------------------- */

function TeacherDialog(props: {
  lop: ClassOffer
  teachers: TeacherOption[]
  onClose: () => void
  onDone: (lop: ClassOffer) => void
}) {
  const [gv, setGv] = useState(props.lop.maGiangVien ?? '')
  const { busy, error, run } = useSubmit(props.onDone)
  return (
    <FormDialog
      title={`Phân công giảng viên — ${props.lop.maLopHP}`}
      subtitle={`${props.lop.tenMonHoc} · ${formatLich(props.lop.lich)}`}
      busy={busy}
      error={error}
      submitLabel={gv ? 'Phân công' : 'Gỡ phân công'}
      onClose={props.onClose}
      onSubmit={() => void run(() => api.assignTeacher(props.lop.maLopHP, gv || null))}
    >
      <label>
        <span>Giảng viên</span>
        <Select
          ariaLabel="Giảng viên"
          value={gv}
          options={[
            { value: '', label: '— Gỡ phân công —' },
            ...props.teachers.map((t) => ({ value: t.maGiangVien, label: `${t.hoTen} (${t.maGiangVien})` })),
          ]}
          onChange={setGv}
        />
      </label>
      <p className={styles.hintBox}>
        Giảng viên phải cùng cơ sở với lớp và không dạy lớp khác trùng khung giờ — hệ thống sẽ từ chối nếu trùng.
      </p>
    </FormDialog>
  )
}

/* --- Xếp lịch ------------------------------------------------------------------ */

function ScheduleDialog(props: { lop: ClassOffer; onClose: () => void; onDone: (lop: ClassOffer) => void }) {
  const { lop } = props
  const [rows, setRows] = useState<SlotInput[]>(() => lop.lich.map((s) => ({ ...s })))
  const { busy, error, run } = useSubmit(props.onDone)
  const locked = lop.soLuongDaDangKy > 0

  function update(i: number, patch: Partial<SlotInput>) {
    setRows((r) => r.map((s, j) => (j === i ? { ...s, ...patch } : s)))
  }

  return (
    <FormDialog
      title={`Xếp lịch — ${lop.maLopHP}`}
      subtitle={`${lop.tenMonHoc} · GV ${lop.tenGiangVien ?? 'chưa phân công'} · lưu là thay toàn bộ lịch`}
      busy={busy}
      error={error}
      submitLabel="Lưu lịch"
      submitDisabled={locked}
      onClose={props.onClose}
      onSubmit={() => void run(() => api.setSchedule(lop.maLopHP, rows))}
    >
      {locked ? (
        <p className={styles.warn}>
          Lớp đã có <b>{lop.soLuongDaDangKy} sinh viên</b> đăng ký nên lịch bị khoá — đổi lịch lúc này là
          đổi cam kết với sinh viên mà hệ thống chưa có cách báo lại.
        </p>
      ) : null}
      <table className={styles.slotTable}>
        <thead>
          <tr>
            <th scope="col">Thứ</th>
            <th scope="col">Tiết BĐ</th>
            <th scope="col">Số tiết</th>
            <th scope="col">Phòng</th>
            <th scope="col">Tuần BĐ</th>
            <th scope="col">Tuần KT</th>
            <th scope="col">
              <span className={styles.srOnly}>Xoá</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={7} className={styles.empty}>
                Chưa có buổi nào — lưu danh sách rỗng là xoá hết lịch.
              </td>
            </tr>
          ) : (
            rows.map((s, i) => (
              <tr key={i}>
                <td>
                  <Select
                    ariaLabel={`Thứ của buổi ${i + 1}`}
                    value={s.thu}
                    options={THU}
                    onChange={(thu) => update(i, { thu })}
                    disabled={locked}
                  />
                </td>
                <td>
                  <input type="number" min={1} max={12} value={s.tietBatDau} disabled={locked} aria-label={`Tiết bắt đầu buổi ${i + 1}`}
                    onChange={(e) => update(i, { tietBatDau: Number(e.target.value) })} />
                </td>
                <td>
                  <input type="number" min={1} max={12} value={s.soTiet} disabled={locked} aria-label={`Số tiết buổi ${i + 1}`}
                    onChange={(e) => update(i, { soTiet: Number(e.target.value) })} />
                </td>
                <td>
                  <input value={s.phongHoc ?? ''} disabled={locked} placeholder="VD: 2B34" aria-label={`Phòng buổi ${i + 1}`}
                    onChange={(e) => update(i, { phongHoc: e.target.value })} />
                </td>
                <td>
                  <input type="number" min={1} value={s.tuanBatDau} disabled={locked} aria-label={`Tuần bắt đầu buổi ${i + 1}`}
                    onChange={(e) => update(i, { tuanBatDau: Number(e.target.value) })} />
                </td>
                <td>
                  <input type="number" min={1} value={s.tuanKetThuc} disabled={locked} aria-label={`Tuần kết thúc buổi ${i + 1}`}
                    onChange={(e) => update(i, { tuanKetThuc: Number(e.target.value) })} />
                </td>
                <td>
                  <button type="button" className={styles.removeBtn} disabled={locked} aria-label={`Xoá buổi ${i + 1}`}
                    onClick={() => setRows((r) => r.filter((_, j) => j !== i))}>
                    ×
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      <button
        type="button"
        className={styles.ghost}
        disabled={locked || rows.length >= 14}
        onClick={() =>
          setRows((r) => [...r, { thu: 2, tietBatDau: 1, soTiet: 3, phongHoc: '', tuanBatDau: 1, tuanKetThuc: 15 }])
        }
      >
        + Thêm buổi
      </button>
    </FormDialog>
  )
}
