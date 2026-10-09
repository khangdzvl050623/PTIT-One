import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { ApiError } from '@/shared/api'
import { downloadCsv, useAsyncData } from '@/shared/lib'
import { Dialog, Icon, Select, Skeleton, SkeletonRows } from '@/shared/ui'

import * as api from '../api/directoryApi'
import type {
  AccountSummary,
  ProvisionResult,
  LoaiNguoiDung,
  TrangThaiTaiKhoan,
} from '../types'
import { ProvisionResultCard } from './ProvisionResultCard'
import styles from './AccountDirectory.module.scss'

/** Giá trị "mọi cơ sở" / "mọi loại" của ô lọc (API bỏ trống tham số). */
const ALL = 'ALL'

const LOAI_LABELS: Record<LoaiNguoiDung, string> = {
  SINH_VIEN: 'Sinh viên',
  GIANG_VIEN: 'Giảng viên',
  ADMIN_CO_SO: 'Quản trị đào tạo',
  ADMIN_MASTER: 'Quản trị danh mục',
}

const TRANG_THAI_LABELS: Record<string, string> = {
  HOAT_DONG: 'Hoạt động',
  NGUNG: 'Đã khoá',
}

type Modal =
  | { kind: 'result'; result: ProvisionResult; tieuDe: string }
  | { kind: 'status'; account: AccountSummary; toi: TrangThaiTaiKhoan }
  | { kind: 'reissue'; account: AccountSummary }
  | { kind: 'email'; account: AccountSummary }
  | { kind: 'reset'; account: AccountSummary }

function fold(s: string): string {
  return s.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
}

export interface AccountDirectoryProps {
  /** Hiện lối "Đặt lại dữ liệu demo" — bỏ khi đã nối API thật. */
  demo?: boolean
  /**
   * Đổi giá trị là danh bạ tải lại. Khung cấp hồ sơ nằm ở cột bên cạnh nên
   * không gọi thẳng vào đây được; trang tăng số này sau mỗi lần cấp xong.
   */
  reloadKey?: number
}

/**
 * Hồ sơ và tài khoản (F02) — chỉ `ADMIN_MASTER`.
 *
 * Danh bạ là bảng **Master sở hữu**: Admin cơ sở chỉ đọc (B3) và ở Phần 2 site
 * bị `DENY` ghi bảng nhân bản, nên màn này không bao giờ hiện cho vai khác —
 * tuyến đã gác bằng `RequireAuth roles={['ADMIN_MASTER']}`.
 *
 * Cấp tài khoản đi **kèm** cấp hồ sơ (`POST /api/students` / `/api/teachers`),
 * không có đường tạo tài khoản rỗng.
 */
