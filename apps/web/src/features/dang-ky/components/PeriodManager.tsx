import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '@/shared/api'
import { Dialog, Icon, Select } from '@/shared/ui'

import * as api from '../api/mockEnrollmentApi'
import { DEMO_MA_HOC_KY, TERM_NAMES } from '../data/demo'
import { PHASE_LABEL, phaseOf, vnDateTime } from '../lib/period'
import type { EnrollmentPeriod } from '../types'
import styles from './Admin.module.scss'

const STATUS_OPTIONS = [
  { value: 'CHUA_MO', label: 'Chưa mở' },
  { value: 'DANG_MO', label: 'Đang mở' },
  { value: 'DA_DONG', label: 'Đã đóng' },
]

const VN_OFFSET_MS = 7 * 60 * 60 * 1000

/** ISO → giá trị ô `datetime-local` theo giờ Việt Nam (không theo múi giờ máy). */
function toVnInput(iso: string): string {
  return new Date(Date.parse(iso) + VN_OFFSET_MS).toISOString().slice(0, 16)
}

/** Ô `datetime-local` (giờ Việt Nam) → ISO UTC gửi server. */
function fromVnInput(value: string): string {
  return new Date(`${value}:00+07:00`).toISOString()
}

interface Draft {
  maDot: string | null
  maHocKy: string
  mo: string
  dong: string
  trangThai: string
}

/**
 * Quản lý đợt đăng ký của cơ sở (ADMIN_CO_SO). Cơ sở lấy từ JWT, không có
 * trong form. Mỗi học kỳ chỉ một đợt `DANG_MO` — server chặn bằng unique index.
 */
