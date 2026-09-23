/**
 * Thông tin nhận diện (branding) dùng chung cho toàn ứng dụng.
 *
 * Quy ước: KHÔNG hard-code các giá trị này trong component.
 * Import qua alias `@` rồi truyền xuống prop, ví dụ:
 *
 *   import { SITE } from '@/shared/config'
 */
export interface SiteConfig {
  /** Tên sản phẩm hiển thị trên UI */
  appName: string
  /** Tên đơn vị chủ quản (dòng copyright) */
  orgName: string
  /** Tên cơ sở đang vận hành */
  campusName: string
  /** Năm bắt đầu giữ bản quyền */
  copyrightYear: string
  /** Tiêu đề hệ thống trên thanh header */
  siteName: string
  /** Đường dẫn trang chủ */
  homeHref: string
  /** Nhãn phiên bản hiển thị ở footer */
  version: string
}

export const SITE: SiteConfig = {
  appName: 'PTIT One',
  orgName: 'Học viện Công nghệ Bưu chính Viễn thông',
  campusName: 'Cơ sở tại TP. Hồ Chí Minh',
  copyrightYear: '2020',
  siteName: 'HỆ THỐNG QUẢN LÝ ĐÀO TẠO ĐẠI HỌC CHÍNH QUY (UIS)',
  homeHref: '/',
  version: 'Version: BCVT-2025.08X.07',
}
