import { DEMO_GRADES } from '../data/grades'
import { summarize } from '../lib/grading'
import type { StudentGrade, TermSummary } from '../types'

/** Bản giả; cùng chữ ký với `httpGradesApi`. */

const LATENCY_MS = 300

export async function fetchGrades(): Promise<readonly StudentGrade[]> {
  await delay()
  return DEMO_GRADES
}

/* Bản giả phải tự tính vì không có backend — dùng `lib/grading.ts`, nơi giữ
   bản sao quy tắc cho chế độ mock. */
export async function fetchTranscript(): Promise<readonly TermSummary[]> {
  await delay()
  return summarize(DEMO_GRADES)
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}
