import { ApiError } from '@/shared/api'

import { readMockCatalog } from './mockCatalogStore'
import { isOpen } from '../lib/period'
import {
  BEST_RESULTS,
  DEMO_CLASSES,
  DEMO_ENROLLED,
  DEMO_MA_HOC_KY,
  DEMO_NGAY_BAT_DAU,
  DEMO_PERIODS,
  DEMO_PROGRAM,
  DEMO_TEN_HOC_KY,
  TEACHERS,
  TRAN_TIN_CHI,
} from '../data/demo'
import type {
  BestResults,
  CancelledClass,
  ClassOffer,
  CreateClassInput,
  ClassRoster,
  CourseRelation,
  CurrentTerm,
  CourseSummary,
  EnrolledCourse,
  EnrollmentPeriod,
  RegistrationResult,
  RosterEntry,
  SavePeriodInput,
  ScheduleSlot,
  SlotInput,
  StudentEnrollments,
  StudentProgram,
  TeacherOption,
  UpdateClassInput,
} from '../types'

/**
 * Đăng ký học phần GIẢ — dựng UI khi chưa chạy backend. Kiểm tra theo ĐÚNG
 * thứ tự `EnrollmentService.register` và ném cùng mã lỗi + câu báo, để màn
 * hình xử lý lỗi y như khi nối API thật:
 *
 *   đợt mở → lớp MO → thuộc CTĐT → trùng môn → tiên quyết → trùng lịch → trần TC → còn chỗ
 *
 * Trạng thái lưu localStorage để F5 không mất; `resetDemo()` trả về ban đầu.
 * Nối API thật: `GET /api/classes` (+ `/schedule`), `GET|POST|DELETE /api/me/enrollments`.
 */

const STORAGE_KEY = 'ptitone:mock:dang-ky'
const LATENCY_MS = 350

/**
 * Một "database" giả dùng chung cho màn sinh viên và màn quản trị: admin đóng
 * đợt hay huỷ lớp thì màn đăng ký của sinh viên thấy ngay.
 */
interface State {
  /** Sĩ số theo lớp — bộ đếm `SoLuongDaDangKy`. */
  siSo: Record<string, number>
  dangKy: EnrolledCourse[]
  /** Đợt đăng ký của cơ sở (admin sửa được). */
  periods: EnrollmentPeriod[]
  /** Trạng thái lớp đã đổi so với dữ liệu gốc (huỷ lớp → `DA_HUY`). */
  trangThaiLop: Record<string, string>
  /** Lớp admin vừa mở (`POST /api/classes`). */
  lopMoi: ClassOffer[]
  /** Phần admin đã sửa trên lớp: sức chứa, trạng thái, hình thức, GV, lịch. */
  lopSua: Record<string, Partial<ClassOffer>>
}

/** Cơ sở của dữ liệu demo — API thật lấy từ JWT. */
const CAMPUS = 'HCM'
const PERIOD_STATUSES = ['CHUA_MO', 'DANG_MO', 'DA_DONG']

function initial(): State {
  return {
    siSo: Object.fromEntries(DEMO_CLASSES.map((c) => [c.maLopHP, c.soLuongDaDangKy])),
    dangKy: DEMO_ENROLLED.map((e) => ({ ...e })),
    periods: DEMO_PERIODS.map((p) => ({ ...p })),
    trangThaiLop: {},
    lopMoi: [],
    lopSua: {},
  }
}

function load(): State {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    // Gộp với bản đầu: dữ liệu lưu từ phiên bản cũ có thể thiếu trường mới.
    if (raw) return { ...initial(), ...(JSON.parse(raw) as Partial<State>) }
  } catch {
    /* Bộ nhớ bị chặn hoặc hỏng — dùng dữ liệu ban đầu. */
  }
  return initial()
}

/** Đợt áp dụng cho học kỳ demo: đợt `DANG_MO` nếu có, không thì đợt mở gần nhất. */
function activePeriod(state: State): EnrollmentPeriod {
  const ofTerm = state.periods.filter((p) => p.maHocKy === DEMO_MA_HOC_KY && p.maCoSo === CAMPUS)
  return (
    ofTerm.find((p) => p.trangThai === 'DANG_MO') ??
    [...ofTerm].sort((a, b) => b.thoiGianMo.localeCompare(a.thoiGianMo))[0] ??
    DEMO_PERIODS[0]!
  )
}

