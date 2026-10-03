import { DEMO_TERMS, demoTimetableOf } from '../data/timetable'
import { entriesInWeek } from '../lib/week'
import type { Term, Timetable } from '../types'

/** Bản giả; cùng chữ ký với `httpTimetableApi`. */

const LATENCY_MS = 300

export async function fetchTerms(): Promise<readonly Term[]> {
  await delay()
  return DEMO_TERMS
}

export async function fetchTimetable(maHocKy: string, tuan?: number): Promise<Timetable> {
  await delay()
  const term = DEMO_TERMS.find((t) => t.maHocKy === maHocKy) ?? DEMO_TERMS[0]
  const all = demoTimetableOf(maHocKy)
  return {
    maHocKy,
    ngayBatDau: term?.ngayBatDau ?? '',
    tuan: tuan ?? null,
    // Lọc đúng như server làm, để đổi chế độ không đổi kết quả.
    buoiHoc: tuan === undefined ? all : entriesInWeek(all, tuan),
  }
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}
