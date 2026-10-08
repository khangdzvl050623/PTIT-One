import { apiFetch } from '@/shared/api'

import type {
  CourseResult,
  StudentProfile,
  StudentSummary,
  TeacherProfile,
  TermResults,
  UpdateMyProfileInput,
} from '../types'

/**
 * Hồ sơ và số liệu nhanh của trang Thông tin.
 *
 * Ba ô số liệu lấy từ **ba** endpoint khác nhau vì backend không có endpoint
 * tổng hợp cho màn này — gọi song song rồi gộp ở đây, chứ không bắt màn hình
 * biết chuyện đó.
 */

export function fetchProfile(): Promise<StudentProfile> {
  return apiFetch<StudentProfile>('/api/me/profile')
}

/**
 * Lưu phần lý lịch. Trả về hồ sơ ĐẦY ĐỦ sau khi lưu, nên màn hình dùng luôn
 * kết quả này thay vì gọi lại {@link fetchProfile}.
 *
 * Cần email đã xác minh, nếu không server trả `409 EMAIL_NOT_VERIFIED`.
 */
export function updateMyProfile(input: UpdateMyProfileInput): Promise<StudentProfile> {
  return apiFetch<StudentProfile>('/api/me/profile', { method: 'PUT', json: input })
}

/**
 * Tải ảnh đại diện từ máy người dùng.
 *
 * Gửi **file thật** dạng `multipart/form-data`; server kiểm định dạng bằng byte
 * đầu file, đẩy lên Cloudinary rồi tự ghi URL vào hồ sơ. Client không bao giờ
 * đặt URL, nên không trỏ được ảnh sang địa chỉ bất kỳ.
 *
 * Không đặt `Content-Type`: trình duyệt phải tự sinh, vì nó còn phải kèm
 * `boundary` của phần thân. Đặt tay là server không tách được các phần.
 */
export function uploadAvatar(file: File): Promise<StudentProfile> {
  const form = new FormData()
  form.append('file', file)
  return apiFetch<StudentProfile>('/api/me/profile/avatar', { method: 'POST', form })
}

export function removeAvatar(): Promise<StudentProfile> {
  return apiFetch<StudentProfile>('/api/me/profile/avatar', { method: 'DELETE' })
}

// --- Giảng viên ------------------------------------------------------
//
// Đường riêng chứ không dùng chung `/api/me/profile`: hai vai trả hai hình
// dạng khác nhau, gộp một đường sẽ buộc màn hình tự đoán kiểu theo vai trò.

const GV = '/api/me/teacher-profile'

export function fetchTeacherProfile(): Promise<TeacherProfile> {
  return apiFetch<TeacherProfile>(GV)
}

export function updateMyTeacherProfile(input: UpdateMyProfileInput): Promise<TeacherProfile> {
  return apiFetch<TeacherProfile>(GV, { method: 'PUT', json: input })
}

export function uploadTeacherAvatar(file: File): Promise<TeacherProfile> {
  const form = new FormData()
  form.append('file', file)
  return apiFetch<TeacherProfile>(`${GV}/avatar`, { method: 'POST', form })
}

export function removeTeacherAvatar(): Promise<TeacherProfile> {
  return apiFetch<TeacherProfile>(`${GV}/avatar`, { method: 'DELETE' })
}

interface UnreadCount {
  soChuaDoc: number
}

/**
 * Số thông báo chưa đọc. Dùng được cho CẢ sinh viên và giảng viên —
 * `/api/me/notifications` mở cho hai vai đó.
 *
 * Tách riêng khỏi {@link fetchSummary} vì ba ô số liệu của sinh viên gộp thêm
 * thời khoá biểu và tín chỉ, hai thứ giảng viên không có.
 */
export async function fetchUnreadCount(): Promise<number> {
  return (await apiFetch<UnreadCount>('/api/me/notifications/unread-count')).soChuaDoc
}

interface TimetableResponse {
  buoiHoc: readonly { tuanBatDau: number; tuanKetThuc: number }[]
}

