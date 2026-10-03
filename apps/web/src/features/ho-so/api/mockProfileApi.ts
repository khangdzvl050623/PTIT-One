import { DEMO_PROFILE, DEMO_RESULTS, DEMO_SUMMARY } from '../data/profile'
import type { StudentProfile, StudentSummary, TermResults } from '../types'

/**
 * Bản giả để dựng UI khi chưa chạy backend. Cùng chữ ký với `httpProfileApi`
 * nên màn hình không phân biệt được hai bản.
 */

/** Đủ lâu để thấy trạng thái "Đang tải…" khi thiết kế. */
const LATENCY_MS = 300

export async function fetchProfile(): Promise<StudentProfile> {
  await delay()
  return DEMO_PROFILE
}

export async function fetchSummary(): Promise<StudentSummary> {
  await delay()
  return DEMO_SUMMARY
}

export async function fetchResults(): Promise<readonly TermResults[]> {
  await delay()
  return DEMO_RESULTS
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}
