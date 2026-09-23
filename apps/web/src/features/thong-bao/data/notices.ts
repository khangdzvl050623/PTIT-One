import type { Notice } from '../types'

/**
 * Dữ liệu tạm để dựng giao diện — sẽ thay bằng API thông báo khi backend sẵn sàng.
 * `href` giữ `#` cho tới khi có route chi tiết thông báo.
 */
export const SPOTLIGHT_NOTICE: Notice = {
  id: 'spotlight-hoc-phi-hk3',
  title:
    'V/v: Thu học phí Học kỳ hè (HK3) năm học 2024-2025 đối với sinh viên hệ Đại học chính quy',
  publishedAt: '03/07/2025 21:27',
  isNew: true,
  excerpt:
    'Căn cứ Quyết định 1989/QĐ-HV ngày 28/12/2022 của Giám đốc Học viện Công nghệ Bưu chính Viễn thông v/v Ban hành quy định về việc thu nộp học phí các kh ...',
  href: '#',
}

export const NOTICES: readonly Notice[] = [
  {
    id: 'dk-bo-sung-hk1-2025-2026',
    title:
      'Thông báo (Lịch đăng ký chi tiết) đăng ký bổ sung môn học HK1 năm học 2025-2026',
    publishedAt: '30/06/2025 10:43',
    isNew: true,
    href: '#',
  },
  {
    id: 'dk-mon-hoc-hk1-2025-2026',
    title: 'Thông báo (Lịch đăng ký chi tiết) đăng ký môn học HK1 năm học 2025-2026',
    publishedAt: '30/06/2025 10:33',
    isNew: true,
    href: '#',
  },
  {
    id: 'dk-mon-hoc-hk1',
    title: 'V/v đăng ký môn học HK1 năm học 2025-2026',
    publishedAt: '30/06/2025 10:28',
    isNew: true,
    href: '#',
  },
  {
    id: 'dk-hk-phu-2024-2025',
    title: 'Thông báo lịch đăng ký môn học HK phụ năm học 2024-2025',
    publishedAt: '26/05/2025 14:47',
    isNew: true,
    href: '#',
  },
  {
    id: 'dk-hoc-ky-phu',
    title: 'V/v đăng ký học kỳ phụ năm học 2024-2025',
    publishedAt: '26/05/2025 14:07',
    isNew: true,
    href: '#',
  },
  {
    id: 'thi-vet-tieng-anh-khoa-2020',
    title:
      'V/v tổ chức kỳ thi vét Tiếng Anh chuẩn đầu ra cho các khóa 2020 trở về trước hệ Đại học Chính quy',
    publishedAt: '22/05/2025 14:36',
    isNew: true,
    href: '#',
  },
  {
    id: 'chuan-dau-ra-chung-chi',
    title:
      'V/v: Thu tiền Xét, quản lý chuẩn đầu ra đối với sinh viên dùng chứng chỉ; Xét, quản lý học phần được chuyển đổi điểm; Xét, quản lý học phần được miễn học – miễn thi.',
    publishedAt: '15/05/2025 16:49',
    isNew: true,
    href: '#',
  },
  {
    id: 'hoc-phi-cuoi-khoa-k2021',
    title:
      'V/v: Thu học phí sinh viên cuối khóa làm đồ án và các môn thay thế tốt nghiệp Hệ Đại học chính quy Khóa 2021 khối ngành kinh tế và các khóa trước trả nợ;',
    publishedAt: '15/05/2025 16:37',
    isNew: true,
    href: '#',
  },
  {
    id: 'thi-vet-tieng-anh-dau-ra',
    title:
      'Thông báo kỳ thi vét Tiếng Anh Đầu Ra cho các khóa từ 2020 trở về trước',
    publishedAt: '24/04/2025 15:13',
    isNew: true,
    href: '#',
  },
]

export const TUITION_NOTICES: readonly Notice[] = [
  {
    id: 'huong-dan-nop-hoc-phi',
    title:
      'Thông báo V/v: Hướng dẫn nộp tiền học phí và các khoản thu khác của sinh viên qua hệ thống ngân hàng',
    publishedAt: '12/01/2024 23:28',
    isNew: true,
    href: '#',
  },
]
