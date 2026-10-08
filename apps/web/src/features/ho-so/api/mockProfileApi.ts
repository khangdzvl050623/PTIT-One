import { currentMockUser } from '@/features/auth/api/mockAuthApi'
import { fetchGrades } from '@/features/bang-diem/api/mockGradesApi'
import { ApiError } from '@/shared/api'

import { DEMO_PROFILE, DEMO_SUMMARY } from '../data/profile'
import type {
  CourseResult,
  StudentProfile,
  StudentSummary,
  TermResults,
  UpdateMyProfileInput,
} from '../types'

/**
 * Bản giả để dựng UI khi chưa chạy backend. Cùng chữ ký với `httpProfileApi`
 * nên màn hình không phân biệt được hai bản.
 */

/** Đủ lâu để thấy trạng thái "Đang tải…" khi thiết kế. */
const LATENCY_MS = 300

const STORAGE_KEY = 'ptitone:mock:ho-so-sinh-vien'

export async function fetchProfile(): Promise<StudentProfile> {
  await delay()
  return read()
}

/**
 * Lưu phần lý lịch, giữ ĐÚNG hai hành vi của server để giao diện dựng trên bản
 * giả không gặp bất ngờ khi bật API thật:
 *
 * 1. Chưa xác minh email thì `409 EMAIL_NOT_VERIFIED`.
 * 2. Thay toàn bộ chín ô — ô trống xoá giá trị cũ, không giữ nguyên.
 */
export async function updateMyProfile(input: UpdateMyProfileInput): Promise<StudentProfile> {
  await delay()
  requireVerifiedEmail()

  const saved: StudentProfile = {
    ...read(),
    gioiTinh: input.gioiTinh,
    dienThoai: trim(input.dienThoai),
    soCCCD: trim(input.soCCCD),
    emailCaNhan: trim(input.emailCaNhan),
    noiSinh: trim(input.noiSinh),
    danToc: trim(input.danToc),
    tonGiao: trim(input.tonGiao),
    hoKhau: trim(input.hoKhau),
    // anhDaiDien KHÔNG nằm ở đây: ảnh có đường riêng, như bản thật.
  }
  write(saved)
  return saved
}

/**
 * Tải ảnh đại diện.
 *
 * Không có Cloudinary nên dùng `URL.createObjectURL`: ảnh hiện đúng ngay trong
 * phiên này, nhưng **mất sau khi F5** vì blob URL chỉ sống trong bộ nhớ trang.
 * Khi đó `IdPhoto` quay về khung mặc định — cùng cách nó xử lý một URL
 * Cloudinary đã bị xoá, nên giao diện không có nhánh nào mới.
 *
 * Giữ hai cổng chặn của bản thật: cần email đã xác minh, và chỉ nhận ảnh.
 */
export async function uploadAvatar(file: File): Promise<StudentProfile> {
  await delay()
  requireVerifiedEmail()
  if (!file.type.startsWith('image/')) {
    throw new ApiError(400, {
      code: 'IMAGE_FORMAT_INVALID',
      message: 'Chỉ nhận ảnh JPEG, PNG, GIF, WEBP hoặc BMP.',
    })
  }
  const saved: StudentProfile = { ...read(), anhDaiDien: URL.createObjectURL(file) }
  write(saved)
  return saved
}

export async function removeAvatar(): Promise<StudentProfile> {
  await delay()
  requireVerifiedEmail()
  const saved: StudentProfile = { ...read(), anhDaiDien: null }
  write(saved)
  return saved
}

function requireVerifiedEmail(): void {
  if (!currentMockUser()?.emailDaXacMinh) {
    throw new ApiError(409, {
      code: 'EMAIL_NOT_VERIFIED',
      message: 'Cần xác minh email trước khi sửa hồ sơ. Vào Tài khoản > Email để xác minh.',
    })
  }
}

export async function fetchSummary(): Promise<StudentSummary> {
  await delay()
  return DEMO_SUMMARY
}

/**
 * Biểu đồ điểm dùng CÙNG nguồn với bảng điểm, nên nó cũng thấy điểm giảng viên
 * vừa công bố. Dùng `DEMO_RESULTS` riêng thì hai màn cùng nói về một thứ mà ra
 * hai số khác nhau.
 */
export async function fetchResults(): Promise<readonly TermResults[]> {
  const grades = await fetchGrades()
  const theoKy = new Map<string, TermResults>()
  for (const g of grades) {
    const mon: CourseResult = {
      maMonHoc: g.maMonHoc,
      tenMonHoc: g.tenMonHoc,
      diemTongKet: g.diemTongKet,
    }
    const ky = theoKy.get(g.maHocKy)
    if (ky) {
      theoKy.set(g.maHocKy, { ...ky, monHoc: [...ky.monHoc, mon] })
    } else {
      theoKy.set(g.maHocKy, { maHocKy: g.maHocKy, tenHocKy: g.tenHocKy, monHoc: [mon] })
    }
  }
  // Kỳ cũ trước, cho trục thời gian đi xuôi — như bản http.
  return [...theoKy.values()].sort((a, b) => a.maHocKy.localeCompare(b.maHocKy))
}

/** Chuỗi rỗng hoặc toàn khoảng trắng = bỏ trống, như `MyProfileService.trim`. */
function trim(value: string | null): string | null {
  const cut = value?.trim()
  return cut ? cut : null
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}

/* localStorage có thể ném lỗi ở chế độ riêng tư — khi đó chỉ mất bản sửa khi F5. */
function read(): StudentProfile {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return DEMO_PROFILE
    /* Hợp nhất với DEMO_PROFILE: bản lưu cũ có thể thiếu trường mới thêm, và
       những ô hành chính không sửa được thì luôn lấy theo dữ liệu mẫu. */
    return { ...DEMO_PROFILE, ...(JSON.parse(raw) as Partial<StudentProfile>) }
  } catch {
    return DEMO_PROFILE
  }
}

function write(profile: StudentProfile): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(profile))
  } catch {
    /* Hết chỗ hoặc bị chặn: bản giả vẫn trả đúng giá trị vừa lưu trong phiên này. */
  }
}
