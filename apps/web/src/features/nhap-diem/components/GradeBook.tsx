import { useMemo, useState } from 'react'
import type { KeyboardEvent } from 'react'

import { TRONG_SO, diemChu, downloadCsv, ketQuaCua, tongKet } from '@/shared/lib'
import { Dialog, Icon, Select, Skeleton, SkeletonRows } from '@/shared/ui'

import { TERM_NAMES } from '../data/demo'
import { parseScore, useGradeBook } from '../hooks/useGradeBook'
import type { DraftScores, GradeEntry, GradeSheetStatus, TeachingClass } from '../types'
import styles from './GradeBook.module.scss'

const SHEET_STATUS: Record<GradeSheetStatus, string> = {
  NHAP: 'Đang nhập',
  DA_CONG_BO: 'Đã công bố',
  DA_KHOA: 'Đã khoá',
}

/** Ba cột điểm thành phần, kèm trọng số để in lên đầu bảng. */
const COLUMNS: readonly { field: keyof DraftScores; label: string; weight: number }[] = [
  { field: 'diemChuyenCan', label: 'Chuyên cần', weight: TRONG_SO.chuyenCan },
  { field: 'diemGiuaKy', label: 'Giữa kỳ', weight: TRONG_SO.giuaKy },
  { field: 'diemCuoiKy', label: 'Cuối kỳ', weight: TRONG_SO.cuoiKy },
]

function fold(s: string): string {
  return s.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
}

function fmt(value: number | null): string {
  return value === null ? '—' : value.toFixed(1)
}

export interface GradeBookProps {
  /** Học kỳ mở sẵn khi vào màn. */
  maHocKy: string
  /** Hiện lối "Đặt lại dữ liệu demo" — bỏ khi đã nối API thật. */
  demo?: boolean
}

/**
 * Nhập, lưu nháp và công bố điểm một lớp (F06).
 *
 * Luồng theo đúng hợp đồng API: GV **nhập nháp** → **công bố cả lớp** → Admin
 * cơ sở **khoá**. Màn này không có nút khoá — khoá là quyền của Admin.
 * Điểm tổng kết do **server** tính; cột tổng kết ở đây chỉ là xem trước.
 */