/** Lớp gốc + lớp mới mở — chưa áp phần sửa. */
function baseClasses(state: State): ClassOffer[] {
  return [...DEMO_CLASSES, ...state.lopMoi]
}

function classOf(state: State, maLopHP: string): ClassOffer | undefined {
  const lop = baseClasses(state).find((c) => c.maLopHP === maLopHP)
  if (!lop) return undefined
  const sua = state.lopSua[maLopHP] ?? {}
  return {
    ...lop,
    ...sua,
    soLuongDaDangKy: state.siSo[maLopHP] ?? lop.soLuongDaDangKy,
    // Huỷ lớp đi đường riêng và thắng mọi sửa đổi trạng thái khác.
    trangThai: state.trangThaiLop[maLopHP] ?? sua.trangThai ?? lop.trangThai,
  }
}

function classesOfTerm(state: State, maHocKy: string): ClassOffer[] {
  return baseClasses(state)
    .filter((c) => c.maHocKy === maHocKy)
    .map((c) => classOf(state, c.maLopHP)!)
}

function save(state: State): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {
    /* Không lưu được thì chỉ mất khi F5. */
  }
}

const delay = () => new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))

const conflict = (code: string, message: string) => new ApiError(409, { code, message })

function credits(state: State): number {
  return state.dangKy.reduce((sum, e) => sum + e.soTinChi, 0)
}

function snapshot(state: State, maHocKy: string): StudentEnrollments {
  return {
    maHocKy,
    soTinChiDaDangKy: credits(state),
    tranTinChi: state.dangKy.length ? TRAN_TIN_CHI : null,
    dangKy: [...state.dangKy].sort((a, b) => a.maMonHoc.localeCompare(b.maMonHoc)),
  }
}

/** Cùng thứ, chồng tiết VÀ chồng tuần — như `ScheduleSlot.trungTiet/trungTuan`. */
function clash(a: ScheduleSlot, b: ScheduleSlot): boolean {
  return (
    a.thu === b.thu &&
    a.tietBatDau <= b.tietBatDau + b.soTiet - 1 &&
    b.tietBatDau <= a.tietBatDau + a.soTiet - 1 &&
    a.tuanBatDau <= b.tuanKetThuc &&
    b.tuanBatDau <= a.tuanKetThuc
  )
}

/** Đợt của cơ sở sinh viên, mới nhất trước — `GET /api/enrollment-periods?maHocKy=`. */
export async function listPeriods(maHocKy?: string): Promise<EnrollmentPeriod[]> {
  await delay()
  return load()
    .periods.filter((p) => !maHocKy || p.maHocKy === maHocKy)
    .sort((a, b) => b.maHocKy.localeCompare(a.maHocKy) || b.maDot.localeCompare(a.maDot))
}

export async function currentPeriod(): Promise<EnrollmentPeriod> {
  await delay()
  return activePeriod(load())
}

/**
 * Tạo (`maDot` rỗng) hoặc sửa đợt — `POST /api/enrollment-periods` và
 * `PUT /api/enrollment-periods/{maDot}`. Kiểm như `EnrollmentPeriodService`:
 * trạng thái hợp lệ → mở trước đóng → mỗi cơ sở MỘT đợt `DANG_MO` mỗi học kỳ.
 */
