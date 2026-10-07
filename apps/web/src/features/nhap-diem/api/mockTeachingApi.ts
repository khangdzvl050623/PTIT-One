import { ApiError } from '@/shared/api'
import { DIEM_MAX, DIEM_MIN, ketQuaCua, tongKet } from '@/shared/lib'

import {
  CAMPUS,
  DEMO_CLASSES,
  NGAY_BAT_DAU_HOC_KY,
  NGAY_MO_DOT,
  initialSheet,
  roster,
  teachingEntriesOf,
} from '../data/demo'
import type {
  ClassRoster,
  GradeEntry,
  GradeSheet,
  GradeSheetStatus,
  RosterEntry,
  SaveGradeRow,
  TeachingClass,
  TeachingSchedule,
} from '../types'

/**
 * API GIẢ cho màn hình giảng viên — dựng UI khi chưa chạy backend. Phủ cả lớp
 * phụ trách (F05) lẫn bảng điểm (F06), vì hai màn dùng chung danh sách lớp.
 *
 * Phần bảng điểm kiểm theo đúng thứ tự `GradeBookService` và ném **cùng mã
 * lỗi, cùng câu báo**, để màn hình xử lý lỗi y như khi nối API thật:
 *
 *   lớp phải `MO` → không trùng sinh viên trong một lần lưu → đúng `version`
 *   → sinh viên có trong lớp
 *
 * Trạng thái lưu localStorage để F5 không mất; `resetDemo()` trả về ban đầu.
 *
 * Nối API thật:
 * - F05: `GET /api/me/teaching-classes`, `GET /api/classes/{maLopHP}/students`,
 *   `GET /api/me/teaching-schedule?maHocKy=&tuan=`
 * - F06: `GET|PUT /api/classes/{maLopHP}/grades`, `POST .../grades/publish`
 *
 * Khoá điểm (`POST .../grades/lock`) là việc của Admin cơ sở, không ở đây.
 */

const STORAGE_KEY = 'ptitone:mock:nhap-diem'
const LATENCY_MS = 350

interface State {
  /** Bảng điểm theo mã lớp. */
  diem: Record<string, GradeEntry[]>
  /** Trạng thái lớp đã đổi so với dữ liệu gốc (chỉ Admin khoá được). */
  trangThaiLop: Record<string, string>
}

function initial(): State {
  const diem: Record<string, GradeEntry[]> = {}
  for (const lop of DEMO_CLASSES) diem[lop.maLopHP] = initialSheet(lop)
  return { diem, trangThaiLop: {} }
}

/* localStorage có thể ném lỗi ở chế độ riêng tư — khi đó mỗi lần F5 là một phiên mới. */
function load(): State {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as State) : initial()
  } catch {
    return initial()
  }
}

function save(state: State): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {
    /* Bỏ qua: xem ghi chú ở load. */
  }
}

export function resetDemo(): void {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* Bỏ qua. */
  }
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}

/** Lớp kèm trạng thái hiện tại (admin có thể đã khoá điểm). */
function classOf(state: State, maLopHP: string): TeachingClass | undefined {
  const lop = DEMO_CLASSES.find((c) => c.maLopHP === maLopHP)
  if (!lop) return undefined
  const trangThai = state.trangThaiLop[maLopHP]
  return trangThai ? { ...lop, trangThai } : lop
}

function requireClass(state: State, maLopHP: string): TeachingClass {
  const lop = classOf(state, maLopHP)
  if (!lop) {
    throw new ApiError(404, { code: 'CLASS_NOT_FOUND', message: `Không tìm thấy lớp ${maLopHP}.` })
  }
  return lop
}

/** `GradeBookService.requireOpen` — chỉ lớp `MO` mới nhập được điểm. */
function requireOpen(lop: TeachingClass): void {
  if (lop.trangThai === 'MO') return
  if (lop.trangThai === 'DA_KHOA') {
    throw new ApiError(409, {
      code: 'GRADE_LOCKED',
      message: `Lớp ${lop.maLopHP} đã khoá điểm, không sửa được.`,
    })
  }
  throw new ApiError(409, {
    code: 'GRADE_CLASS_NOT_OPEN',
    message: `Lớp ${lop.maLopHP} đang ở trạng thái ${lop.trangThai}, không nhập điểm được.`,
  })
}

