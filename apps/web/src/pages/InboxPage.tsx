import { useAuth } from '@/features/auth'
import { GvNoticeHub, Inbox } from '@/features/thong-bao'

import { StudentShell } from './StudentShell'

/**
 * Hộp thư `/thong-bao` — SV và GV dùng chung endpoint nên dùng chung màn.
 *
 * Khung thì khác: trang SV có cột phải, trang GV render trần như
 * `TeachingClassesPage`. Bọc `StudentShell` cho GV sẽ lòi menu SV (bug
 * 10/2026: GV bấm Thông báo thấy toàn link sinh viên).
 *
 * Riêng GV có thêm tab "Gửi thông báo" trong cùng chỗ này — đọc tin lớp xong
 * soạn tiếp là luồng liền mạch, TÍNH NĂNG chỉ cần một link "Thông báo".
 */
export function InboxPage() {
  const { user } = useAuth()

  if (user?.role === 'GIANG_VIEN') return <GvNoticeHub />

  return (
    <StudentShell>
      <Inbox />
    </StudentShell>
  )
}