export async function savePeriod(
  maDot: string | null,
  input: SavePeriodInput,
): Promise<EnrollmentPeriod> {
  await delay()
  const state = load()
  if (!PERIOD_STATUSES.includes(input.trangThai)) {
    throw new ApiError(400, {
      code: 'PERIOD_STATUS_INVALID',
      message: `Trạng thái đợt không hợp lệ: ${input.trangThai}.`,
    })
  }
  if (!(Date.parse(input.thoiGianMo) < Date.parse(input.thoiGianDong))) {
    throw new ApiError(400, {
      code: 'PERIOD_WINDOW_INVALID',
      message: 'Thời gian mở phải trước thời gian đóng.',
    })
  }
  const existing = maDot ? state.periods.find((p) => p.maDot === maDot) : undefined
  if (maDot && !existing) {
    throw new ApiError(404, { code: 'PERIOD_NOT_FOUND', message: `Không tìm thấy đợt đăng ký ${maDot}.` })
  }
  // PUT bỏ qua maHocKy trong thân — học kỳ của đợt không đổi được.
  const maHocKy = existing?.maHocKy ?? input.maHocKy
  const openOther = state.periods.some(
    (p) =>
      p.maHocKy === maHocKy && p.maCoSo === CAMPUS && p.trangThai === 'DANG_MO' && p.maDot !== maDot,
  )
  if (input.trangThai === 'DANG_MO' && openOther) {
    throw conflict(
      'PERIOD_ALREADY_OPEN',
      `Cơ sở ${CAMPUS} đã có một đợt đang mở cho học kỳ ${maHocKy}. Đóng đợt đó trước.`,
    )
  }

  const prefix = `${CAMPUS}-${maHocKy}-`
  const saved: EnrollmentPeriod = {
    maDot:
      maDot ??
      `${prefix}${String(state.periods.filter((p) => p.maDot.startsWith(prefix)).length + 1).padStart(2, '0')}`,
    maHocKy,
    maCoSo: CAMPUS,
    thoiGianMo: input.thoiGianMo,
    thoiGianDong: input.thoiGianDong,
    trangThai: input.trangThai,
  }
  state.periods = existing
    ? state.periods.map((p) => (p.maDot === maDot ? saved : p))
    : [...state.periods, saved]
  save(state)
  return saved
}

/** Lớp của cơ sở trong học kỳ, kể cả lớp đã huỷ — màn quản trị. */
export async function adminClasses(maHocKy: string): Promise<ClassOffer[]> {
  await delay()
  const state = load()
  return classesOfTerm(state, maHocKy).sort((a, b) => a.maLopHP.localeCompare(b.maLopHP))
}

const HO = ['Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Huỳnh', 'Phan', 'Vũ', 'Võ', 'Đặng', 'Bùi', 'Đỗ']
const DEM = ['Văn', 'Thị', 'Minh', 'Hoàng', 'Ngọc', 'Quốc', 'Thanh', 'Gia', 'Đức', 'Bảo']
const TEN = ['An', 'Bình', 'Châu', 'Dũng', 'Giang', 'Hà', 'Hải', 'Khánh', 'Linh', 'Long',
  'Mai', 'Nam', 'Phúc', 'Quân', 'Sơn', 'Trang', 'Tú', 'Vy', 'Yến', 'Huy']

/**
 * Danh sách lớp — `GET /api/classes/{maLopHP}/students`. Sinh viên demo thật
 * (B26DCCN001) có trong lớp nếu đang đăng ký; còn lại sinh tất định cho đủ sĩ số.
 */
export async function classRoster(maLopHP: string): Promise<ClassRoster> {
  await delay()
  const state = load()
  const lop = classOf(state, maLopHP)
  if (!lop) throw new ApiError(404, { code: 'CLASS_NOT_FOUND', message: `Không tìm thấy lớp ${maLopHP}.` })

  const mine = state.dangKy.find((e) => e.maLopHP === maLopHP)
  const seed = [...maLopHP].reduce((s, ch) => s + ch.charCodeAt(0), 0)
  const start = Date.parse(activePeriod(state).thoiGianMo)
  const others: RosterEntry[] = Array.from(
    { length: Math.max(0, lop.soLuongDaDangKy - (mine ? 1 : 0)) },
    (_, i) => {
      const k = seed + i * 7
      return {
        maSinhVien: `B26DC${['CN', 'AT', 'DT', 'VT'][k % 4]}${String(100 + ((seed * 3 + i * 13) % 800)).padStart(3, '0')}`,
        hoTen: `${HO[k % HO.length]} ${DEM[(k >> 1) % DEM.length]} ${TEN[(k * 3) % TEN.length]}`,
        maCoSoNha: CAMPUS,
        ngayDangKy: new Date(start + (i + 1) * 37 * 60_000).toISOString(),
        trangThai: 'DA_DANG_KY',
      }
    },
  )
  const sinhVien = mine
    ? [
        {
          maSinhVien: 'B26DCCN001',
          hoTen: 'Nguyễn Văn An',
          maCoSoNha: CAMPUS,
          ngayDangKy: mine.ngayDangKy ?? new Date(start).toISOString(),
          trangThai: mine.trangThai,
        },
        ...others,
      ]
    : others
  return { lop, sinhVien: sinhVien.sort((a, b) => a.maSinhVien.localeCompare(b.maSinhVien)) }
}

