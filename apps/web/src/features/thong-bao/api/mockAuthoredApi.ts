import type { AuthoredNotice } from './authoredTypes'
import { AUTHORED_MOCK } from '../data/authored'

/** Dữ liệu mẫu cho hộp thư gửi — đủ 3 phạm vi, lẫn nháp/đã gửi. */
export function fetchAuthored(): Promise<AuthoredNotice[]> {
  // Copy để chữ ký khớp bản http (mảng mutable); nguồn mock giữ readonly.
  return Promise.resolve([...AUTHORED_MOCK])
}

export function fetchAuthoredDetail(maThongBao: string): Promise<AuthoredNotice> {
  const found = AUTHORED_MOCK.find((n) => n.maThongBao === maThongBao)
  if (!found) throw new Error('Không có thông báo này trong dữ liệu mẫu.')
  return Promise.resolve(found)
}
