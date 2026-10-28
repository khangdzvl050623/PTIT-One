import { useCallback, useState } from 'react'

import { useAuth } from '@/features/auth'
import { fetchCampuses } from '@/features/danh-muc'
import type { Campus } from '@/features/danh-muc'
import { adminClasses } from '@/features/dang-ky'
import type { ClassOffer } from '@/features/dang-ky'
import { useTerms } from '@/features/lich-hoc'
import { NoticeComposer, SentBox } from '@/features/thong-bao'
import type { ComposeTarget } from '@/features/thong-bao'
import { useAsyncData } from '@/shared/lib'
import { Panel, Select } from '@/shared/ui'

import styles from './AdminNoticePage.module.scss'

type PhamVi = ComposeTarget['phamVi']

/**
 * Soạn thông báo của admin (cơ sở và Master): chọn phạm vi → chọn đích → soạn.
 *
 * - Admin cơ sở: `CO_SO` cố định cơ sở nhà mình + `LOP_HOC_PHAN` trong cơ sở
 *   mình (server tự giới hạn, xin cơ sở khác ăn `403`).
 * - Admin Master: thêm `TOAN_TRUONG`, chọn cơ sở bất kỳ, chọn lớp thuộc cơ sở
 *   đã chọn (lọc theo `maCoSoHost` vì `GET /api/classes` không nhận tham số
 *   cơ sở).
 */
export function AdminNoticePage() {
  const { user } = useAuth()
  const master = user?.role === 'ADMIN_MASTER'
  const coSoNha = user?.homeCampus ?? null

  const [phamVi, setPhamVi] = useState<PhamVi>('CO_SO')
  const [maCoSo, setMaCoSo] = useState('')
  const [maLopHP, setMaLopHP] = useState('')

  const { defaultTerm, loading: loadingKy, error: errorKy } = useTerms()
  const campuses = useAsyncData(fetchCampuses)
  const loadClasses = useCallback(
    () =>
      phamVi === 'LOP_HOC_PHAN' && defaultTerm
        ? adminClasses(defaultTerm)
        : Promise.resolve([] as ClassOffer[]),
    [phamVi, defaultTerm],
  )
  const classes = useAsyncData(loadClasses)

  /* Cơ sở hiệu lực: cơ sở nhà với admin cơ sở, cơ sở đã chọn với Master. */
  const coSoHieuLuc = master ? maCoSo || null : coSoNha
  /* Danh sách lớp đã lọc theo cơ sở (Master) — admin cơ sở server lo. */
  const lopTheoCoSo = (classes.data ?? []).filter(
    (c) => !master || !coSoHieuLuc || c.maCoSoHost === coSoHieuLuc,
  )
  const lop = lopTheoCoSo.find((c) => c.maLopHP === maLopHP) ?? null

  const target: ComposeTarget | null =
    phamVi === 'TOAN_TRUONG'
      ? { phamVi, maCoSo: null, maLopHP: null }
      : phamVi === 'CO_SO' && coSoHieuLuc
        ? { phamVi, maCoSo: coSoHieuLuc, maLopHP: null }
        : phamVi === 'LOP_HOC_PHAN' && lop
          ? { phamVi, maCoSo: null, maLopHP: lop.maLopHP }
          : null

  function chonPhamVi(next: PhamVi) {
    setPhamVi(next)
    setMaLopHP('')
  }

  return (
    <div className={styles.page}>
      <Panel title="GỬI THÔNG BÁO" icon="bell">
        <div className={styles.grid}>
          <label className={styles.field}>
            Phạm vi
            <Select
              value={phamVi}
              options={[
                ...(master ? [{ value: 'TOAN_TRUONG' as const, label: 'Toàn trường' }] : []),
                { value: 'CO_SO' as const, label: master ? 'Một cơ sở' : `Cơ sở ${coSoNha ?? ''}`.trim() },
                { value: 'LOP_HOC_PHAN' as const, label: 'Một lớp học phần' },
              ]}
              onChange={chonPhamVi}
              ariaLabel="Phạm vi thông báo"
            />
          </label>

          {master && phamVi !== 'TOAN_TRUONG' ? (
            <label className={styles.field}>
              Cơ sở
              <Select
                value={maCoSo}
                options={[
                  { value: '', label: 'CHỌN CƠ SỞ' },
                  ...(campuses.data ?? []).map((c: Campus) => ({
                    value: c.maCoSo,
                    label: `${c.maCoSo} — ${c.tenCoSo}`,
                  })),
                ]}
                onChange={(v) => {
                  setMaCoSo(v)
                  setMaLopHP('')
                }}
                ariaLabel="Cơ sở nhận thông báo"
                disabled={campuses.loading}
              />
            </label>
          ) : null}

          {phamVi === 'LOP_HOC_PHAN' ? (
            <label className={styles.field}>
              Lớp học phần
              <Select
                value={maLopHP}
                options={[
                  { value: '', label: 'CHỌN LỚP' },
                  ...lopTheoCoSo.map((c) => ({
                    value: c.maLopHP,
                    label: `${c.maLopHP} — ${c.tenMonHoc} (${c.soLuongDaDangKy} SV)`,
                  })),
                ]}
                onChange={setMaLopHP}
                ariaLabel="Lớp nhận thông báo"
                disabled={classes.loading || loadingKy}
              />
            </label>
          ) : null}
        </div>

        {loadingKy || classes.loading || campuses.loading ? <p>Đang tải danh mục…</p> : null}
        {errorKy ? <p role="alert">{errorKy}</p> : null}
        {classes.error ? <p role="alert">{classes.error}</p> : null}
        {campuses.error ? <p role="alert">{campuses.error}</p> : null}
        {target ? (
          <p className={styles.hint}>
            Tin gửi tới <b>{tenNguoiNhan(target, campuses.data ?? [], lop)}</b>.
          </p>
        ) : (
          <p className={styles.hint}>Chọn đủ phạm vi và đích nhận ở trên để bắt đầu soạn.</p>
        )}
      </Panel>

      {target ? (
        <NoticeComposer
          key={`${target.phamVi}:${target.maCoSo ?? ''}:${target.maLopHP ?? ''}`}
          target={target}
          tenNguoiNhan={tenNguoiNhan(target, campuses.data ?? [], lop)}
          coDoiTuongGV
        />
      ) : null}

      {/* Hộp thư gửi nằm ngay dưới chỗ soạn: gửi xong kiểm tra ngay được. */}
      <SentBox />
    </div>
  )
}

function tenNguoiNhan(
  target: ComposeTarget,
  campuses: readonly Campus[],
  lop: ClassOffer | null,
): string {
  if (target.phamVi === 'TOAN_TRUONG') return 'toàn trường'
  if (target.phamVi === 'CO_SO') {
    const ten = campuses.find((c) => c.maCoSo === target.maCoSo)?.tenCoSo
    return `cơ sở ${target.maCoSo}${ten ? ` (${ten})` : ''}`
  }
  return `lớp ${lop?.tenMonHoc ?? ''} (${target.maLopHP ?? ''})`.trim()
}
