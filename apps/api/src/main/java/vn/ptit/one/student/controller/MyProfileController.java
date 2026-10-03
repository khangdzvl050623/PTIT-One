package vn.ptit.one.student.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
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
}
