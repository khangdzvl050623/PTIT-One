import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'

import { useAsyncData } from '@/shared/lib'
import { Panel } from '@/shared/ui'

import { fetchInbox, markAllRead, markRead } from '../api/inboxApi'
import type { InboxItem } from '../api/inboxTypes'

import { formatNgay, NoticeDetailDialog } from './NoticeDetailDialog'
import dialogStyles from './NoticeDetailDialog.module.scss'
import { Pills } from './Pills'

import styles from './Inbox.module.scss'

type Filter = 'TAT_CA' | 'CHUA_DOC'

/**
 * Hộp thư của sinh viên (giảng viên dùng chung endpoint nên dùng chung màn).
 *
 * Mở hộp thư không tự đánh dấu đã đọc — chỉ khi mở dialog chi tiết (`read`)
 * hoặc bấm đọc tất cả (`read-all`). Đọc lại là no-op ở server nên bấm lại
 * dialog không sinh thêm gì.
 */
export function Inbox() {
  // Một lần tải một trang (tối đa 50 theo hợp đồng) rồi lọc client-side — hộp
  // thư demo và thật đều nhỏ hơn ngưỡng này rất xa.
  const load = useCallback(() => fetchInbox(), [])
  const { data, loading, error } = useAsyncData(load)
  const [filter, setFilter] = useState<Filter>('TAT_CA')
  const [selected, setSelected] = useState<InboxItem | null>(null)
  /* Mã đã đọc trong phiên này mà server chưa kịp trả về ở lần tải đầu — cộng
     với `daDoc` của server để chấm đỏ tắt ngay khi mở dialog. */
  const [extraRead, setExtraRead] = useState<readonly string[]>([])
  const [unreadOverride, setUnreadOverride] = useState<number | null>(null)

  const items = data?.thongBao ?? []
  const read = (n: InboxItem) => n.daDoc || extraRead.includes(n.maThongBao)
  const unread = unreadOverride ?? data?.soChuaDoc ?? 0
  const visible = filter === 'TAT_CA' ? items : items.filter((n) => !read(n))

  function openDetail(n: InboxItem) {
    setSelected(n)
    if (read(n)) return
    /* Lạc quan: tắt chấm trước, server xác nhận sau. Hỏng thì trả chấm về. */
    setExtraRead((ids) => [...ids, n.maThongBao])
    void markRead(n.maThongBao).then(
      (count) => setUnreadOverride(count.soChuaDoc),
      () => setExtraRead((ids) => ids.filter((id) => id !== n.maThongBao)),
    )
  }

  function readAll() {
    void markAllRead().then(
      (count) => {
        setExtraRead(items.map((n) => n.maThongBao))
        setUnreadOverride(count.soChuaDoc)
      },
      () => undefined,
    )
  }

  return (
    <Panel title="THÔNG BÁO" icon="bell">
      <div className={styles.toolbar}>
        <Pills
          options={[
            { value: 'TAT_CA' as const, label: 'Tất cả', count: items.length },
            { value: 'CHUA_DOC' as const, label: 'Chưa đọc', count: unread },
          ]}
          value={filter}
          onChange={setFilter}
          ariaLabel="Lọc theo trạng thái đọc"
        />
        {unread > 0 ? (
          <button type="button" className={styles.readAll} onClick={readAll}>
            Đánh dấu đã đọc tất cả
          </button>
        ) : null}
      </div>

      {loading ? <p className={styles.state}>Đang tải thông báo…</p> : null}
      {error ? (
        <p className={styles.state} role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error && visible.length === 0 ? (
        <p className={styles.state}>
          {items.length === 0 ? 'Chưa có thông báo nào.' : 'Không còn thông báo chưa đọc.'}
        </p>
      ) : null}

      {visible.length > 0 ? (
        <ul className={styles.list}>
          {visible.map((n) => (
            <li key={n.maThongBao} className={styles.row}>
              <button type="button" className={styles.main} onClick={() => openDetail(n)}>
                <span className={styles.titleRow}>
                  <span className={styles.title}>{n.tieuDe}</span>
                  {n.mucDo === 'QUAN_TRONG' ? <span className={styles.urgent}>Quan trọng</span> : null}
                </span>
                <time className={styles.date}>{formatNgay(n.ngayGui)}</time>
              </button>
              {!read(n) ? <span className={styles.dot} aria-label="Chưa đọc" /> : null}
            </li>
          ))}
        </ul>
      ) : null}

      {selected ? <InboxDialog item={selected} onClose={() => setSelected(null)} /> : null}
    </Panel>
  )
}

/** Dialog chi tiết: Ngày gửi / Tiêu đề / Nội dung (+ liên kết). Không có phản hồi. */
function InboxDialog({ item, onClose }: { item: InboxItem; onClose: () => void }) {
  return (
    <NoticeDetailDialog
      title="NỘI DUNG THÔNG BÁO"
      onClose={onClose}
      rows={[
        {label: 'Ngày gửi', content: formatNgay(item.ngayGui) },
        { label: 'Tiêu đề', content: <span className={dialogStyles.strong}>{item.tieuDe}</span> },
        { label: 'Nội dung', content: <span className={dialogStyles.body}>{item.noiDung}</span> },
        ...(item.lienKet
          ? [
              {
                label: 'Liên kết',
                content: (
                  <Link className={dialogStyles.innerLink} to={item.lienKet} onClick={onClose}>
                    Mở trang liên quan
                  </Link>
                ),
              },
            ]
          : []),
      ]}
    />
  )
}
