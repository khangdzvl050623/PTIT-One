package vn.ptit.one.teacher.controller;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.teacher.dto.UpdateMyTeacherProfileRequest;
import vn.ptit.one.teacher.model.TeacherDetail;
import vn.ptit.one.teacher.service.MyTeacherProfileService;

/**
 * Hồ sơ của giảng viên đang đăng nhập.
 *
 * <p>Đường riêng chứ không dùng chung {@code /api/me/profile} với sinh viên:
 * hai vai trả hai hình dạng khác nhau, gộp một đường sẽ buộc client phải tự
 * đoán kiểu theo vai trò. Cùng cách {@code /api/me/teaching-classes} và
 * {@code /api/me/grades} đã tách sẵn.
 */
@RestController
@RequestMapping("/api/me/teacher-profile")
@Profile("central")
public class MyTeacherProfileController {

    private final MyTeacherProfileService profiles;

    public MyTeacherProfileController(MyTeacherProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public TeacherDetail myProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return profiles.myProfile(user);
    }

    /**
     * Giảng viên tự điền phần lý lịch.
     *
     * <p>Cần **đã xác minh email**, nếu không trả `409 EMAIL_NOT_VERIFIED`.
     * Thay TOÀN BỘ phần lý lịch: ô bỏ trống là xoá giá trị cũ.
     */
    @PutMapping
    public TeacherDetail updateMyProfile(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateMyTeacherProfileRequest body) {
        return profiles.updateMyProfile(user, body);
    }

    /** Nhận file thật; server đẩy lên kho ảnh rồi tự ghi URL — xem `/api/me/profile/avatar`. */
    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeacherDetail uploadAvatar(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("file") MultipartFile file) throws IOException {
        return profiles.updateAvatar(user, file.getBytes());
    }

    @DeleteMapping("/avatar")
    public TeacherDetail removeAvatar(@AuthenticationPrincipal AuthenticatedUser user) {
        return profiles.removeAvatar(user);
    }
}
