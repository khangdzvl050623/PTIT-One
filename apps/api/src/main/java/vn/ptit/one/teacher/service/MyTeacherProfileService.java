package vn.ptit.one.teacher.service;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.auth.model.Role;
import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.shared.exception.ApiException;
import vn.ptit.one.shared.media.CloudinaryUploader;
import vn.ptit.one.shared.media.ImageKind;
import vn.ptit.one.teacher.dto.UpdateMyTeacherProfileRequest;
import vn.ptit.one.teacher.model.TeacherDetail;
import vn.ptit.one.teacher.repository.TeacherRepository;

/**
 * Hồ sơ của chính giảng viên đang đăng nhập.
 *
 * <p>Song song với {@code MyProfileService} của sinh viên, cùng ba quy tắc:
 * mã lấy từ principal đã ký (không có tham số mã giảng viên, nên không có
 * đường xem hồ sơ người khác); sửa lý lịch cần **email đã xác minh**; và thay
 * toàn bộ phần lý lịch chứ không vá từng ô.
 *
 * <p>Hai service không gộp được vì hai bảng khác nhau, khác bộ cột hành chính
 * và khác module sở hữu. Phần giống nhau là quy tắc, không phải code.
 */
@Service
@Profile("central")
public class MyTeacherProfileService {

    private final TeacherRepository teachers;
    private final AccountService accounts;
    private final CloudinaryUploader cloudinary;

    public MyTeacherProfileService(TeacherRepository teachers, AccountService accounts,
            CloudinaryUploader cloudinary) {
        this.teachers = teachers;
        this.accounts = accounts;
        this.cloudinary = cloudinary;
    }

    public TeacherDetail myProfile(AuthenticatedUser user) {
        return teachers.findDetail(requireTeacher(user)).orElseThrow(() -> notFound(user.entityId()));
    }

    @Transactional
    public TeacherDetail updateMyProfile(AuthenticatedUser user, UpdateMyTeacherProfileRequest body) {
        String maGiangVien = requireVerifiedTeacher(user);
        if (teachers.updateProfile(maGiangVien, trim(body.gioiTinh()), trim(body.dienThoai()),
                trim(body.soCCCD()), trim(body.emailCaNhan()), trim(body.noiSinh()),
                trim(body.danToc()), trim(body.tonGiao()), trim(body.hoKhau())) != 1) {
            throw notFound(maGiangVien);
        }
        return teachers.findDetail(maGiangVien).orElseThrow(() -> notFound(maGiangVien));
    }

    /** Đẩy ảnh lên trước, ghi DB sau — xem ghi chú cùng tên ở {@code MyProfileService}. */
    @Transactional
    public TeacherDetail updateAvatar(AuthenticatedUser user, byte[] bytes) {
        String maGiangVien = requireVerifiedTeacher(user);
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Chưa chọn ảnh.");
        }
        ImageKind kind = ImageKind.of(bytes);
        if (kind == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_FORMAT_INVALID",
                    "Chỉ nhận ảnh JPEG, PNG, GIF, WEBP hoặc BMP.");
        }
        if (!teachers.exists(maGiangVien)) {
            throw notFound(maGiangVien);
        }

        String url = cloudinary.upload(maGiangVien, bytes, kind);
        if (teachers.updateAvatar(maGiangVien, url) != 1) {
            throw notFound(maGiangVien);
        }
        return teachers.findDetail(maGiangVien).orElseThrow(() -> notFound(maGiangVien));
    }

    @Transactional
    public TeacherDetail removeAvatar(AuthenticatedUser user) {
        String maGiangVien = requireVerifiedTeacher(user);
        if (teachers.updateAvatar(maGiangVien, null) != 1) {
            throw notFound(maGiangVien);
        }
        cloudinary.delete(maGiangVien);
        return teachers.findDetail(maGiangVien).orElseThrow(() -> notFound(maGiangVien));
    }

    private String requireVerifiedTeacher(AuthenticatedUser user) {
        String maGiangVien = requireTeacher(user);
        if (!accounts.emailVerified(user.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_NOT_VERIFIED",
                    "Cần xác minh email trước khi sửa hồ sơ. Vào Tài khoản > Email để xác minh.");
        }
        return maGiangVien;
    }

    private static String requireTeacher(AuthenticatedUser user) {
        if (user.role() != Role.GIANG_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ giảng viên mới có hồ sơ giảng viên.");
        }
        return user.entityId();
    }

    /** Chuỗi rỗng hoặc toàn khoảng trắng = bỏ trống, lưu NULL chứ không lưu ''. */
    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String cut = value.trim();
        return cut.isEmpty() ? null : cut;
    }

    private static ApiException notFound(String maGiangVien) {
        return new ApiException(HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND",
                "Không có hồ sơ giảng viên %s.".formatted(maGiangVien));
    }
}
