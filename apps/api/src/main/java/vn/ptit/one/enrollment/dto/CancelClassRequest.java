package vn.ptit.one.enrollment.dto;

import jakarta.validation.constraints.Size;

/** Lý do huỷ, không bắt buộc; được đưa vào thông báo gửi sinh viên và giảng viên. */
public record CancelClassRequest(@Size(max = 500) String lyDo) {
}