interface EnrollmentsResponse {
  soTinChiDaDangKy: number
  tranTinChi: number | null
}

interface TermRange {
  maHocKy: string
  ngayBatDau: string
  ngayKetThuc: string
}

const MS_PER_DAY = 86_400_000

/**
 * Học kỳ chứa ngày hôm nay, và hôm nay là tuần thứ mấy của kỳ đó.
 *
 * Tự xác định ở đây để {@link fetchSummary} không cần tham số — nhờ vậy bản
 * thật và bản giả cùng chữ ký và màn hình không phải biết hôm nay là kỳ nào.
 */
async function currentTerm(): Promise<{ maHocKy: string; tuan: number } | null> {
  const today = new Date()
  const terms = await apiFetch<readonly TermRange[]>('/api/terms')
  const term = terms.find(
    (t) => new Date(t.ngayBatDau) <= today && today <= new Date(t.ngayKetThuc),
  )
  if (!term) return null

  const days = Math.floor((today.getTime() - new Date(term.ngayBatDau).getTime()) / MS_PER_DAY)
  return { maHocKy: term.maHocKy, tuan: Math.floor(days / 7) + 1 }
}

/** Ba ô số liệu đều về 0 khi hôm nay không nằm trong học kỳ nào (nghỉ hè). */
const KHONG_CO_SO_LIEU: StudentSummary = {
  thongBaoChuaDoc: 0,
  buoiHocTrongTuan: 0,
  tinChiHocKy: { daDangKy: 0, tran: 0 },
}

export async function fetchSummary(): Promise<StudentSummary> {
  const now = await currentTerm()

  /* Một endpoint lỗi không được làm trắng cả ba ô: dùng allSettled rồi điền 0
     cho ô nào không lấy được. Màn Thông tin là trang chủ của sinh viên, hỏng
     một con số không đáng để trắng cả trang. */
  const [unread, timetable, enrollments] = await Promise.allSettled([
    apiFetch<UnreadCount>('/api/me/notifications/unread-count'),
    now
      ? apiFetch<TimetableResponse>(
          `/api/me/timetable?maHocKy=${encodeURIComponent(now.maHocKy)}&tuan=${now.tuan}`,
        )
      : Promise.reject(new Error('ngoài học kỳ')),
    now
      ? apiFetch<EnrollmentsResponse>(
          `/api/me/enrollments?maHocKy=${encodeURIComponent(now.maHocKy)}`,
        )
      : Promise.reject(new Error('ngoài học kỳ')),
  ])

  return {
    ...KHONG_CO_SO_LIEU,
    thongBaoChuaDoc: unread.status === 'fulfilled' ? unread.value.soChuaDoc : 0,
    buoiHocTrongTuan: timetable.status === 'fulfilled' ? timetable.value.buoiHoc.length : 0,
    tinChiHocKy: {
      daDangKy: enrollments.status === 'fulfilled' ? enrollments.value.soTinChiDaDangKy : 0,
      tran: enrollments.status === 'fulfilled' ? (enrollments.value.tranTinChi ?? 0) : 0,
    },
  }
}

interface TranscriptTerm {
  maHocKy: string
  tenHocKy: string
  monHoc: readonly { maMonHoc: string; tenMonHoc: string; diemTongKet: number | null }[]
}

/** Biểu đồ điểm: rút gọn từ bảng điểm, kỳ cũ trước cho trục thời gian đi xuôi. */
export async function fetchResults(): Promise<readonly TermResults[]> {
  const terms = await apiFetch<readonly TranscriptTerm[]>('/api/me/transcript')
  return terms
    .map((term) => ({
      maHocKy: term.maHocKy,
      tenHocKy: term.tenHocKy,
      monHoc: term.monHoc.map(
        (m): CourseResult => ({
          maMonHoc: m.maMonHoc,
          tenMonHoc: m.tenMonHoc,
          diemTongKet: m.diemTongKet,
        }),
      ),
    }))
    .reverse()
}