export function GradeBook({ maHocKy: initialTerm, demo = false }: GradeBookProps) {
  const [maHocKy, setMaHocKy] = useState(initialTerm)
  const [query, setQuery] = useState('')
  const [confirmPublish, setConfirmPublish] = useState(false)
  /** Lớp người dùng định mở khi đang còn sửa dở — hỏi trước khi bỏ. */
  const [pendingClass, setPendingClass] = useState<string | null>(null)
  const book = useGradeBook(maHocKy)
  const { sheet, drafts, changed, invalid, busy } = book

  const lop = book.classes?.find((c) => c.maLopHP === book.maLopHP) ?? null
  const readOnly = sheet !== null && sheet.trangThai === 'DA_KHOA'
  const dirty = changed.length > 0

  const visible = useMemo(() => {
    const q = fold(query.trim())
    return (sheet?.diem ?? []).filter((row) => !q || fold(`${row.maSinhVien} ${row.hoTen}`).includes(q))
  }, [sheet, query])

  /** Đủ ba điểm thành phần thì mới công bố được — đếm trên bảng đã lưu. */
  const daDuDiem = (sheet?.diem ?? []).filter((row) => row.diemTongKet !== null).length
  const daCongBo = (sheet?.diem ?? []).filter((row) => row.ngayCongBo !== null).length
  const tongSo = sheet?.diem.length ?? 0

  function chonLop(next: string) {
    if (next === book.maLopHP) return
    if (dirty) setPendingClass(next)
    else book.setMaLopHP(next)
  }

  function chonHocKy(next: string) {
    if (next === maHocKy) return
    // Đổi học kỳ là đổi cả danh sách lớp, nên cũng phải hỏi khi đang sửa dở.
    if (dirty && !window.confirm('Bỏ các thay đổi chưa lưu và chuyển học kỳ?')) return
    setMaHocKy(next)
  }

  /** Enter hoặc ↓/↑ nhảy sang cùng cột của dòng kế — nhập cả lớp bằng bàn phím. */
  function onCellKeyDown(event: KeyboardEvent<HTMLInputElement>, field: keyof DraftScores, index: number) {
    const step = event.key === 'Enter' || event.key === 'ArrowDown' ? 1 : event.key === 'ArrowUp' ? -1 : 0
    if (step === 0) return
    const next = visible[index + step]
    if (!next) return
    event.preventDefault()
    document.getElementById(cellId(next.maSinhVien, field))?.focus()
  }

  function exportCsv() {
    if (!sheet) return
    downloadCsv(
      [
        ['STT', 'Mã SV', 'Họ tên', 'Chuyên cần', 'Giữa kỳ', 'Cuối kỳ', 'Tổng kết', 'Điểm chữ', 'Kết quả', 'Trạng thái'],
        ...sheet.diem.map((row, i) => [
          i + 1,
          row.maSinhVien,
          row.hoTen,
          row.diemChuyenCan,
          row.diemGiuaKy,
          row.diemCuoiKy,
          row.diemTongKet,
          row.diemTongKet === null ? null : diemChu(row.diemTongKet),
          row.ketQua === 'DAT' ? 'Đạt' : row.ketQua === 'KHONG_DAT' ? 'Không đạt' : null,
          row.ngayCongBo ? 'Đã công bố' : 'Nháp',
        ]),
      ],
      `bang-diem-${sheet.lop.maLopHP}.csv`,
    )
  }

  /* Chưa có danh sách lớp thì chưa dựng được ô chọn lớp, nên dựng khung xương
     theo đúng bố cục sắp hiện: thanh công cụ · đầu bảng · bảng điểm. */
  if (!book.classes) {
    return (
      <div className={styles.block} aria-busy="true">
        <div className={styles.toolbar}>
          <Skeleton width="230px" height="34px" radius="6px" />
          <Skeleton width="330px" height="34px" radius="6px" />
          <Skeleton height="34px" radius="6px" />
        </div>
        <Skeleton height="66px" radius="6px" />
        <div className={styles.scroll}>
          <table className={styles.table}>
            <tbody>
              <SkeletonRows cols={10} rows={6} />
            </tbody>
          </table>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.block}>
      <div className={styles.toolbar} data-print="hide">
        <Select
          ariaLabel="Học kỳ"
          className={styles.termSelect}
          value={maHocKy}
          options={Object.entries(TERM_NAMES).map(([value, label]) => ({ value, label }))}
          onChange={chonHocKy}
        />
        {book.classes.length > 0 ? (
          <Select
            ariaLabel="Lớp học phần"
            className={styles.classSelect}
            value={book.maLopHP ?? ''}
            options={book.classes.map((c) => ({ value: c.maLopHP, label: nhanLop(c) }))}
            onChange={chonLop}
          />
        ) : null}
        <label className={styles.search}>
          <Icon name="search" size="14px" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo mã hoặc tên sinh viên…"
            aria-label="Tìm sinh viên"
          />
        </label>
      </div>

      {book.classes.length === 0 ? (
        <p className={styles.empty}>Học kỳ này bạn chưa được phân công lớp nào.</p>
      ) : null}

      {/* Lần tải đầu của một lớp: `sheet` còn `null` nên khối bên dưới chưa
          dựng được. Không có nhánh này thì dưới thanh công cụ trống trơn,
          người dùng không biết là đang tải hay lớp rỗng. */}
      {lop && !sheet ? (
        <>
          <Skeleton height="66px" radius="6px" />
          <div className={styles.scroll} aria-busy="true">
            <table className={styles.table}>
              <tbody>
                <SkeletonRows cols={10} rows={8} />
              </tbody>
            </table>
          </div>
        </>
      ) : null}

      {lop && sheet ? (
        <>
          <ClassHeader lop={lop} trangThai={sheet.trangThai} daDuDiem={daDuDiem} daCongBo={daCongBo} tong={tongSo} />

          {book.notice ? (
            <p
              className={`${styles.notice} ${styles[book.notice.tone]}`}
              role={book.notice.tone === 'error' ? 'alert' : 'status'}
            >
              {book.notice.text}
              <button type="button" onClick={() => book.setNotice(null)} aria-label="Đóng thông báo">
                ×
              </button>
            </p>
          ) : null}

          {readOnly ? (
            <p className={styles.hintBox}>
              Lớp đã <b>khoá điểm</b> nên bảng này chỉ xem. Khoá là thao tác của quản trị đào tạo cơ sở và
              không có đường mở lại.
            </p>
          ) : (
            <p className={styles.hintBox}>
              Điểm thang <b>0–10</b>, tối đa 1 chữ số thập phân. Ô để trống là <b>chưa có điểm</b> — không
              phải 0 điểm. Tổng kết do hệ thống tính ({ty(TRONG_SO.chuyenCan)}·CC + {ty(TRONG_SO.giuaKy)}·GK
              + {ty(TRONG_SO.cuoiKy)}·CK), cột bên dưới chỉ là xem trước cho tới khi bấm Lưu.
            </p>
          )}

          <div className={styles.scroll} aria-busy={book.loadingSheet}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th scope="col">STT</th>
                  <th scope="col">Mã SV</th>
                  <th scope="col" className={styles.left}>
                    Họ và tên
                  </th>
                  {COLUMNS.map((col) => (
                    <th scope="col" key={col.field}>
                      {col.label}
                      <small className={styles.sub}>{ty(col.weight)}</small>
                    </th>
                  ))}
                  <th scope="col">Tổng kết</th>
                  <th scope="col">Điểm chữ</th>
                  <th scope="col">Kết quả</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {book.loadingSheet ? (
                  <SkeletonRows cols={10} rows={8} />
                ) : visible.length === 0 ? (
                  <tr>
                    <td colSpan={10} className={styles.empty}>
                      {tongSo === 0 ? 'Lớp chưa có sinh viên nào.' : 'Không tìm thấy sinh viên'}
                    </td>
                  </tr>
                ) : (
                  visible.map((row, index) => (
                    <Row
                      key={row.maSinhVien}
                      row={row}
                      stt={index + 1}
                      draft={drafts[row.maSinhVien]}
                      readOnly={readOnly || busy}
                      invalid={invalid.has(row.maSinhVien)}
                      onEdit={book.edit}
                      onKeyDown={(event, field) => onCellKeyDown(event, field, index)}
                    />
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className={styles.actions} data-print="hide">
            <button type="button" className={styles.ghost} onClick={exportCsv} disabled={tongSo === 0}>
              <Icon name="download" size="13px" /> Xuất Excel
            </button>
            <button type="button" className={styles.ghost} onClick={() => window.print()}>
              <Icon name="printer" size="13px" /> In bảng điểm
            </button>
            {demo ? (
              <button type="button" className={styles.linkBtn} onClick={() => void book.resetDemo()}>
                Đặt lại dữ liệu demo
              </button>
            ) : null}
            <span className={styles.spacer} />
            {readOnly ? null : (
              <>
                <button
                  type="button"
                  className={styles.ghost}
                  onClick={book.discard}
                  disabled={!dirty || busy}
                >
                  Huỷ sửa
                </button>
                <button
                  type="button"
                  className={styles.primary}
                  onClick={() => void book.saveDraft()}
                  disabled={!dirty || busy || invalid.size > 0}
                >
                  {busy ? 'Đang lưu…' : dirty ? `Lưu ${changed.length} dòng` : 'Lưu nháp'}
                </button>
                <button
                  type="button"
                  className={styles.publish}
                  onClick={() => setConfirmPublish(true)}
                  disabled={busy || tongSo === 0}
                >
                  Công bố điểm
                </button>
              </>
            )}
          </div>

          {invalid.size > 0 ? (
            <p className={styles.warn} role="alert">
              Có {invalid.size} dòng điểm không hợp lệ (ngoài 0–10 hoặc quá 1 chữ số thập phân). Sửa xong
              mới lưu được.
            </p>
          ) : null}
        </>
      ) : null}

      <Dialog
        open={confirmPublish}
        onClose={() => setConfirmPublish(false)}
        ariaLabel={sheet ? `Công bố điểm lớp ${sheet.lop.maLopHP}` : 'Công bố điểm'}
        header={
          <p className={styles.dialogTitle}>
            Công bố điểm {sheet?.lop.maLopHP}
            <small>{sheet?.lop.tenMonHoc}</small>
          </p>
        }
        footer={
          <>
            <button
              type="button"
              className={styles.ghost}
              onClick={() => setConfirmPublish(false)}
              disabled={busy}
            >
              Chưa công bố
            </button>
            <button
              type="button"
              className={styles.publish}
              onClick={() => {
                setConfirmPublish(false)
                void book.publish()
              }}
              disabled={busy}
            >
              Công bố
            </button>
          </>
        }
      >
        <div className={styles.form}>
          {dirty ? (
            <p className={styles.warn}>
              Còn <b>{changed.length} dòng chưa lưu</b>. Công bố chỉ áp dụng cho điểm đã lưu — lưu trước
              rồi hãy công bố.
            </p>
          ) : null}
          <p>
            Công bố sẽ gửi điểm tới <b>{tongSo - daCongBo} sinh viên</b> chưa nhận kết quả của lớp này; mỗi
            người nhận một thông báo và thấy điểm ngay trong bảng điểm.
          </p>
          <p className={styles.hintBox}>
            Còn sinh viên thiếu điểm thành phần thì hệ thống <b>từ chối</b> công bố cả lớp (hiện đủ{' '}
            {daDuDiem}/{tongSo}). Công bố rồi <b>vẫn sửa được</b>; sinh viên sẽ nhận thông báo điểm đã
            thay đổi.
          </p>
        </div>
      </Dialog>

      <Dialog
        open={pendingClass !== null}
        onClose={() => setPendingClass(null)}
        ariaLabel="Bỏ thay đổi chưa lưu"
        header={
          <p className={styles.dialogTitle}>
            Còn {changed.length} dòng chưa lưu
            <small>Chuyển sang lớp khác sẽ bỏ các thay đổi này</small>
          </p>
        }
        footer={
          <>
            <button type="button" className={styles.ghost} onClick={() => setPendingClass(null)}>
              Ở lại nhập tiếp
            </button>
            <button
              type="button"
              className={styles.primary}
              onClick={() => {
                const next = pendingClass
                setPendingClass(null)
                if (next) book.setMaLopHP(next)
              }}
            >
              Bỏ thay đổi và chuyển lớp
            </button>
          </>
        }
      >
        <p>Điểm vừa gõ chưa được gửi lên hệ thống. Lưu nháp trước nếu muốn giữ lại.</p>
      </Dialog>
    </div>
  )
}

/* --- Đầu bảng: thông tin lớp và tiến độ ---------------------------------- */

function ClassHeader(props: {
  lop: TeachingClass
  trangThai: GradeSheetStatus
  daDuDiem: number
  daCongBo: number
  tong: number
}) {
  const { lop, trangThai, daDuDiem, daCongBo, tong } = props
  return (
    <div className={styles.classHead}>
      <div className={styles.classTitle}>
        <b>{lop.tenMonHoc}</b>
        <small>
          {lop.maLopHP} · {lop.soTinChi} TC · sĩ số {lop.soLuongDaDangKy}/{lop.soLuongToiDa}
        </small>
      </div>
      <div className={styles.progress}>
        <span className={styles.stat}>
          Đủ điểm <b>{daDuDiem}</b>/{tong}
        </span>
        <span className={styles.stat}>
          Đã công bố <b>{daCongBo}</b>/{tong}
        </span>
        <span className={`${styles.badge} ${styles[trangThai]}`}>{SHEET_STATUS[trangThai]}</span>
      </div>
    </div>
  )
}

/* --- Một dòng sinh viên --------------------------------------------------- */

function Row(props: {
  row: GradeEntry
  stt: number
  draft: DraftScores | undefined
  readOnly: boolean
  invalid: boolean
  onEdit: (maSinhVien: string, field: keyof DraftScores, value: string) => void
  onKeyDown: (event: KeyboardEvent<HTMLInputElement>, field: keyof DraftScores) => void
}) {
  const { row, draft, readOnly, invalid } = props
  // Xem trước theo đúng công thức server, tính trên những gì đang gõ.
  const preview = draft
    ? xemTruoc(draft)
    : { diemTongKet: row.diemTongKet, ketQua: row.ketQua }

  return (
    <tr className={invalid ? styles.rowInvalid : undefined}>
      <td className={styles.center}>{props.stt}</td>
      <td className={`${styles.center} ${styles.code}`}>{row.maSinhVien}</td>
      <td>{row.hoTen}</td>
      {COLUMNS.map((col) => (
        <td key={col.field} className={styles.center}>
          {readOnly ? (
            fmt(row[col.field])
          ) : (
            <input
              id={cellId(row.maSinhVien, col.field)}
              className={styles.score}
              /* `text` + `inputMode` thay cho `type="number"`: bàn phím tiếng
                 Việt hay cho ra dấu phẩy, mà ô number sẽ nuốt mất giá trị đó
                 không báo gì. Ở đây nhận cả `8,5` và báo lỗi nếu sai. */
              type="text"
              inputMode="decimal"
              autoComplete="off"
              value={draft?.[col.field] ?? ''}
              aria-label={`${col.label} của ${row.hoTen}`}
              onChange={(e) => props.onEdit(row.maSinhVien, col.field, e.target.value)}
              onKeyDown={(e) => props.onKeyDown(e, col.field)}
            />
          )}
        </td>
      ))}
      <td className={`${styles.center} ${styles.total}`}>{fmt(preview.diemTongKet)}</td>
      <td className={styles.center}>
        {preview.diemTongKet === null ? '—' : diemChu(preview.diemTongKet)}
      </td>
      <td className={styles.center}>
        {preview.ketQua === null ? (
          <span className={styles.muted}>Chưa có điểm</span>
        ) : (
          <span className={`${styles.badge} ${styles[preview.ketQua]}`}>
            {preview.ketQua === 'DAT' ? 'Đạt' : 'Không đạt'}
          </span>
        )}
      </td>
      <td className={styles.center}>
        {row.ngayCongBo ? (
          <span className={`${styles.badge} ${styles.DA_CONG_BO}`}>Đã công bố</span>
        ) : (
          <span className={`${styles.badge} ${styles.NHAP}`}>Nháp</span>
        )}
      </td>
    </tr>
  )
}

/* --- Phụ trợ -------------------------------------------------------------- */

function cellId(maSinhVien: string, field: keyof DraftScores): string {
  return `diem-${maSinhVien}-${field}`
}

function nhanLop(c: TeachingClass): string {
  return `${c.maLopHP} — ${c.tenMonHoc} (${c.soLuongDaDangKy} SV)`
}

/** `0.1` → `10%`. */
function ty(weight: number): string {
  return `${Math.round(weight * 100)}%`
}

/**
 * Tổng kết xem trước. Ô đang gõ dở (không phải số) coi như **chưa có điểm**,
 * để không hiện một con số sai trong lúc người dùng còn đang gõ.
 */
function xemTruoc(draft: DraftScores): { diemTongKet: number | null; ketQua: 'DAT' | 'KHONG_DAT' | null } {
  const values = [draft.diemChuyenCan, draft.diemGiuaKy, draft.diemCuoiKy].map(parseScore)
  if (values.some((v) => v === null || Number.isNaN(v))) return { diemTongKet: null, ketQua: null }
  const [cc, gk, ck] = values as [number, number, number]
  const tong = tongKet(cc, gk, ck)
  return { diemTongKet: tong, ketQua: ketQuaCua(tong) }
}