/**
 * Huỷ lớp — `POST /api/classes/{maLopHP}/cancel`. Một giao dịch: huỷ mọi ghi
 * danh, trả tín chỉ, sĩ số về 0, lớp sang `DA_HUY`. Đã huỷ rồi thì trả 0.
 */
export async function cancelClass(maLopHP: string): Promise<CancelledClass> {
  await delay()
  const state = load()
  const lop = classOf(state, maLopHP)
  if (!lop) throw new ApiError(404, { code: 'CLASS_NOT_FOUND', message: `Không tìm thấy lớp ${maLopHP}.` })
  if (lop.trangThai === 'DA_HUY') return { lop, soDangKyDaHuy: 0 }

  const soDangKyDaHuy = lop.soLuongDaDangKy
  state.siSo[maLopHP] = 0
  state.trangThaiLop[maLopHP] = 'DA_HUY'
  state.dangKy = state.dangKy.filter((e) => e.maLopHP !== maLopHP)
  save(state)
  return { lop: classOf(state, maLopHP)!, soDangKyDaHuy }
}

/** CTĐT của sinh viên — thay bằng `GET /api/programs/{maCTDT}`. */
export async function myProgram(): Promise<StudentProgram> {
  await delay()
  return DEMO_PROGRAM
}

/** Danh mục môn kèm khoa — thay bằng `GET /api/courses`. */
export async function courseCatalog(): Promise<CourseSummary[]> {
  await delay()
  return readMockCatalog().courses
}

/** Kết quả tốt nhất từng môn — thay bằng tổng hợp từ `GET /api/me/grades`. */
export async function myBestResults(): Promise<BestResults> {
  await delay()
  return BEST_RESULTS
}

/**
 * Mọi quan hệ tiên quyết, phẳng hoá. API thật: `GET /api/courses` rồi
 * `GET /api/courses/{maMonHoc}` (trường `tienQuyet`) cho từng môn.
 */
export async function listRelations(): Promise<CourseRelation[]> {
  await delay()
  return readMockCatalog().relations
}

export async function listOpenClasses(maHocKy: string): Promise<ClassOffer[]> {
  await delay()
  const state = load()
  // Sinh viên chỉ thấy lớp đang mở — lớp dự kiến chưa công bố, lớp huỷ thì thôi.
  return classesOfTerm(state, maHocKy).filter((c) => c.trangThai === 'MO')
}

export async function myEnrollments(maHocKy: string): Promise<StudentEnrollments> {
  await delay()
  return snapshot(load(), maHocKy)
}

