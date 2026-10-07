import { apiFetch, apiRequest } from '@/shared/api'

import type {
  BestResults,
  CancelledClass,
  ClassOffer,
  ClassRoster,
  ClassSection,
  CourseRelation,
  CreateClassInput,
  CurrentTerm,
  CourseSummary,
  EnrollmentPeriod,
  ProgramCourse,
  RegistrationResult,
  SavePeriodInput,
  ScheduleSlot,
  SlotInput,
  StudentEnrollments,
  StudentProgram,
  TeacherOption,
  UpdateClassInput,
} from '../types'

/**
 * Đăng ký học phần (F08) qua API thật. Cùng chữ ký với `mockEnrollmentApi`.
 *
 * Mọi phép kiểm điều kiện (đợt mở, tiên quyết, trùng lịch, trần tín chỉ, hết
 * chỗ) do **backend** quyết trong một giao dịch. Những gì tính ở đây chỉ để
 * hiển thị — không được dùng làm điều kiện cho phép hay chặn.
 */

export function listPeriods(maHocKy?: string): Promise<EnrollmentPeriod[]> {
  const query = maHocKy ? `?maHocKy=${encodeURIComponent(maHocKy)}` : ''
  return apiFetch<EnrollmentPeriod[]>(`/api/enrollment-periods${query}`)
}

/**
 * Đợt đang mở của cơ sở người gọi.
 *
 * "Đang mở" cần CẢ `TrangThai = DANG_MO` VÀ hiện tại nằm trong khung giờ —
 * đúng như backend kiểm; chỉ xem trạng thái sẽ nhận cả đợt đã hết giờ mà quên đóng.
 */
export async function currentPeriod(): Promise<EnrollmentPeriod> {
  const now = Date.now()
  const periods = await listPeriods()
  const open = periods.find(
    (d) =>
      d.trangThai === 'DANG_MO' &&
      Date.parse(d.thoiGianMo) <= now &&
      now <= Date.parse(d.thoiGianDong),
  )
  if (open) return open

  /* Không có đợt mở là trạng thái BÌNH THƯỜNG (ngoài kỳ đăng ký), không phải
     lỗi. Trả đợt gần nhất để màn hình hiện đúng khung giờ; nếu không có đợt
     nào thì trả một đợt rỗng đã đóng. */
  return (
    [...periods].sort((a, b) => Date.parse(b.thoiGianMo) - Date.parse(a.thoiGianMo))[0] ?? {
      maDot: '',
      maHocKy: '',
      maCoSo: '',
      thoiGianMo: new Date(0).toISOString(),
      thoiGianDong: new Date(0).toISOString(),
      trangThai: 'DA_DONG',
    }
  )
}

interface TermResponse {
  maHocKy: string
  tenHocKy: string
  ngayBatDau: string
}

/**
 * Học kỳ mà màn đăng ký làm việc = học kỳ của ĐỢT ĐANG MỞ, không phải học kỳ
 * chứa hôm nay. Hai thứ đó khác nhau: đợt đăng ký cho kỳ sau thường mở trong
 * khi kỳ hiện tại còn đang học.
 */
export async function currentTerm(): Promise<CurrentTerm> {
  const [period, terms] = await Promise.all([
    currentPeriod(),
    apiFetch<readonly TermResponse[]>('/api/terms'),
  ])
  const term = terms.find((t) => t.maHocKy === period.maHocKy)
  return {
    maHocKy: period.maHocKy,
    tenHocKy: term?.tenHocKy ?? period.maHocKy,
    ngayBatDau: term?.ngayBatDau ?? '',
  }
}

interface ProfileResponse {
  maCTDT: string
}

interface ProgramDetailResponse {
  chuongTrinh: { maCTDT: string; tenCTDT: string }
  monHoc: readonly ProgramCourse[]
}

export async function myProgram(): Promise<StudentProgram> {
  const { maCTDT } = await apiFetch<ProfileResponse>('/api/me/profile')
  const detail = await apiFetch<ProgramDetailResponse>(
    `/api/programs/${encodeURIComponent(maCTDT)}`,
  )
  return {
    maCTDT: detail.chuongTrinh.maCTDT,
    tenCTDT: detail.chuongTrinh.tenCTDT,
    /* ⚠️ Backend CHƯA có "sinh viên đang ở học kỳ thứ mấy" — không cột nào suy
       ra được. 0 làm phạm vi KE_HOACH bị ẩn (xem lib/scope.ts) thay vì hiện ra
       một bộ lọc luôn rỗng. Nhóm đang chốt cách xác định. */
    hocKyHienTai: 0,
    monHoc: detail.monHoc,
  }
}

