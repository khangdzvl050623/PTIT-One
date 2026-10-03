import { ApiError } from '@/shared/api'

import { isOpen } from '../lib/period'
import {
  BEST_RESULTS,
  CATALOG,
  COURSE_NAMES,
  DEMO_CLASSES,
  DEMO_ENROLLED,
  DEMO_PERIOD,
  DEMO_PERIODS,
  DEMO_PROGRAM,
  PREREQUISITES,
  TRAN_TIN_CHI,
} from '../data/demo'
import type {
  BestResults,
  ClassOffer,
  CourseRelation,
  CourseSummary,
  EnrolledCourse,
  EnrollmentPeriod,
  RegistrationResult,
  ScheduleSlot,
  StudentEnrollments,
  StudentProgram,
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

interface State {
  /** Sĩ số theo lớp — bộ đếm `SoLuongDaDangKy`. */
  siSo: Record<string, number>
  dangKy: EnrolledCourse[]
}

function initial(): State {
  return {
    siSo: Object.fromEntries(DEMO_CLASSES.map((c) => [c.maLopHP, c.soLuongDaDangKy])),
    dangKy: DEMO_ENROLLED.map((e) => ({ ...e })),
  }
}

function load(): State {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw) as State
  } catch {
    /* Bộ nhớ bị chặn hoặc hỏng — dùng dữ liệu ban đầu. */
  }
  return initial()
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
  return DEMO_PERIODS.filter((p) => !maHocKy || p.maHocKy === maHocKy)
}

export async function currentPeriod(): Promise<EnrollmentPeriod> {
  await delay()
  return DEMO_PERIOD
}

/** CTĐT của sinh viên — thay bằng `GET /api/programs/{maCTDT}`. */
export async function myProgram(): Promise<StudentProgram> {
  await delay()
  return DEMO_PROGRAM
}

/** Danh mục môn kèm khoa — thay bằng `GET /api/courses`. */
export async function courseCatalog(): Promise<CourseSummary[]> {
  await delay()
  return [...CATALOG]
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
  const name = (ma: string) =>
    CATALOG.find((c) => c.maMonHoc === ma)?.tenMonHoc ?? COURSE_NAMES[ma] ?? ma
  return Object.entries(PREREQUISITES).flatMap(([maMonHoc, yeuCau]) =>
    yeuCau.map((maMonYeuCau) => ({
      loai: 'TIEN_QUYET' as const,
      maMonHoc,
      tenMonHoc: name(maMonHoc),
      maMonYeuCau,
      tenMonYeuCau: name(maMonYeuCau),
    })),
  )
}

export async function listOpenClasses(maHocKy: string): Promise<ClassOffer[]> {
  await delay()
  const state = load()
  return DEMO_CLASSES.filter((c) => c.maHocKy === maHocKy).map((c) => ({
    ...c,
    soLuongDaDangKy: state.siSo[c.maLopHP] ?? c.soLuongDaDangKy,
  }))
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
  const lop = DEMO_CLASSES.find((c) => c.maLopHP === maLopHP)
  if (!lop) {
    throw new ApiError(404, { code: 'CLASS_NOT_FOUND', message: `Không tìm thấy lớp ${maLopHP}.` })
  }

  if (!isOpen(DEMO_PERIOD)) {
    throw conflict(
      'ENROLLMENT_PERIOD_CLOSED',
      `Cơ sở ${DEMO_PERIOD.maCoSo} không có đợt đăng ký đang mở cho học kỳ ${lop.maHocKy}.`,
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

  const chuaDat = (PREREQUISITES[lop.maMonHoc] ?? []).filter((m) => BEST_RESULTS[m] !== 'DAT')
  if (chuaDat.length) {
    throw conflict('PREREQUISITE_NOT_MET', `Chưa đạt môn tiên quyết: ${chuaDat.join(', ')}.`)
  }

  for (const e of state.dangKy) {
    const other = DEMO_CLASSES.find((c) => c.maLopHP === e.maLopHP)
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
  if (!isOpen(DEMO_PERIOD)) {
    throw conflict(
      'ENROLLMENT_PERIOD_CLOSED',
      `Cơ sở ${DEMO_PERIOD.maCoSo} không có đợt đăng ký đang mở cho học kỳ ${maHocKy}.`,
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