/** `201` khi vừa đăng ký, `200` khi bấm lại đúng lớp đang giữ (`created = false`). */
export async function register(
  maLopHP: string,
): Promise<{ result: RegistrationResult; created: boolean }> {
  await delay()
  const state = load()
  const lop = classOf(state, maLopHP)
  if (!lop) {
    throw new ApiError(404, { code: 'CLASS_NOT_FOUND', message: `Không tìm thấy lớp ${maLopHP}.` })
  }

  if (!isOpen(activePeriod(state))) {
    throw conflict(
      'ENROLLMENT_PERIOD_CLOSED',
      `Cơ sở ${CAMPUS} không có đợt đăng ký đang mở cho học kỳ ${lop.maHocKy}.`,
    )
  }
  if (lop.trangThai !== 'MO') {
    throw conflict('CLASS_NOT_OPEN', `Lớp ${maLopHP} chưa mở đăng ký.`)
  }

  if (!DEMO_PROGRAM.monHoc.some((m) => m.maMonHoc === lop.maMonHoc)) {
    throw conflict(
      'COURSE_NOT_IN_PROGRAM',
      `Môn ${lop.maMonHoc} không thuộc chương trình đào tạo ${DEMO_PROGRAM.maCTDT}.`,
    )
  }

  const dangGiu = state.dangKy.find((e) => e.maMonHoc === lop.maMonHoc)
  const loaiDangKy: RegistrationResult['loaiDangKy'] =
    BEST_RESULTS[lop.maMonHoc] === 'DAT'
      ? 'CAI_THIEN'
      : BEST_RESULTS[lop.maMonHoc] === 'KHONG_DAT'
        ? 'HOC_LAI'
        : 'HOC_MOI'
  if (dangGiu) {
    if (dangGiu.maLopHP === maLopHP) return { result: { dangKy: dangGiu, loaiDangKy }, created: false }
    throw conflict(
      'ENROLLMENT_DUPLICATE_COURSE',
      `Bạn đã đăng ký lớp ${dangGiu.maLopHP} của môn ${lop.maMonHoc} trong học kỳ này.`,
    )
  }

  const prerequisites = readMockCatalog().relations
    .filter((relation) => relation.maMonHoc === lop.maMonHoc && relation.loai === 'TIEN_QUYET')
    .map((relation) => relation.maMonYeuCau)
  const chuaDat = prerequisites.filter((m) => BEST_RESULTS[m] !== 'DAT')
  if (chuaDat.length) {
    throw conflict('PREREQUISITE_NOT_MET', `Chưa đạt môn tiên quyết: ${chuaDat.join(', ')}.`)
  }

  for (const e of state.dangKy) {
    const other = classOf(state, e.maLopHP)
    if (other?.lich.some((a) => lop.lich.some((b) => clash(a, b)))) {
      throw conflict(
        'SCHEDULE_CLASH',
        `Lớp ${maLopHP} trùng lịch với lớp ${e.maLopHP} bạn đã đăng ký.`,
      )
    }
  }

  const daDangKy = credits(state)
  if (daDangKy + lop.soTinChi > TRAN_TIN_CHI) {
    throw conflict(
      'CREDIT_LIMIT_EXCEEDED',
      `Đã đăng ký ${daDangKy}/${TRAN_TIN_CHI} tín chỉ, thêm ${lop.soTinChi} tín chỉ của ${lop.maMonHoc} sẽ vượt trần.`,
    )
  }

  const siSo = state.siSo[maLopHP] ?? lop.soLuongDaDangKy
  if (siSo >= lop.soLuongToiDa) {
    throw conflict('CLASS_FULL', `Lớp ${maLopHP} đã đủ ${lop.soLuongToiDa} sinh viên.`)
  }

  const dangKy: EnrolledCourse = {
    maLopHP,
    maMonHoc: lop.maMonHoc,
    tenMonHoc: lop.tenMonHoc,
    soTinChi: lop.soTinChi,
    trangThai: 'DA_DANG_KY',
    ngayDangKy: new Date().toISOString(),
  }
  state.siSo[maLopHP] = siSo + 1
  state.dangKy.push(dangKy)
  save(state)
  return { result: { dangKy, loaiDangKy }, created: true }
}

/** Huỷ: trả chỗ + trả tín chỉ; trả về danh sách mới như `DELETE` của API. */
export async function cancel(maLopHP: string, maHocKy: string): Promise<StudentEnrollments> {
  await delay()
  const state = load()
  if (!state.dangKy.some((e) => e.maLopHP === maLopHP)) {
    throw new ApiError(404, {
      code: 'ENROLLMENT_NOT_FOUND',
      message: `Bạn không đăng ký lớp ${maLopHP}.`,
    })
  }
  if (!isOpen(activePeriod(state))) {
    throw conflict(
      'ENROLLMENT_PERIOD_CLOSED',
      `Cơ sở ${CAMPUS} không có đợt đăng ký đang mở cho học kỳ ${maHocKy}.`,
    )
  }
  state.dangKy = state.dangKy.filter((e) => e.maLopHP !== maLopHP)
  state.siSo[maLopHP] = Math.max(0, (state.siSo[maLopHP] ?? 1) - 1)
  save(state)
  return snapshot(state, maHocKy)
}

/** Chỉ bản giả: trả dữ liệu demo về như lúc đầu. */
export function resetDemo(): void {
  save(initial())
}

// --- Quản trị lớp học phần (F04) -------------------------------------------

const MODES = ['TRUC_TIEP', 'TRUC_TUYEN', 'KET_HOP']
const MAX_TIET = 12
const bad = (code: string, message: string) => new ApiError(400, { code, message })

