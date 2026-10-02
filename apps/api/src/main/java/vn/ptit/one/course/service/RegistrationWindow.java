package vn.ptit.one.course.service;

import java.util.List;

/**
 * "Học kỳ nào đang mở đăng ký" — điều `course` cần biết trước khi cho đổi môn
 * tiên quyết.
 *
 * <p>Đợt đăng ký thuộc module {@code enrollment}, nhưng {@code enrollment} lại
 * phụ thuộc {@code course} (lớp, môn, tiên quyết). Interface này đảo chiều để
 * phụ thuộc chỉ đi một hướng {@code enrollment → course}.
 */
public interface RegistrationWindow {

    /** Học kỳ đang có đợt mở ở bất kỳ cơ sở nào, tính theo cả trạng thái và giờ. */
    List<String> openTerms();
}