/**
 * Dựng `GradeSheet` như `GradeBookService.sheetOf`: tổng kết và kết quả tính
 * tại đây (server tính, không phải client gửi lên), trạng thái suy ra từ lớp
 * và từ việc mọi dòng đã công bố hay chưa.
 */
function sheetOf(state: State, lop: TeachingClass): GradeSheet {
  const rows = state.diem[lop.maLopHP] ?? []
  const diem = rows.map((row) => {
    const tk = tongKet(row.diemChuyenCan, row.diemGiuaKy, row.diemCuoiKy)
    return { ...row, diemTongKet: tk, ketQua: ketQuaCua(tk) }
  })

  let trangThai: GradeSheetStatus
  if (lop.trangThai === 'DA_KHOA') trangThai = 'DA_KHOA'
  else if (rows.length > 0 && rows.every((row) => row.ngayCongBo !== null)) trangThai = 'DA_CONG_BO'
  else trangThai = 'NHAP'

  return { lop, trangThai, diem }
}

/** `GET /api/me/teaching-classes` — chỉ lớp GV **đang** được phân công. */
export async function teachingClasses(maHocKy?: string): Promise<TeachingClass[]> {
  await delay()
  const state = load()
  return DEMO_CLASSES.filter((c) => !maHocKy || c.maHocKy === maHocKy).map(
    (c) => classOf(state, c.maLopHP) ?? c,
  )
}

/**
 * `GET /api/classes/{maLopHP}/students` (F05).
 *
 * Trả **cả** bộ đếm `lop.soLuongDaDangKy` lẫn danh sách để đối soát: hai số
 * lệch nhau là lỗi dữ liệu, UI chỉ báo chứ không tự sửa.
 */
export async function classRoster(maLopHP: string): Promise<ClassRoster> {
  await delay()
  const state = load()
  const lop = requireClass(state, maLopHP)
  // Cùng nguồn sinh viên với bảng điểm, nên hai màn luôn khớp nhau.
  const sinhVien: RosterEntry[] = roster(lop.maLopHP, lop.soLuongDaDangKy).map((sv, i) => ({
    ...sv,
    maCoSoNha: CAMPUS,
    ngayDangKy: new Date(Date.parse(NGAY_MO_DOT) + (i + 1) * 37 * 60_000).toISOString(),
    trangThai: 'DA_DANG_KY',
  }))
  return { lop, sinhVien }
}

/**
 * `GET /api/me/teaching-schedule?maHocKy=` (bỏ `tuan` nên trả cả học kỳ).
 * Cùng hình dạng với thời khoá biểu sinh viên.
 */
export async function teachingSchedule(maHocKy: string): Promise<TeachingSchedule> {
  await delay()
  return {
    maHocKy,
    ngayBatDau: NGAY_BAT_DAU_HOC_KY[maHocKy] ?? '',
    tuan: null,
    buoiHoc: teachingEntriesOf(maHocKy),
  }
}

/** `GET /api/classes/{maLopHP}/grades`. */
export async function sheet(maLopHP: string): Promise<GradeSheet> {
  await delay()
  const state = load()
  return sheetOf(state, requireClass(state, maLopHP))
}

/**
 * `PUT /api/classes/{maLopHP}/grades` — lưu cả loạt trong MỘT giao dịch: một
 * dòng lỗi thì không dòng nào được ghi. Vì vậy ở đây kiểm **toàn bộ** trước,
 * ghi sau.
 */