/** Giảng viên của cơ sở — `GET /api/teachers`. */
export async function listTeachers(): Promise<TeacherOption[]> {
  await delay()
  return TEACHERS.filter((t) => t.maCoSo === CAMPUS)
}

/** Môn mở lớp được — danh mục môn của CTĐT, `GET /api/courses`. */
export async function openableCourses(): Promise<{ maMonHoc: string; tenMonHoc: string; soTinChi: number }[]> {
  await delay()
  const byCode = new Map<string, { maMonHoc: string; tenMonHoc: string; soTinChi: number }>()
  for (const m of DEMO_PROGRAM.monHoc) byCode.set(m.maMonHoc, m)
  for (const c of readMockCatalog().courses) byCode.set(c.maMonHoc, c)
  return [...byCode.values()].sort((a, b) => a.maMonHoc.localeCompare(b.maMonHoc))
}

function requireClass(state: State, maLopHP: string): ClassOffer {
  const lop = classOf(state, maLopHP)
  if (!lop) {
    throw new ApiError(404, { code: 'CLASS_NOT_FOUND', message: `Không tìm thấy lớp học phần ${maLopHP}.` })
  }
  return lop
}

function checkMode(hinhThucHoc: string, choPhepLienCoSo: boolean): void {
  if (!MODES.includes(hinhThucHoc)) {
    throw bad('CLASS_MODE_INVALID', `Hình thức học không hợp lệ: ${hinhThucHoc}.`)
  }
  // D18: kiểm "không trùng tiết" vô nghĩa khi hai điểm cách nhau 1.700 km.
  if (choPhepLienCoSo && hinhThucHoc !== 'TRUC_TUYEN') {
    throw bad('CROSS_CAMPUS_REQUIRES_ONLINE', 'Chỉ lớp trực tuyến mới cho phép đăng ký liên cơ sở.')
  }
}

function overlaps(a: SlotInput, b: SlotInput): boolean {
  return clash(a, b)
}

/** Lớp khác của cùng GV trùng giờ với `lich` — để phân công và xếp lịch. */
function teacherClashes(state: State, lop: ClassOffer, maGiangVien: string, lich: readonly SlotInput[]): string[] {
  return classesOfTerm(state, lop.maHocKy)
    .filter((c) => c.maLopHP !== lop.maLopHP && c.trangThai !== 'DA_HUY' && c.maGiangVien === maGiangVien)
    .filter((c) => c.lich.some((a) => lich.some((b) => overlaps(a, b))))
    .map((c) => c.maLopHP)
}

/**
 * Mở lớp — `POST /api/classes`. Mã lớp do server sinh từ môn + kỳ + cơ sở
 * trong JWT (`BAS1203-2026-1-HCM01`); lớp mới luôn ở `DU_KIEN`, chưa có lịch.
 */
export async function createClass(input: CreateClassInput): Promise<ClassOffer> {
  await delay()
  const state = load()
  const mon = (await openableCourses()).find((m) => m.maMonHoc === input.maMonHoc)
  if (!mon) throw bad('COURSE_NOT_FOUND', `Không có môn học ${input.maMonHoc}.`)
  if (input.soLuongToiDa < 1 || input.soLuongToiDa > 500) {
    throw bad('VALIDATION_ERROR', 'Sức chứa phải từ 1 đến 500.')
  }
  checkMode(input.hinhThucHoc, input.choPhepLienCoSo)

  const [y1, , k] = input.maHocKy.split('-')
  const prefix = `${input.maMonHoc}-${y1}-${k?.replace('HK', '')}-${CAMPUS}`
  const used = baseClasses(state)
    .filter((c) => c.maLopHP.startsWith(prefix))
    .map((c) => Number(c.maLopHP.slice(prefix.length)) || 0)
  const maLopHP = `${prefix}${String(Math.max(0, ...used) + 1).padStart(2, '0')}`
  const gv = input.maGiangVien ? TEACHERS.find((t) => t.maGiangVien === input.maGiangVien) : undefined
  if (input.maGiangVien && !gv) throw bad('TEACHER_NOT_FOUND', `Không có giảng viên ${input.maGiangVien}.`)

  const lop: ClassOffer = {
    maLopHP,
    maMonHoc: mon.maMonHoc,
    tenMonHoc: mon.tenMonHoc,
    soTinChi: mon.soTinChi,
    maHocKy: input.maHocKy,
    maCoSoHost: CAMPUS,
    maGiangVien: gv?.maGiangVien ?? null,
    tenGiangVien: gv?.hoTen ?? null,
    soLuongToiDa: input.soLuongToiDa,
    soLuongDaDangKy: 0,
    trangThai: 'DU_KIEN',
    choPhepLienCoSo: input.choPhepLienCoSo,
    hinhThucHoc: input.hinhThucHoc,
    phienBanLich: 0,
    lich: [],
  }
  state.lopMoi.push(lop)
  state.siSo[maLopHP] = 0
  save(state)
  return lop
}

