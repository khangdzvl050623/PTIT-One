import { useState } from 'react'
import type { FormEvent } from 'react'

import { ApiError } from '@/shared/api'
import { Dialog, Icon, Panel } from '@/shared/ui'

import { createDraft, previewNotice, sendDraft } from '../api/composeApi'
import { NOI_DUNG_MAX, TIEU_DE_MAX } from '../api/composeTypes'
import type { ComposeTarget, PreviewCount } from '../api/composeTypes'

import styles from './NoticeComposer.module.scss'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/** Kết quả lần gửi gần nhất — một box duy nhất, thành công hoặc thất bại. */
type Result = { ok: true; tong: number } | { ok: false; message: string }

/** Hộp đếm dự kiến: có người nhận thì cho xác nhận, 0 người thì chặn gửi. */
function PreviewBox({
  preview,
  locked,
  onConfirm,
}: {
  preview: PreviewCount
  locked: boolean
  onConfirm: () => void
}) {
  const tong = preview.soSinhVien + preview.soGiangVien

  if (tong === 0) {
    return (
      <p className={`${styles.previewBox} ${styles.empty}`} role="status">
        Lớp chưa có ai giữ chỗ. Báo sinh viên đăng ký vào
        lớp trước rồi quay lại.
      </p>
    )
  }

  return (
    <div className={styles.previewBox} role="status">
      <p>
        Sẽ gửi tới <b>{preview.soSinhVien} sinh viên</b>
        {preview.soGiangVien > 0 ? (
          <>
            {' '}và <b>{preview.soGiangVien} giảng viên</b>
          </>
        ) : null}{' '}
        (dự kiến, chốt lúc gửi).
      </p>
      <button
        className={styles.primary}
        type="button"
        disabled={locked}
        onClick={onConfirm}
      >
        <Icon name="check" size="15px" />
        Xác nhận gửi
      </button>
    </div>
  )
}

export interface NoticeComposerProps {
  target: ComposeTarget
  /** Tên hiển thị đích: `LỚP CSDL (INT1313)` · `CƠ SỞ HCM` · `TOÀN TRƯỜNG`. */
  tenNguoiNhan: string
  /** Admin được gửi cho giảng viên; giảng viên chỉ gửi sinh viên/cả lớp. */
  coDoiTuongGV?: boolean
}

/**
 * Soạn và gửi thông báo: nhập → xem trước số người nhận → xác nhận trong
 * dialog → tạo nháp + gửi luôn. Đổi đích (prop `target` đổi) thì form reset từ
 * đầu — giữ nội dung cũ sang đích mới là gửi nhầm chỗ.
 */