export async function saveGrades(maLopHP: string, rows: readonly SaveGradeRow[]): Promise<GradeSheet> {
  await delay()
  const state = load()
  const lop = requireClass(state, maLopHP)
  requireOpen(lop)

  if (rows.length === 0) {
    throw new ApiError(400, { code: 'VALIDATION_ERROR', message: 'Chưa có dòng điểm nào để lưu.' })
  }

  const seen = new Set<string>()
  for (const row of rows) {
    if (seen.has(row.maSinhVien)) {
      throw new ApiError(400, {
        code: 'VALIDATION_ERROR',
        message: `Sinh viên ${row.maSinhVien} xuất hiện hai lần trong cùng một lần lưu.`,
      })
    }
    seen.add(row.maSinhVien)
    for (const score of [row.diemChuyenCan, row.diemGiuaKy, row.diemCuoiKy]) {
      if (score !== null && !hopLe(score)) {
        throw new ApiError(400, {
          code: 'VALIDATION_ERROR',
          message: `Điểm của ${row.maSinhVien} phải trong khoảng ${DIEM_MIN}–${DIEM_MAX} và tối đa 1 chữ số thập phân.`,
        })
      }
    }
  }

  const current = state.diem[maLopHP] ?? []
  const byId = new Map(current.map((row) => [row.maSinhVien, row]))
  for (const row of rows) {
    const cu = byId.get(row.maSinhVien)
    if (!cu) {
      throw new ApiError(400, {
        code: 'GRADE_STUDENT_NOT_ENROLLED',
        message: `Sinh viên ${row.maSinhVien} không có trong lớp ${maLopHP}.`,
      })
    }
    if (cu.version !== row.version) {
      throw new ApiError(409, {
        code: 'GRADE_VERSION_CONFLICT',
        message: `Điểm của ${row.maSinhVien} vừa được người khác sửa. Tải lại bảng điểm rồi nhập lại.`,
      })
    }
  }

  // Qua hết vòng kiểm mới ghi — mô phỏng tính nguyên tử của giao dịch.
  const saved = new Map(rows.map((row) => [row.maSinhVien, row]))
  state.diem[maLopHP] = current.map((row) => {
    const moi = saved.get(row.maSinhVien)
    if (!moi) return row
    return {
      ...row,
      diemChuyenCan: moi.diemChuyenCan,
      diemGiuaKy: moi.diemGiuaKy,
      diemCuoiKy: moi.diemCuoiKy,
      version: row.version + 1,
    }
  })
  save(state)
  return sheetOf(state, lop)
}

/**
 * `POST /api/classes/{maLopHP}/grades/publish` — công bố mọi dòng còn nháp.
 * Chặn khi còn sinh viên thiếu điểm thành phần: công bố bảng dở dang là công
 * bố "chưa có điểm" như kết quả.
 */
export async function publishGrades(maLopHP: string): Promise<GradeSheet> {
  await delay()
  const state = load()
  const lop = requireClass(state, maLopHP)
  requireOpen(lop)

  const rows = state.diem[maLopHP] ?? []
  const thieu = rows
    .filter((row) => tongKet(row.diemChuyenCan, row.diemGiuaKy, row.diemCuoiKy) === null)
    .map((row) => row.maSinhVien)
  if (thieu.length > 0) {
    throw new ApiError(409, {
      code: 'GRADE_INCOMPLETE',
      message: `Còn ${thieu.length} sinh viên chưa đủ điểm thành phần: ${thieu.join(', ')}.`,
    })
  }

  // Thời điểm công bố ĐẦU được giữ: công bố lại không dời mốc của dòng cũ.
  const now = new Date().toISOString()
  state.diem[maLopHP] = rows.map((row) => (row.ngayCongBo ? row : { ...row, ngayCongBo: now }))
  save(state)
  return sheetOf(state, lop)
}

/** 0–10, tối đa 1 chữ số thập phân — trùng ràng buộc `SaveGradesRequest.Row`. */
function hopLe(score: number): boolean {
  if (!Number.isFinite(score) || score < DIEM_MIN || score > DIEM_MAX) return false
  return Math.round(score * 10) === score * 10
}

/**
 * Điểm ĐÃ CÔNG BỐ của một sinh viên ở một lớp, hoặc `null`.
 *
 * Bản giả của bảng điểm sinh viên dùng hàm này để thấy việc giảng viên vừa
 * công bố. Không có nó thì hai bản giả là hai thế giới rời nhau: giảng viên
 * nhập điểm xong mà màn sinh viên không đổi gì.
 *
 * Chỉ trả dòng đã công bố — giống server: điểm nháp không lộ cho sinh viên.
 */
export function publishedGradeOf(maLopHP: string, maSinhVien: string): GradeEntry | null {
  const row = load().diem[maLopHP]?.find((e) => e.maSinhVien === maSinhVien)
  return row && row.ngayCongBo !== null ? row : null
}