/** Sửa lớp — `PUT /api/classes/{maLopHP}`. Không đặt thẳng `DA_KHOA`/`DA_HUY`. */
export async function updateClass(maLopHP: string, input: UpdateClassInput): Promise<ClassOffer> {
  await delay()
  const state = load()
  const lop = requireClass(state, maLopHP)
  if (!['DU_KIEN', 'MO', 'DA_KHOA', 'DA_HUY'].includes(input.trangThai)) {
    throw bad('CLASS_STATUS_INVALID', `Trạng thái lớp không hợp lệ: ${input.trangThai}.`)
  }
  if (lop.trangThai === 'DA_KHOA') throw conflict('GRADE_LOCKED', `Lớp ${maLopHP} đã khoá điểm, không sửa được.`)
  if (input.trangThai === 'DA_KHOA') {
    throw bad('CLASS_STATUS_INVALID', 'Khoá điểm bằng thao tác khoá bảng điểm, không đặt trạng thái trực tiếp.')
  }
  if (lop.trangThai === 'DA_HUY') throw conflict('CLASS_CANCELLED', `Lớp ${maLopHP} đã huỷ, không sửa được.`)
  if (input.trangThai === 'DA_HUY') {
    throw bad('CLASS_STATUS_INVALID', 'Huỷ lớp bằng thao tác huỷ lớp, không đặt trạng thái trực tiếp.')
  }
  checkMode(input.hinhThucHoc, input.choPhepLienCoSo)
  if (input.soLuongToiDa < 1 || input.soLuongToiDa > 500) {
    throw bad('VALIDATION_ERROR', 'Sức chứa phải từ 1 đến 500.')
  }
  if (input.soLuongToiDa < lop.soLuongDaDangKy) {
    throw conflict(
      'CLASS_CAPACITY_BELOW_ENROLLED',
      `Không hạ được sức chứa xuống ${input.soLuongToiDa}: lớp đang có ${lop.soLuongDaDangKy} sinh viên.`,
    )
  }
  state.lopSua[maLopHP] = { ...state.lopSua[maLopHP], ...input }
  save(state)
  return classOf(state, maLopHP)!
}

/** Phân công GV — `PUT /api/classes/{maLopHP}/teacher`. `null` = gỡ phân công. */
export async function assignTeacher(maLopHP: string, maGiangVien: string | null): Promise<ClassOffer> {
  await delay()
  const state = load()
  const lop = requireClass(state, maLopHP)
  if (lop.trangThai === 'DA_HUY') throw conflict('CLASS_CANCELLED', `Lớp ${maLopHP} đã huỷ, không sửa được.`)
  if (maGiangVien) {
    const gv = TEACHERS.find((t) => t.maGiangVien === maGiangVien)
    if (!gv) throw bad('TEACHER_NOT_FOUND', `Không có giảng viên ${maGiangVien}.`)
    if (gv.maCoSo !== lop.maCoSoHost) {
      throw bad(
        'TEACHER_WRONG_CAMPUS',
        `Giảng viên ${maGiangVien} thuộc cơ sở ${gv.maCoSo}, không dạy được lớp của cơ sở ${lop.maCoSoHost}.`,
      )
    }
    const clashes = teacherClashes(state, lop, maGiangVien, lop.lich)
    if (clashes.length) {
      throw conflict('TEACHER_SCHEDULE_CLASH', `Giảng viên ${gv.hoTen} đã có lịch trùng ở lớp ${clashes.join(', ')}.`)
    }
  }
  const gv = maGiangVien ? TEACHERS.find((t) => t.maGiangVien === maGiangVien) : undefined
  state.lopSua[maLopHP] = {
    ...state.lopSua[maLopHP],
    maGiangVien: gv?.maGiangVien ?? null,
    tenGiangVien: gv?.hoTen ?? null,
  }
  save(state)
  return classOf(state, maLopHP)!
}