export function AccountDirectory({ demo = false, reloadKey = 0 }: AccountDirectoryProps) {
  const [accounts, setAccounts] = useState<AccountSummary[] | null>(null)
  const [maCoSo, setMaCoSo] = useState<string>(ALL)
  const [loai, setLoai] = useState<string>(ALL)
  const [query, setQuery] = useState('')
  const [modal, setModal] = useState<Modal | null>(null)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const [emailMoi, setEmailMoi] = useState('')
  /* Lấy từ API, không dùng hằng số: ô lọc liệt kê cơ sở không có thật thì
     người dùng chọn xong chỉ thấy bảng rỗng mà không hiểu vì sao. */
  const campuses = useAsyncData(api.fetchCampuses)

  const reload = useCallback(async () => {
    setAccounts(
      await api.listAccounts(
        maCoSo === ALL ? null : maCoSo,
        loai === ALL ? null : (loai as LoaiNguoiDung),
      ),
    )
  }, [maCoSo, loai])

  useEffect(() => {
    void reload()
    // `reloadKey`: cấp hồ sơ xong ở cột bên cạnh thì danh bạ phải có người mới.
  }, [reload, reloadKey])

  const visible = useMemo(() => {
    const q = fold(query.trim())
    return (accounts ?? []).filter(
      (a) => !q || fold(`${a.tenDangNhap} ${a.maThucThe ?? ''}`).includes(q),
    )
  }, [accounts, query])

  async function run(action: () => Promise<string>) {
    setBusy(true)
    setNotice(null)
    try {
      setNotice({ tone: 'success', text: await action() })
      await reload()
    } catch (cause) {
      setNotice({
        tone: 'error',
        text: cause instanceof ApiError ? cause.message : 'Không thực hiện được. Vui lòng thử lại.',
      })
    } finally {
      setBusy(false)
      setModal(null)
    }
  }

  function confirmStatus() {
    if (modal?.kind !== 'status') return
    const { account, toi } = modal
    void run(async () => {
      await api.changeStatus(account.tenDangNhap, toi)
      return toi === 'NGUNG'
        ? `Đã khoá tài khoản ${account.tenDangNhap}. Mọi phiên đăng nhập của người này bị thu hồi ngay.`
        : `Đã mở khoá tài khoản ${account.tenDangNhap}.`
    })
  }

  function confirmReissue(guiEmail: boolean) {
    if (modal?.kind !== 'reissue') return
    const { account } = modal
    setBusy(true)
    setNotice(null)
    void api
      .reissueActivationCode(account.tenDangNhap, guiEmail)
      .then(
        (code) =>
          setModal({
            kind: 'result',
            tieuDe: `Mã kích hoạt mới — ${account.tenDangNhap}`,
            result: {
              ma: account.tenDangNhap,
              hoTen: account.maThucThe ?? account.tenDangNhap,
              loai: account.loaiNguoiDung === 'GIANG_VIEN' ? 'GIANG_VIEN' : 'SINH_VIEN',
              kichHoat: code,
              matKhauBanDau: null,
            },
          }),
        (cause: unknown) => {
          setNotice({
            tone: 'error',
            text: cause instanceof ApiError ? cause.message : 'Không cấp lại được mã.',
          })
          setModal(null)
        },
      )
      .finally(() => setBusy(false))
  }

  function confirmEmail() {
    if (modal?.kind !== 'email') return
    const { account } = modal
    setBusy(true)
    setNotice(null)
    void api
      .changeAccountEmail(account.tenDangNhap, emailMoi)
      .then(
        (saved) => {
          setNotice({
            tone: 'success',
            text: `Đã đặt email ${saved.email} cho ${account.tenDangNhap}. Chưa xác minh — `
              + 'sẽ tự xác minh khi người dùng dùng được mã gửi tới đó.',
          })
          setModal(null)
          setEmailMoi('')
        },
        (cause: unknown) => {
          setNotice({
            tone: 'error',
            text: cause instanceof ApiError ? cause.message : 'Không đổi được email.',
          })
        },
      )
      .finally(() => setBusy(false))
  }

  function confirmReset() {
    if (modal?.kind !== 'reset') return
    const { account } = modal
    setBusy(true)
    setNotice(null)
    void api
      .forcePasswordReset(account.tenDangNhap)
      .then(
        (code) => {
          setModal({
            kind: 'result',
            tieuDe: `Cấp lại mật khẩu — ${account.tenDangNhap}`,
            result: {
              ma: account.tenDangNhap,
              hoTen: account.maThucThe ?? account.tenDangNhap,
              loai: account.loaiNguoiDung === 'GIANG_VIEN' ? 'GIANG_VIEN' : 'SINH_VIEN',
              kichHoat: code,
              matKhauBanDau: null,
            },
          })
          void reload()
        },
        (cause: unknown) => {
          setNotice({
            tone: 'error',
            text: cause instanceof ApiError ? cause.message : 'Không cấp lại được mật khẩu.',
          })
          setModal(null)
        },
      )
      .finally(() => setBusy(false))
  }

  /* Đang tải thì vẫn dựng nguyên bộ khung: thanh lọc dùng được ngay, chỗ bảng
     là khung xương. Trả về một dòng "Đang tải…" sẽ làm cả màn nhảy một nhịp
     khi dữ liệu về. */
  const dangTai = accounts === null
  const chuaKichHoat = accounts?.filter((a) => !a.daKichHoat).length ?? 0
  const daKhoa = accounts?.filter((a) => a.trangThai === 'NGUNG').length ?? 0

  return (
    <div className={styles.block}>
      <div className={styles.toolbar} data-print="hide">
        <Select
          ariaLabel="Lọc cơ sở"
          className={styles.filter}
          value={maCoSo}
          options={[
            { value: ALL, label: 'Mọi cơ sở' },
            ...(campuses.data ?? []).map((c) => ({ value: c.maCoSo, label: c.tenCoSo })),
          ]}
          onChange={setMaCoSo}
        />
        <Select
          ariaLabel="Lọc loại người dùng"
          className={styles.filter}
          value={loai}
          options={[
            { value: ALL, label: 'Mọi loại người dùng' },
            ...(Object.keys(LOAI_LABELS) as LoaiNguoiDung[]).map((v) => ({
              value: v,
              label: LOAI_LABELS[v],
            })),
          ]}
          onChange={setLoai}
        />
        <label className={styles.search}>
          <Icon name="search" size="14px" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo tên đăng nhập hoặc mã hồ sơ…"
            aria-label="Tìm tài khoản"
          />
        </label>
      </div>

      <p className={styles.summary}>
        {dangTai ? (
          <Skeleton width="240px" height="15px" />
        ) : (
          <>
            {accounts.length} tài khoản · {chuaKichHoat} chưa kích hoạt · {daKhoa} đang khoá
          </>
        )}
      </p>

      {notice ? (
        <p
          className={`${styles.notice} ${styles[notice.tone]}`}
          role={notice.tone === 'error' ? 'alert' : 'status'}
        >
          {notice.text}
          <button type="button" onClick={() => setNotice(null)} aria-label="Đóng thông báo">
            ×
          </button>
        </p>
      ) : null}

      <div className={styles.scroll} aria-busy={dangTai}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col" className={styles.left}>
                Tên đăng nhập
              </th>
              <th scope="col">Loại người dùng</th>
              <th scope="col">Cơ sở</th>
              <th scope="col">Mã hồ sơ</th>
              <th scope="col">Kích hoạt</th>
              <th scope="col">Trạng thái</th>
              <th scope="col">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {dangTai ? (
              <SkeletonRows cols={7} rows={6} />
            ) : visible.length === 0 ? (
              <tr>
                <td colSpan={7} className={styles.empty}>
                  Không tìm thấy tài khoản nào
                </td>
              </tr>
            ) : (
              visible.map((a) => {
                const master = a.loaiNguoiDung === 'ADMIN_MASTER'
                const khoa = a.trangThai === 'NGUNG'
                return (
                  <tr key={a.tenDangNhap} className={khoa ? styles.rowLocked : undefined}>
                    <td className={styles.code}>{a.tenDangNhap}</td>
                    <td className={styles.center}>{LOAI_LABELS[a.loaiNguoiDung]}</td>
                    {/* Master không thuộc cơ sở nào — gạch, không bịa mã. */}
                    <td className={styles.center}>{a.maCoSo ?? '—'}</td>
                    <td className={`${styles.center} ${styles.code}`}>{a.maThucThe ?? '—'}</td>
                    <td className={styles.center}>
                      <span className={`${styles.badge} ${a.daKichHoat ? styles.on : styles.off}`}>
                        {a.daKichHoat ? 'Đã kích hoạt' : 'Chưa kích hoạt'}
                      </span>
                    </td>
                    <td className={styles.center}>
                      <span className={`${styles.badge} ${khoa ? styles.locked : styles.on}`}>
                        {TRANG_THAI_LABELS[a.trangThai] ?? a.trangThai}
                      </span>
                    </td>
                    <td className={styles.actions}>
                      {/* Master: server chặn cả cấp mã lẫn đổi trạng thái, nên không hiện nút. */}
                      {master ? (
                        <span className={styles.muted}>Không quản trị được</span>
                      ) : (
                        <>
                          {a.daKichHoat ? (
                            /* Đã kích hoạt thì không cấp lại mã được nữa —
                               lối thoát khi quên mật khẩu là cấp lại mật khẩu. */
                            <button
                              type="button"
                              className={styles.ghost}
                              disabled={busy}
                              onClick={() => setModal({ kind: 'reset', account: a })}
                            >
                              Cấp lại mật khẩu
                            </button>
                          ) : (
                            <button
                              type="button"
                              className={styles.ghost}
                              disabled={busy}
                              onClick={() => setModal({ kind: 'reissue', account: a })}
                            >
                              Cấp lại mã
                            </button>
                          )}
                          <button
                            type="button"
                            className={styles.ghost}
                            disabled={busy}
                            onClick={() => {
                              setEmailMoi('')
                              setModal({ kind: 'email', account: a })
                            }}
                          >
                            Đổi email
                          </button>
                          <button
                            type="button"
                            className={khoa ? styles.ghost : styles.danger}
                            disabled={busy}
                            onClick={() =>
                              setModal({ kind: 'status', account: a, toi: khoa ? 'HOAT_DONG' : 'NGUNG' })
                            }
                          >
                            {khoa ? 'Mở khoá' : 'Khoá'}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      <div className={styles.foot} data-print="hide">
        {demo ? (
          <button
            type="button"
            className={styles.linkBtn}
            onClick={() => {
              api.resetDemo()
              setNotice(null)
              void reload()
            }}
          >
            Đặt lại dữ liệu demo
          </button>
        ) : (
          <span />
        )}
        <button
          type="button"
          className={styles.ghost}
          onClick={() =>
            downloadCsv(
              [
                ['Tên đăng nhập', 'Loại người dùng', 'Cơ sở', 'Mã hồ sơ', 'Kích hoạt', 'Trạng thái'],
                ...visible.map((a) => [
                  a.tenDangNhap,
                  LOAI_LABELS[a.loaiNguoiDung],
                  a.maCoSo,
                  a.maThucThe,
                  a.daKichHoat ? 'Đã kích hoạt' : 'Chưa kích hoạt',
                  TRANG_THAI_LABELS[a.trangThai] ?? a.trangThai,
                ]),
              ],
              'danh-ba-tai-khoan.csv',
            )
          }
        >
          <Icon name="download" size="13px" /> Xuất Excel
        </button>
      </div>

      {modal?.kind === 'result' ? (
        <ProvisionResultCard
          result={modal.result}
          title={modal.tieuDe}
          onClose={() => setModal(null)}
        />
      ) : null}

      {modal?.kind === 'status' ? (
        <ConfirmDialog
          title={`${modal.toi === 'NGUNG' ? 'Khoá' : 'Mở khoá'} tài khoản ${modal.account.tenDangNhap}`}
          confirmLabel={modal.toi === 'NGUNG' ? 'Khoá tài khoản' : 'Mở khoá'}
          danger={modal.toi === 'NGUNG'}
          busy={busy}
          onClose={() => setModal(null)}
          onConfirm={confirmStatus}
        >
          {modal.toi === 'NGUNG' ? (
            <p className={styles.warn}>
              Khoá sẽ <b>thu hồi mọi phiên đăng nhập</b> của tài khoản này ngay lập tức — kể cả
              phiên đang mở trên máy khác. Người dùng không đăng nhập lại được cho tới khi mở khoá.
            </p>
          ) : (
            <p>Tài khoản sẽ đăng nhập lại được bình thường. Mật khẩu cũ vẫn dùng được.</p>
          )}
        </ConfirmDialog>
      ) : null}

      {modal?.kind === 'reissue' ? (
        <ConfirmDialog
          title={`Cấp lại mã kích hoạt — ${modal.account.tenDangNhap}`}
          confirmLabel="Cấp mã trao tay"
          busy={busy}
          onClose={() => setModal(null)}
          onConfirm={() => confirmReissue(false)}
          extra={
            <button
              type="button"
              className={styles.ghost}
              disabled={busy}
              onClick={() => confirmReissue(true)}
            >
              Gửi qua email đã lưu
            </button>
          }
        >
          <p>
            Mã cũ <b>mất hiệu lực ngay</b>. Chỉ cấp được cho tài khoản chưa kích hoạt.
          </p>
          <p className={styles.hintBox}>
            Gửi qua email thì bạn <b>không thấy mã</b> — người nhận tự mở thư, và kích hoạt thành
            công cũng là bằng chứng họ sở hữu hòm thư đó. Chọn <b>trao tay</b> khi tài khoản chưa
            có email hoặc thư không tới được.
          </p>
        </ConfirmDialog>
      ) : null}

      {modal?.kind === 'email' ? (
        <ConfirmDialog
          title={`Đổi email — ${modal.account.tenDangNhap}`}
          confirmLabel="Lưu email"
          busy={busy}
          confirmDisabled={emailMoi.trim().length === 0}
          onClose={() => setModal(null)}
          onConfirm={confirmEmail}
        >
          <p>
            Dùng khi người dùng <b>mất quyền vào hòm thư cũ</b> nên không tự đổi được — tự đổi cần
            mật khẩu hiện tại.
          </p>
          <label className={styles.dialogField}>
            Email mới
            <input
              type="email"
              value={emailMoi}
              onChange={(event) => setEmailMoi(event.target.value)}
              placeholder="vidu@gmail.com"
              maxLength={254}
              disabled={busy}
            />
          </label>
          <p className={styles.hintBox}>
            Email mới ở trạng thái <b>chưa xác minh</b>, và sẽ tự thành đã xác minh khi người dùng
            dùng được mã gửi tới đó. Thao tác này <b>không</b> đụng tới mật khẩu.
          </p>
        </ConfirmDialog>
      ) : null}

      {modal?.kind === 'reset' ? (
        <ConfirmDialog
          title={`Cấp lại mật khẩu — ${modal.account.tenDangNhap}`}
          confirmLabel="Cấp lại mật khẩu"
          busy={busy}
          onClose={() => setModal(null)}
          onConfirm={confirmReset}
        >
          <p>
            Mọi phiên bị thu hồi và <b>mật khẩu hiện tại bị xoá</b>. Tài khoản{' '}
            <b>không đăng nhập được</b> cho tới khi chủ tài khoản tự đặt mật khẩu mới bằng mã này.
          </p>
          <p className={styles.hintBox}>
            Bạn <b>không</b> đặt mật khẩu hộ. Tài khoản có email thì mã chỉ đi qua thư và bạn không
            thấy mã; không có email thì mã hiện <b>một lần</b> để đọc cho người dùng qua điện thoại.
          </p>
        </ConfirmDialog>
      ) : null}
    </div>
  )
}

/* --- Hộp thoại xác nhận dùng chung ---------------------------------------- */

/**
 * Luôn mở — nơi gọi **tháo hẳn** component khi đóng thay vì hạ cờ `open`.
 *
 * Giữ nó trên cây rồi bật/tắt `open` thì lúc chuyển thẳng sang hộp thoại khác
 * (xác nhận cấp mã → hiện mã), sự kiện `close` của `<dialog>` cũ sẽ gọi
 * `onClose` và xoá mất hộp thoại vừa mở.
 */
function ConfirmDialog(props: {
  title: string
  confirmLabel: string
  busy: boolean
  danger?: boolean
  extra?: ReactNode
  onClose: () => void
  onConfirm: () => void
  /**
   * Khoá riêng nút xác nhận khi biểu mẫu chưa hợp lệ.
   *
   * Tách khỏi `busy`: `busy` còn đổi nhãn thành "Đang xử lý…" và khoá cả nút
   * Huỷ, nên dùng nó để chặn một ô trống sẽ khiến hộp thoại không đóng được.
   */
  confirmDisabled?: boolean
  children: ReactNode
}) {
  return (
    <Dialog
      open
      onClose={props.onClose}
      ariaLabel={props.title}
      header={<p className={styles.dialogTitle}>{props.title}</p>}
      footer={
        <>
          <button type="button" className={styles.ghost} onClick={props.onClose} disabled={props.busy}>
            Huỷ
          </button>
          {props.extra}
          <button
            type="button"
            className={props.danger ? styles.dangerSolid : styles.primary}
            onClick={props.onConfirm}
            disabled={props.busy || props.confirmDisabled}
          >
            {props.busy ? 'Đang xử lý…' : props.confirmLabel}
          </button>
        </>
      }
    >
      <div className={styles.form}>{props.children}</div>
    </Dialog>
  )
}
