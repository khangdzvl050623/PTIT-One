/**
 * Nhãn UI dùng chung ở nhiều màn hình.
 *
 * Chỉ đưa vào đây những nhãn bị lặp ở từ 2 chỗ trở lên (hoặc cần đổi thống nhất).
 * Nhãn riêng của một màn hình thì viết thẳng trong template của màn hình đó.
 */
export const LABELS = {
  home: 'Trang chủ',
  notices: 'THÔNG BÁO',
  tuition: 'HỌC PHÍ',
  login: 'ĐĂNG NHẬP',
  username: 'Mã sinh viên',
  password: 'Mật khẩu',
  loginSubmit: 'Đăng nhập',
  loginPending: 'Chưa nối API xác thực — biểu mẫu sẽ hoạt động khi backend sẵn sàng.',
  accessStats: 'THỐNG KÊ TRUY CẬP',
  placeholderText: 'Màn hình đang được dựng. Nội dung sẽ xuất hiện ở đây khi tính năng hoàn thiện.',
  notFoundTitle: 'Không tìm thấy trang',
  notFoundText: 'Đường dẫn bạn mở không tồn tại trong hệ thống.',
} as const