export function courseCatalog(): Promise<CourseSummary[]> {
  return apiFetch<CourseSummary[]>('/api/courses')
}

interface GradeRow {
  maMonHoc: string
  diemTongKet: number | null
  ketQua: 'DAT' | 'KHONG_DAT' | null
  daCongBo: boolean
}

/**
 * Kết quả tốt nhất mỗi môn. Lấy LẦN ĐIỂM CAO NHẤT vì học lại và cải thiện đều
 * được phép — cùng quy tắc backend dùng khi xét tiên quyết.
 */
export async function myBestResults(): Promise<BestResults> {
  const rows = await apiFetch<readonly GradeRow[]>('/api/me/grades')
  const best = new Map<string, { diem: number; ketQua: 'DAT' | 'KHONG_DAT' }>()
  for (const row of rows) {
    if (!row.daCongBo || row.diemTongKet === null || row.ketQua === null) continue
    const prev = best.get(row.maMonHoc)
    if (!prev || row.diemTongKet > prev.diem) {
      best.set(row.maMonHoc, { diem: row.diemTongKet, ketQua: row.ketQua })
    }
  }
  return Object.fromEntries([...best].map(([ma, v]) => [ma, v.ketQua]))
}

/** Chỉ có `TIEN_QUYET`: `MonHocTienQuyet` không có cột loại quan hệ. */
export function listRelations(): Promise<CourseRelation[]> {
  return apiFetch<CourseRelation[]>('/api/prerequisites')
}

interface TermSchedulesResponse {
  lopHocPhan: readonly { maLopHP: string; buoiHoc: readonly ScheduleSlot[] }[]
}

/**
 * Lớp mở trong học kỳ, kèm lịch.
 *
 * Hai lời gọi SONG SONG rồi ghép: `GET /api/classes` không trả lịch vì module
 * `course` không phụ thuộc `timetable`. `GET /api/schedules` trả lịch mọi lớp
 * trong kỳ một lần, nên không thành N+1.
 */
export async function listOpenClasses(maHocKy: string): Promise<ClassOffer[]> {
  const term = encodeURIComponent(maHocKy)
  const [classes, schedules] = await Promise.all([
    apiFetch<readonly ClassSection[]>(`/api/classes?maHocKy=${term}`),
    apiFetch<TermSchedulesResponse>(`/api/schedules?maHocKy=${term}`),
  ])

  const byClass = new Map(schedules.lopHocPhan.map((e) => [e.maLopHP, e.buoiHoc]))
  return classes
    .filter((lop) => lop.trangThai !== 'DA_HUY')
    .map((lop) => ({ ...lop, lich: byClass.get(lop.maLopHP) ?? [] }))
}

export function myEnrollments(maHocKy: string): Promise<StudentEnrollments> {
  return apiFetch<StudentEnrollments>(
    `/api/me/enrollments?maHocKy=${encodeURIComponent(maHocKy)}`,
  )
}

/** `201` khi vừa đăng ký, `200` khi bấm lại đúng lớp đang giữ. */
export async function register(
  maLopHP: string,
): Promise<{ result: RegistrationResult; created: boolean }> {
  const { data, status } = await apiRequest<RegistrationResult>('/api/me/enrollments', {
    method: 'POST',
    json: { maLopHP },
  })
  return { result: data, created: status === 201 }
}

export function cancel(maLopHP: string, maHocKy: string): Promise<StudentEnrollments> {
  void maHocKy // Backend lấy học kỳ từ chính lớp; tham số có để khớp bản giả.
  return apiFetch<StudentEnrollments>(
    `/api/me/enrollments/${encodeURIComponent(maLopHP)}`,
    { method: 'DELETE' },
  )
}

/** Chỉ có nghĩa ở bản giả. Dữ liệu thật không có gì để đặt lại. */
export function resetDemo(): void {
  /* Không làm gì — màn hình chỉ hiện nút này khi chạy bản giả (prop `demo`). */
}

// --- Quản trị lớp học phần và đợt đăng ký (F04) -----------------------------
//
// Mọi endpoint dưới đây chỉ `ADMIN_CO_SO` của cơ sở sở hữu lớp gọi được. Cơ sở
// KHÔNG phải tham số: server lấy từ JWT đã ký. Admin Master chỉ ĐỌC (B3).

