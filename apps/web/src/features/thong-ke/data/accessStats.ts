import type { IconName } from '@/shared/ui'

export interface AccessStat {
  id: string
  label: string
  value: number
  icon: IconName
}

/** Số liệu mẫu — sẽ thay bằng API thống kê truy cập khi backend sẵn sàng. */
export const ACCESS_STATS: readonly AccessStat[] = [
  { id: 'live', label: 'Đang truy cập', value: 287, icon: 'users' },
  { id: 'student', label: 'SV đăng nhập', value: 280, icon: 'graduate' },
  { id: 'lecturer', label: 'GV đăng nhập', value: 7, icon: 'chalkboard' },
]