export function PeriodManager() {
  const [periods, setPeriods] = useState<EnrollmentPeriod[] | null>(null)
  const [draft, setDraft] = useState<Draft | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [notice, setNotice] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const now = Date.now()

  const reload = useCallback(async () => setPeriods(await api.listPeriods()), [])

  useEffect(() => {
    void reload()
  }, [reload])

  function openCreate() {
    const start = new Date(Date.now() + 86_400_000).toISOString()
    setDraft({
      maDot: null,
      maHocKy: DEMO_MA_HOC_KY,
      mo: toVnInput(start).slice(0, 11) + '08:00',
      dong: toVnInput(new Date(Date.parse(start) + 11 * 86_400_000).toISOString()).slice(0, 11) + '17:00',
      trangThai: 'CHUA_MO',
    })
    setFormError(null)
  }

  function openEdit(p: EnrollmentPeriod) {
    setDraft({
      maDot: p.maDot,
      maHocKy: p.maHocKy,
      mo: toVnInput(p.thoiGianMo),
      dong: toVnInput(p.thoiGianDong),
      trangThai: p.trangThai,
    })
    setFormError(null)
  }

  async function submit() {
    if (!draft) return
    setBusy(true)
    setFormError(null)
    try {
      const saved = await api.savePeriod(draft.maDot, {
        maHocKy: draft.maHocKy,
        thoiGianMo: fromVnInput(draft.mo),
        thoiGianDong: fromVnInput(draft.dong),
        trangThai: draft.trangThai,
      })
      setDraft(null)
      setNotice({ tone: 'success', text: `Đã lưu đợt ${saved.maDot}.` })
      await reload()
    } catch (cause) {
      setFormError(cause instanceof ApiError ? cause.message : 'Không lưu được. Vui lòng thử lại.')
    } finally {
      setBusy(false)
    }
  }

  /** Mở/đóng nhanh: chỉ đổi trạng thái, giữ nguyên khung giờ. */
  async function setStatus(p: EnrollmentPeriod, trangThai: string) {
    setBusy(true)
    setNotice(null)
    try {
      await api.savePeriod(p.maDot, { ...p, trangThai })
      setNotice({
        tone: 'success',
        text: `${trangThai === 'DANG_MO' ? 'Đã mở' : 'Đã đóng'} đợt ${p.maDot}.`,
      })
      await reload()
    } catch (cause) {
      setNotice({ tone: 'error', text: cause instanceof ApiError ? cause.message : 'Không lưu được.' })
    } finally {
      setBusy(false)
    }
  }

  if (!periods) return <p className={styles.muted}>Đang tải đợt đăng ký…</p>

  return (
    <div className={styles.block}>
      <div className={styles.toolbar}>
        <p className={styles.hint}>
          Sinh viên chỉ đăng ký và huỷ được khi đợt <b>Đang mở</b> và đang trong khung giờ. Mỗi học kỳ
          chỉ một đợt mở — muốn mở đợt bổ sung thì đóng đợt cũ trước.
        </p>
        <button type="button" className={styles.primary} onClick={openCreate}>
          + Tạo đợt mới
        </button>
      </div>

      {notice ? (
        <p className={`${styles.notice} ${styles[notice.tone]}`} role={notice.tone === 'error' ? 'alert' : 'status'}>
          {notice.text}
          <button type="button" onClick={() => setNotice(null)} aria-label="Đóng thông báo">
            ×
          </button>
        </p>
      ) : null}

      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">Mã đợt</th>
              <th scope="col" className={styles.left}>
                Học kỳ
              </th>
              <th scope="col">Thời gian mở</th>
              <th scope="col">Thời gian đóng</th>
              <th scope="col">Trạng thái</th>
              <th scope="col">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {periods.map((p) => {
              const phase = phaseOf(p, now)
              return (
                <tr key={p.maDot} className={phase === 'DANG_MO' ? styles.rowOpen : undefined}>
                  <td className={`${styles.center} ${styles.code}`}>{p.maDot}</td>
                  <td>{TERM_NAMES[p.maHocKy] ?? p.maHocKy}</td>
                  <td className={styles.center}>{vnDateTime(p.thoiGianMo)}</td>
                  <td className={styles.center}>{vnDateTime(p.thoiGianDong)}</td>
                  <td className={styles.center}>
                    <span className={`${styles.badge} ${styles[phase]}`}>{PHASE_LABEL[phase]}</span>
                  </td>
                  <td className={styles.actions}>
                    <button type="button" className={styles.ghost} onClick={() => openEdit(p)} disabled={busy}>
                      Sửa
                    </button>
                    {p.trangThai === 'DANG_MO' ? (
                      <button
                        type="button"
                        className={styles.danger}
                        onClick={() => void setStatus(p, 'DA_DONG')}
                        disabled={busy}
                      >
                        Đóng đợt
                      </button>
                    ) : (
                      <button
                        type="button"
                        className={styles.ghost}
                        onClick={() => void setStatus(p, 'DANG_MO')}
                        disabled={busy}
                      >
                        Mở đợt
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <Dialog
        open={draft !== null}
        onClose={() => setDraft(null)}
        ariaLabel={draft?.maDot ? `Sửa đợt ${draft.maDot}` : 'Tạo đợt đăng ký'}
        header={
          <p className={styles.dialogTitle}>
            {draft?.maDot ? `Sửa đợt ${draft.maDot}` : 'Tạo đợt đăng ký mới'}
            <small>Cơ sở lấy theo tài khoản quản trị · giờ Việt Nam (UTC+7)</small>
          </p>
        }
        footer={
          <>
            <button type="button" className={styles.ghost} onClick={() => setDraft(null)} disabled={busy}>
              Huỷ
            </button>
            <button type="button" className={styles.primary} onClick={() => void submit()} disabled={busy}>
              {busy ? 'Đang lưu…' : 'Lưu đợt'}
            </button>
          </>
        }
      >
        {draft ? (
          <div className={styles.form}>
            <label>
              <span>Học kỳ</span>
              {draft.maDot ? (
                /* PUT bỏ qua maHocKy — muốn đổi kỳ thì tạo đợt mới. */
                <input value={TERM_NAMES[draft.maHocKy] ?? draft.maHocKy} disabled />
              ) : (
                <Select
                  ariaLabel="Học kỳ"
                  value={draft.maHocKy}
                  options={Object.entries(TERM_NAMES).map(([value, label]) => ({ value, label }))}
                  onChange={(maHocKy) => setDraft({ ...draft, maHocKy })}
                />
              )}
            </label>
            <label>
              <span>Thời gian mở</span>
              <input
                type="datetime-local"
                value={draft.mo}
                onChange={(e) => setDraft({ ...draft, mo: e.target.value })}
              />
            </label>
            <label>
              <span>Thời gian đóng</span>
              <input
                type="datetime-local"
                value={draft.dong}
                onChange={(e) => setDraft({ ...draft, dong: e.target.value })}
              />
            </label>
            <label>
              <span>Trạng thái</span>
              <Select
                ariaLabel="Trạng thái đợt"
                value={draft.trangThai}
                options={STATUS_OPTIONS}
                onChange={(trangThai) => setDraft({ ...draft, trangThai })}
              />
            </label>
            {formError ? (
              <p className={`${styles.notice} ${styles.error}`} role="alert">
                <Icon name="close" size="12px" /> {formError}
              </p>
            ) : null}
          </div>
        ) : null}
      </Dialog>
    </div>
  )
}