/**
 * Xếp lịch — `PUT /api/classes/{maLopHP}/schedule`, thay TOÀN BỘ. Kiểm như
 * `ScheduleService`: lớp có SV thì khoá → buổi hợp lệ → tự trùng → trùng GV → trùng phòng.
 */
export async function setSchedule(maLopHP: string, lich: readonly SlotInput[]): Promise<ClassOffer> {
  await delay()
  const state = load()
  const lop = requireClass(state, maLopHP)
  if (lop.trangThai === 'DA_HUY') throw conflict('CLASS_CANCELLED', `Lớp ${maLopHP} đã huỷ, không sửa được.`)
  if (lop.soLuongDaDangKy > 0) {
    throw conflict(
      'CLASS_HAS_ENROLLMENTS',
      `Lớp ${maLopHP} đã có ${lop.soLuongDaDangKy} sinh viên đăng ký, không sửa được lịch.`,
    )
  }
  if (lich.length > 14) throw bad('VALIDATION_ERROR', 'Tối đa 14 buổi mỗi tuần.')
  lich.forEach((s, i) => {
    if (s.tietBatDau + s.soTiet - 1 > MAX_TIET) {
      throw bad(
        'SCHEDULE_SLOT_INVALID',
        `Buổi thứ ${i + 1} bắt đầu tiết ${s.tietBatDau} kéo ${s.soTiet} tiết là vượt quá tiết ${MAX_TIET}.`,
      )
    }
    if (s.tuanBatDau > s.tuanKetThuc) {
      throw bad('SCHEDULE_SLOT_INVALID', 'Tuần bắt đầu phải nhỏ hơn hoặc bằng tuần kết thúc.')
    }
  })
  lich.forEach((a, i) =>
    lich.slice(i + 1).forEach((b) => {
      if (overlaps(a, b)) throw bad('SCHEDULE_SELF_OVERLAP', `Hai buổi học của cùng lớp chồng nhau vào thứ ${a.thu}.`)
    }),
  )
  if (lop.maGiangVien) {
    const clashes = teacherClashes(state, lop, lop.maGiangVien, lich)
    if (clashes.length) {
      throw conflict(
        'SCHEDULE_TEACHER_CLASH',
        `Giảng viên ${lop.tenGiangVien ?? lop.maGiangVien} đã dạy lớp ${clashes.join(', ')} vào khung giờ này.`,
      )
    }
  }
  // Phòng so sau khi cắt khoảng trắng và bỏ phân biệt hoa thường.
  const room = (r: string | null) => (r ?? '').trim().toLowerCase()
  for (const s of lich) {
    if (!room(s.phongHoc)) continue
    const other = classesOfTerm(state, lop.maHocKy).find(
      (c) =>
        c.maLopHP !== maLopHP &&
        c.trangThai !== 'DA_HUY' &&
        c.lich.some((x) => room(x.phongHoc) === room(s.phongHoc) && overlaps(x, s)),
    )
    if (other) {
      throw conflict('SCHEDULE_ROOM_CLASH', `Phòng ${s.phongHoc} đã được lớp ${other.maLopHP} dùng vào khung giờ này.`)
    }
  }
  state.lopSua[maLopHP] = {
    ...state.lopSua[maLopHP],
    lich: lich.map((s) => ({ ...s, phongHoc: s.phongHoc?.trim() || null })),
    phienBanLich: lop.phienBanLich + 1,
  }
  save(state)
  return classOf(state, maLopHP)!
}

/** Bản giả làm việc trên đúng một học kỳ. */
export async function currentTerm(): Promise<CurrentTerm> {
  await delay()
  return {
    maHocKy: DEMO_MA_HOC_KY,
    tenHocKy: DEMO_TEN_HOC_KY,
    ngayBatDau: DEMO_NGAY_BAT_DAU,
  }
}
