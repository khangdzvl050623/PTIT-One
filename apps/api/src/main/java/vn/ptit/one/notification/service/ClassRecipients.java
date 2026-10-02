package vn.ptit.one.notification.service;

import java.util.List;

/**
 * Sinh viên đang học một lớp học phần — người nhận của thông báo gửi cho lớp.
 *
 * <p>Ghi danh thuộc module {@code enrollment}, mà {@code enrollment} lại gọi sang
 * {@code notification} để phát thông báo đăng ký/huỷ. Interface này đảo chiều
 * (giống {@code RegistrationWindow} của {@code course}) để phụ thuộc chỉ đi một
 * hướng {@code enrollment → notification}.
 */
public interface ClassRecipients {

    /** Mã sinh viên còn giữ chỗ trong lớp. */
    List<String> studentsOf(String maLopHP);
}
