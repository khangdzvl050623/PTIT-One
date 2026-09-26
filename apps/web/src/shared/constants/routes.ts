/**
 * Đường dẫn của các tuyến — dùng chung giữa router, menu và `<Link>`.
 * Không viết chuỗi đường dẫn thẳng trong component.
 */
export const ROUTES = {
  home: '/',
  login: '/dang-nhap',
  account: '/tai-khoan',
  forbidden: '/khong-du-quyen',
  notFound: '*',
} as const

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES]