/** Lớp của học kỳ kèm lịch — cùng nguồn với {@link listOpenClasses} nhưng không lọc lớp đã huỷ. */
export async function adminClasses(maHocKy: string): Promise<ClassOffer[]> {
  const term = encodeURIComponent(maHocKy)
  const [classes, schedules] = await Promise.all([
    apiFetch<readonly ClassSection[]>(`/api/classes?maHocKy=${term}`),
    apiFetch<TermSchedulesResponse>(`/api/schedules?maHocKy=${term}`),
  ])
  const byClass = new Map(schedules.lopHocPhan.map((e) => [e.maLopHP, e.buoiHoc]))
  return classes.map((lop) => ({ ...lop, lich: byClass.get(lop.maLopHP) ?? [] }))
}

/** Giảng viên trong cơ sở của người gọi — server tự giới hạn phạm vi. */
export function listTeachers(): Promise<TeacherOption[]> {
  return apiFetch<TeacherOption[]>('/api/teachers')
}

/** Môn mở lớp được = toàn bộ danh mục. Server kiểm môn có thật khi tạo lớp. */
export function openableCourses(): Promise<{ maMonHoc: string; tenMonHoc: string; soTinChi: number }[]> {
  return apiFetch<{ maMonHoc: string; tenMonHoc: string; soTinChi: number }[]>('/api/courses')
}

/** Lớp mới chưa có lịch nên `lich` rỗng; mã lớp do SERVER sinh. */
export async function createClass(input: CreateClassInput): Promise<ClassOffer> {
  const lop = await apiFetch<ClassSection>('/api/classes', { method: 'POST', json: input })
  return { ...lop, lich: [] }
}

export function updateClass(maLopHP: string, input: UpdateClassInput): Promise<ClassOffer> {
  return withSchedule(
    apiFetch<ClassSection>(`/api/classes/${encodeURIComponent(maLopHP)}`, {
      method: 'PUT',
      json: input,
    }),
    maLopHP,
  )
}

export function assignTeacher(maLopHP: string, maGiangVien: string | null): Promise<ClassOffer> {
  return withSchedule(
    apiFetch<ClassSection>(`/api/classes/${encodeURIComponent(maLopHP)}/teacher`, {
      method: 'PUT',
      json: { maGiangVien },
    }),
    maLopHP,
  )
}

/**
 * Thay TOÀN BỘ lịch của lớp; danh sách rỗng = xoá hết.
 *
 * Server chặn khi lớp đã có sinh viên (`CLASS_HAS_ENROLLMENTS`), trùng giảng
 * viên, trùng phòng, hoặc hai buổi của chính lớp đó chồng nhau.
 */
export async function setSchedule(
  maLopHP: string,
  lich: readonly SlotInput[],
): Promise<ClassOffer> {
  await apiFetch(`/api/classes/${encodeURIComponent(maLopHP)}/schedule`, {
    method: 'PUT',
    json: { buoiHoc: lich },
  })
  return withSchedule(
    apiFetch<ClassSection>(`/api/classes/${encodeURIComponent(maLopHP)}`),
    maLopHP,
  )
}

/** Huỷ CẢ lớp: mọi ghi danh bị huỷ, tín chỉ trả lại, lớp thành `DA_HUY`. */
export function cancelClass(maLopHP: string): Promise<CancelledClass> {
  return apiFetch<CancelledClass>(`/api/classes/${encodeURIComponent(maLopHP)}/cancel`, {
    method: 'POST',
    json: {},
  })
}

export function classRoster(maLopHP: string): Promise<ClassRoster> {
  return apiFetch<ClassRoster>(`/api/classes/${encodeURIComponent(maLopHP)}/students`)
}

/**
 * Tạo đợt mới (`maDot = null`) hoặc sửa đợt đang có.
 *
 * ⚠️ Khi sửa, server **bỏ qua** `maHocKy` trong thân — học kỳ của đợt không đổi
 * được. Muốn đổi kỳ thì tạo đợt mới.
 */
export function savePeriod(
  maDot: string | null,
  input: SavePeriodInput,
): Promise<EnrollmentPeriod> {
  if (maDot === null) {
    return apiFetch<EnrollmentPeriod>('/api/enrollment-periods', { method: 'POST', json: input })
  }
  return apiFetch<EnrollmentPeriod>(`/api/enrollment-periods/${encodeURIComponent(maDot)}`, {
    method: 'PUT',
    json: input,
  })
}

/** Lấy lại lịch sau khi ghi: `GET /api/classes/{id}` không trả lịch. */
async function withSchedule(lop: Promise<ClassSection>, maLopHP: string): Promise<ClassOffer> {
  const [section, schedule] = await Promise.all([
    lop,
    apiFetch<{ buoiHoc: readonly ScheduleSlot[] }>(
      `/api/classes/${encodeURIComponent(maLopHP)}/schedule`,
    ),
  ])
  return { ...section, lich: schedule.buoiHoc }
}
