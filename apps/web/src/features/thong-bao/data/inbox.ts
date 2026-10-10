import type { InboxItem } from '../api/inboxTypes'

/**
 * Hộp thư mẫu của sinh viên: tin lớp của giảng viên lẫn tin hành chính, lẫn
 * đã/chưa đọc. Ngày giờ cố định để ảnh chụp và review ổn định.
 */
export const INBOX_MOCK: InboxItem[] = [
  {
    maThongBao: 'a1a1a1a1-1111-1111-1111-111111111111',
    loai: 'SOAN',
    suKien: null,
    mucDo: 'THONG_THUONG',
    tieuDe: 'Nhóm Zalo học phần Nhập môn công nghệ phần mềm (SoftTech) _D23',
    noiDung:
      'Mời cả lớp tham gia nhóm Zalo học phần để tiện trao đổi, chia sẻ thông tin và nội dung học tập. Mọi hoạt động của lớp đều công khai trong nhóm.',
    lienKet: null,
    vaiTroNguoiGui: 'GIANG_VIEN',
    ngayGui: '2026-01-13T02:00:00Z',
    daDoc: false,
    ngayDoc: null,
  },
  {
    maThongBao: 'b2b2b2b2-2222-2222-2222-222222222222',
    loai: 'SOAN',
    suKien: null,
    mucDo: 'QUAN_TRONG',
    tieuDe: 'Thông báo số 13: Kết thúc học phần Cơ sở dữ liệu (INT1313) và chuẩn bị thi cuối kỳ',
    noiDung:
      'Lớp Cơ sở dữ liệu (INT1313) kết thúc học phần vào tuần 15. Sinh viên ôn tập theo đề cương đã phát, mang thẻ sinh viên khi dự thi và có mặt trước giờ thi 15 phút.',
    lienKet: null,
    vaiTroNguoiGui: 'GIANG_VIEN',
    ngayGui: '2025-11-28T02:00:00Z',
    daDoc: false,
    ngayDoc: null,
  },
  {
    maThongBao: 'c3c3c3c3-3333-3333-3333-333333333333',
    loai: 'TU_DONG',
    suKien: 'CONG_BO_DIEM',
    mucDo: 'THONG_THUONG',
    tieuDe: 'Đã có điểm học phần Cơ sở dữ liệu (INT1313)',
    noiDung:
      'Giảng viên vừa công bố điểm học phần Cơ sở dữ liệu (INT1313). Mở bảng điểm để xem chi tiết.',
    lienKet: '/sinh-vien/bang-diem?maHocKy=2026-1',
    vaiTroNguoiGui: null,
    ngayGui: '2026-02-10T04:00:00Z',
    daDoc: false,
    ngayDoc: null,
  },
  {
    maThongBao: 'd4d4d4d4-4444-4444-4444-444444444444',
    loai: 'TU_DONG',
    suKien: 'DANG_KY',
    mucDo: 'THONG_THUONG',
    tieuDe: 'Đăng ký thành công lớp Giải tích 1 (BAS1201)',
    noiDung: 'Bạn đã đăng ký thành công lớp Giải tích 1 (BAS1201), học kỳ 1 năm học 2026-2027.',
    lienKet: '/sinh-vien/lich-hoc-hoc-ky?maHocKy=2026-1',
    vaiTroNguoiGui: null,
    ngayGui: '2026-02-01T03:00:00Z',
    daDoc: true,
    ngayDoc: '2026-02-01T05:00:00Z',
  },
  {
    maThongBao: 'e5e5e5e5-5555-5555-5555-555555555555',
    loai: 'SOAN',
    suKien: null,
    mucDo: 'THONG_THUONG',
    tieuDe: 'Mở đợt đăng ký bổ sung học kỳ 2 năm học 2026-2027',
    noiDung:
      'Phòng Đào tạo mở đợt đăng ký bổ sung từ 01/03 đến 05/03. Sinh viên chưa đủ tín chỉ tranh thủ đăng ký, quá hạn hệ thống không mở thêm.',
    lienKet: '/sinh-vien/dang-ky',
    vaiTroNguoiGui: 'ADMIN_CO_SO',
    ngayGui: '2026-02-25T10:00:00Z',
    daDoc: true,
    ngayDoc: '2026-02-26T01:00:00Z',
  },
]
