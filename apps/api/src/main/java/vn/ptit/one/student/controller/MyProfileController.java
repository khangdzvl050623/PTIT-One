package vn.ptit.one.student.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.student.dto.UpdateMyProfileRequest;
import vn.ptit.one.student.model.StudentDetail;
import vn.ptit.one.student.service.MyProfileService;

/**
 * Hồ sơ của sinh viên đang đăng nhập. Nằm dưới {@code /api/me} cùng các
 * endpoint "của tôi" khác (điểm, thời khoá biểu, đăng ký).
 */
@RestController
@RequestMapping("/api/me/profile")
@Profile("central")
public class MyProfileController {

    private final MyProfileService profiles;

    public MyProfileController(MyProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public StudentDetail myProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return profiles.myProfile(user);
    }

    /**
     * Sinh viên tự điền phần lý lịch và ảnh đại diện.
     *
     * <p>Cần **đã xác minh email**, nếu không trả `409 EMAIL_NOT_VERIFIED`.
     *
     * <p>Thay TOÀN BỘ phần lý lịch: ô bỏ trống là xoá giá trị cũ. Trường hành
     * chính (họ tên, ngày sinh, cơ sở, chương trình) không sửa được ở đây.
     */
    @PutMapping
    public StudentDetail updateMyProfile(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateMyProfileRequest body) {
        return profiles.updateMyProfile(user, body);
    }
}