export function NoticeComposer({ target, tenNguoiNhan, coDoiTuongGV = false }: NoticeComposerProps) {
  const [tieuDe, setTieuDe] = useState('')
  const [noiDung, setNoiDung] = useState('')
  const [mucDo, setMucDo] = useState('THONG_THUONG')
  const [doiTuong, setDoiTuong] = useState('SINH_VIEN')
  const [lienKet, setLienKet] = useState('')
  const [preview, setPreview] = useState<PreviewCount | null>(null)
  const [checking, setChecking] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [pending, setPending] = useState(false)
  const [result, setResult] = useState<Result | null>(null)

  const succeeded = result?.ok === true
  const locked = checking || pending || succeeded

  function input() {
    return {
      tieuDe: tieuDe.trim(),
      noiDung: noiDung.trim(),
      mucDo,
      doiTuong,
      lienKet: lienKet.trim(),
    }
  }

  /** Kiểm client trùng backend (`SaveNotificationRequest`): câu lỗi khớp chữ. */
  function loiNhap(): string | null {
    const body = input()
    if (body.tieuDe.length === 0) return 'Nhập tiêu đề thông báo.'
    if (body.tieuDe.length > TIEU_DE_MAX) return `Tiêu đề tối đa ${TIEU_DE_MAX} ký tự.`
    if (body.noiDung.length === 0) return 'Nhập nội dung thông báo.'
    if (body.noiDung.length > NOI_DUNG_MAX) return `Nội dung tối đa ${NOI_DUNG_MAX} ký tự.`
    if (body.lienKet.length > 0 && !/^\/(?!\/)\S*$/.test(body.lienKet)) {
      return 'Liên kết chỉ nhận đường dẫn nội bộ bắt đầu bằng /, không khoảng trắng (ví dụ /giang-vien/nhap-diem).'
    }
    return null
  }

  function handlePreview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const loi = loiNhap()
    if (loi) {
      setResult({ ok: false, message: loi })
      return
    }
    setChecking(true)
    setResult(null)
    setPreview(null)
    void previewNotice(target, input()).then(
      (count) => {
        setPreview(count)
        setChecking(false)
      },
      (cause: unknown) => {
        setResult({ ok: false, message: cause instanceof ApiError ? cause.message : NETWORK })
        setChecking(false)
      },
    )
  }

  function doSend() {
    setPending(true)
    setResult(null)
    /* Nối tiếp tạo nháp rồi gửi luôn: dừng ở nháp mà tưởng đã gửi thì người
       nhận không thấy gì. Nháp mồ côi (tạo xong mà gửi hỏng) server giữ, không
       hiện cho người nhận nên không gây hại. */
    void createDraft(target, input()).then(
      (draft) =>
        sendDraft(draft.maThongBao).then(
          () => {
            setConfirmOpen(false)
            setResult({ ok: true, tong: preview ? preview.soSinhVien + preview.soGiangVien : 0 })
            setPending(false)
          },
          (cause: unknown) => {
            setConfirmOpen(false)
            setResult({
              ok: false,
              message: cause instanceof ApiError ? cause.message : NETWORK,
            })
            setPending(false)
          },
        ),
      (cause: unknown) => {
        setConfirmOpen(false)
        setResult({ ok: false, message: cause instanceof ApiError ? cause.message : NETWORK })
        setPending(false)
      },
    )
  }

  function resetAll() {
    setTieuDe('')
    setNoiDung('')
    setMucDo('THONG_THUONG')
    setDoiTuong('SINH_VIEN')
    setLienKet('')
    setPreview(null)
    setResult(null)
  }

  return (
    <Panel title={`GỬI THÔNG BÁO — ${tenNguoiNhan.toUpperCase()}`} icon="bell">
      <form className={styles.form} onSubmit={handlePreview}>
        <label className={styles.field}>
          Tiêu đề
          <input
            className={styles.input}
            value={tieuDe}
            onChange={(event) => setTieuDe(event.target.value)}
            placeholder="Ví dụ: Kết thúc học phần và chuẩn bị thi cuối kỳ"
            maxLength={TIEU_DE_MAX}
            disabled={locked}
            required
          />
        </label>

        <label className={styles.field}>
          Nội dung
          <textarea
            className={`${styles.input} ${styles.area}`}
            value={noiDung}
            onChange={(event) => setNoiDung(event.target.value)}
            placeholder="Nội dung đầy đủ người nhận sẽ đọc trong hộp thư"
            rows={5}
            maxLength={NOI_DUNG_MAX}
            disabled={locked}
            required
          />
        </label>

        <div className={styles.row2}>
          <label className={styles.field}>
            Mức độ
            <select
              className={styles.input}
              value={mucDo}
              onChange={(event) => setMucDo(event.target.value)}
              disabled={locked}
            >
              <option value="THONG_THUONG">Thông thường</option>
              <option value="QUAN_TRONG">Quan trọng</option>
            </select>
          </label>

          <label className={styles.field}>
            Người nhận
            <select
              className={styles.input}
              value={doiTuong}
              onChange={(event) => setDoiTuong(event.target.value)}
              disabled={locked}
            >
              <option value="SINH_VIEN">Sinh viên</option>
              {coDoiTuongGV ? <option value="GIANG_VIEN">Giảng viên</option> : null}
              <option value="TAT_CA">Tất cả</option>
            </select>
          </label>
        </div>

        <label className={styles.field}>
          Liên kết <span className={styles.optional}>(tuỳ chọn)</span>
          <input
            className={styles.input}
            value={lienKet}
            onChange={(event) => setLienKet(event.target.value)}
            placeholder="/sinh-vien/dang-ky — bỏ trống nếu không có"
            disabled={locked}
          />
        </label>

        <div className={styles.actions}>
          <button className={styles.primary} type="submit" disabled={locked}>
            <Icon name="bell" size="15px" />
            {checking ? 'Đang đếm…' : succeeded ? 'Đã gửi' : 'Xem trước số người nhận'}
          </button>
          {succeeded ? (
            <button className={styles.ghost} type="button" onClick={resetAll}>
              Soạn tin khác
            </button>
          ) : null}
        </div>

        {preview && !succeeded ? (
          <PreviewBox
            preview={preview}
            locked={locked}
            onConfirm={() => setConfirmOpen(true)}
          />
        ) : null}

        {result?.ok ? (
          <p className={`${styles.message} ${styles.success}`} role="status">
            <span className={styles.successIcon}>
              <Icon name="check" size="15px" />
            </span>
            <span>
              Đã gửi thông báo tới <b>{tenNguoiNhan}</b> (~{result.tong} người nhận).
            </span>
          </p>
        ) : null}

        {result && !result.ok ? (
          <p key={result.message} className={`${styles.message} ${styles.error}`} role="alert">
            {result.message}
          </p>
        ) : null}
      </form>

      <Dialog
        open={confirmOpen}
        onClose={() => {
          if (!pending) setConfirmOpen(false)
        }}
        ariaLabel={`Xác nhận gửi thông báo tới ${tenNguoiNhan}`}
        header={<p className={styles.dialogTitle}>Gửi thông báo tới {tenNguoiNhan}?</p>}
        footer={
          <>
            <button
              type="button"
              className={styles.ghost}
              onClick={() => setConfirmOpen(false)}
              disabled={pending}
            >
              Để kiểm tra lại
            </button>
            <button
              type="button"
              className={styles.primary}
              onClick={() => void doSend()}
              disabled={pending}
            >
              <Icon name="check" size="13px" />
              {pending ? 'Đang gửi…' : 'Xác nhận gửi'}
            </button>
          </>
        }
      >
        <div className={styles.confirmBody}>
          <p className={styles.confirmAccount}>
            Tiêu đề: <b>{tieuDe.trim() || '—'}</b>
          </p>
          <p>
            Người nhận sẽ thấy tin trong hộp thư ngay sau khi gửi. Bản đã gửi{' '}
            <b>không sửa, không xoá, không gửi lại</b> được.
          </p>
        </div>
      </Dialog>
    </Panel>
  )
}
