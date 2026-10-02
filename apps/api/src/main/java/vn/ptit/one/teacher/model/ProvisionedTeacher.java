package vn.ptit.one.teacher.model;

import vn.ptit.one.auth.model.ActivationCode;

/** Hồ sơ vừa cấp kèm mã kích hoạt — mã chỉ xuất hiện trong response này. */
public record ProvisionedTeacher(Teacher giangVien, ActivationCode kichHoat) {
}
