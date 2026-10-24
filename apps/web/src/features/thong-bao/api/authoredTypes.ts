/**
 * Bản do chính mình soạn — mirror `AuthoredNotification` của backend (module
 * `notification`). Server tự lọc theo vai trò nên màn hình không lọc gì thêm.
 */
export interface AuthoredNotice {
  maThongBao: string
  /** `NHAP` (nháp) · `DA_GUI` (đã gửi). */
  trangThai: string
  mucDo: string
  tieuDe: string
  noiDung: string
  /** Đường dẫn nội bộ (`/…`) hoặc `null`. */
  lienKet: string | null
  /** `TOAN_TRUONG` · `CO_SO` · `LOP_HOC_PHAN`. */
  phamVi: string
  maCoSo: string | null
  maLopHP: string | null
  doiTuong: string
  ngayTao: string
  /** `null` khi còn là nháp. */
  ngayGui: string | null
  /** Nháp: số DỰ KIẾN tính lại; đã gửi: số đã chốt lúc gửi. */
  nguoiNhan: {
    soSinhVien: number
    soGiangVien: number
  }
  /** `0` với bản nháp. */
  soDaDoc: number
}

/** Đã gửi thật chưa — nháp thì chưa có `ngayGui`. */
export function daGui(notice: AuthoredNotice): boolean {
  return notice.trangThai === 'DA_GUI'
}

/** Tổng người nhận (dự kiến với nháp, đã chốt với bản gửi). */
export function tongNguoiNhan(notice: AuthoredNotice): number {
  return notice.nguoiNhan.soSinhVien + notice.nguoiNhan.soGiangVien
}
