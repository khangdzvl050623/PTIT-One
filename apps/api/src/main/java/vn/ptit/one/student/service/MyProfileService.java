package vn.ptit.one.student.service;

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
import vn.ptit.one.student.dto.UpdateMyProfileRequest;
import vn.ptit.one.student.model.StudentDetail;
import vn.ptit.one.student.repository.StudentRepository;

/**
 * Hồ sơ của chính sinh viên đang đăng nhập.
 *
 * <p>Không có tham số mã sinh viên: mã lấy từ principal đã ký, nên không có
 * đường nào xem hồ sơ người khác. Muốn cho Admin xem hồ sơ sinh viên thì mở
 * endpoint riêng có kiểm quyền riêng, đừng thêm tham số vào đây.
 */
@Service
@Profile("central")
public class MyProfileService {

    private final StudentRepository students;
    private final AccountService accounts;
    private final CloudinaryUploader cloudinary;

    public MyProfileService(StudentRepository students, AccountService accounts,
            CloudinaryUploader cloudinary) {
        this.students = students;
        this.accounts = accounts;
        this.cloudinary = cloudinary;
    }

    private void requireVerifiedEmail(AuthenticatedUser user) {
        if (!accounts.emailVerified(user.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_NOT_VERIFIED",
                    "Cần xác minh email trước khi sửa hồ sơ. Vào Tài khoản > Email để xác minh.");
        }
    }

    public StudentDetail myProfile(AuthenticatedUser user) {
        return students.findDetail(requireStudent(user))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND",
                        "Không có hồ sơ sinh viên %s.".formatted(user.entityId())));
    }

    /**
     * Sinh viên tự sửa phần lý lịch của mình.
     *
     * <p><b>Bắt buộc đã xác minh email</b> (quyết định nhóm 07/10/2026). Hồ sơ
     * là dữ liệu định danh; cho sửa khi chưa có một kênh liên lạc đã kiểm chứng
     * thì không truy được ai đã đổi, và tài khoản bị chiếm cũng sửa được. Xác
     * minh email là mốc tối thiểu để gắn thao tác với một người thật.
     *
     * <p>Thay TOÀN BỘ phần lý lịch: ô bỏ trống là xoá giá trị cũ. Không vá từng
     * ô vì giao diện gửi cả biểu mẫu, và vá từng ô sẽ không bao giờ xoá được.
     *
     * <p>Những trường hành chính (họ tên, ngày sinh, cơ sở, chương trình, trạng
     * thái) KHÔNG nằm trong yêu cầu — Phòng Đào tạo quản.
     */
    @Transactional
    public StudentDetail updateMyProfile(AuthenticatedUser user, UpdateMyProfileRequest body) {
        String maSinhVien = requireVerifiedStudent(user);

        if (students.updateProfile(maSinhVien, trim(body.gioiTinh()), trim(body.dienThoai()),
                trim(body.soCCCD()), trim(body.emailCaNhan()), trim(body.noiSinh()),
                trim(body.danToc()), trim(body.tonGiao()), trim(body.hoKhau())) != 1) {
            throw notFound(maSinhVien);
        }
        return students.findDetail(maSinhVien).orElseThrow(() -> notFound(maSinhVien));
    }

    /**
     * Thay ảnh đại diện bằng file người dùng chọn.
     *
     * <p>Thứ tự có chủ ý: kiểm xong rồi **đẩy lên Cloudinary trước**, ghi DB
     * sau. Ngược lại thì DB có thể trỏ tới ảnh chưa bao giờ tồn tại. Đổi lại,
     * nếu ghi DB hỏng thì có một ảnh thừa trên Cloudinary — nhưng ảnh đó mang
     * đúng {@code public_id} của sinh viên này nên lần tải sau ghi đè lên nó,
     * không tích rác.
     *
     * <p>Định dạng nhận theo **byte đầu file**, không theo {@code Content-Type}
     * hay đuôi tên: cả hai thứ đó do client đặt.
     */
    @Transactional
    public StudentDetail updateAvatar(AuthenticatedUser user, byte[] bytes) {
        String maSinhVien = requireVerifiedStudent(user);
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Chưa chọn ảnh.");
        }
        ImageKind kind = ImageKind.of(bytes);
        if (kind == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_FORMAT_INVALID",
                    "Chỉ nhận ảnh JPEG, PNG, GIF, WEBP hoặc BMP.");
        }
        if (!students.exists(maSinhVien)) {
            throw notFound(maSinhVien);
        }

        String url = cloudinary.upload(maSinhVien, bytes, kind);
        if (students.updateAvatar(maSinhVien, url) != 1) {
            throw notFound(maSinhVien);
        }
        return students.findDetail(maSinhVien).orElseThrow(() -> notFound(maSinhVien));
    }

    /**
     * Xoá ảnh đại diện.
     *
     * <p>Xoá DB trước, Cloudinary sau: hỏng ở bước sau thì chỉ còn một file
     * không ai trỏ tới, còn làm ngược lại sẽ để hồ sơ trỏ vào ảnh đã mất.
     */
    @Transactional
    public StudentDetail removeAvatar(AuthenticatedUser user) {
        String maSinhVien = requireVerifiedStudent(user);
        if (students.updateAvatar(maSinhVien, null) != 1) {
            throw notFound(maSinhVien);
        }
        cloudinary.delete(maSinhVien);
        return students.findDetail(maSinhVien).orElseThrow(() -> notFound(maSinhVien));
    }

    /** Hai cổng của mọi thao tác tự sửa hồ sơ: đúng vai sinh viên, và email đã xác minh. */
    private String requireVerifiedStudent(AuthenticatedUser user) {
        String maSinhVien = requireStudent(user);
        requireVerifiedEmail(user);
        return maSinhVien;
    }

    /** Chuỗi rỗng hoặc toàn khoảng trắng = bỏ trống, lưu NULL chứ không lưu ''. */
    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String cut = value.trim();
        return cut.isEmpty() ? null : cut;
    }

    private static String requireStudent(AuthenticatedUser user) {
        if (user.role() != Role.SINH_VIEN || user.entityId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ sinh viên mới có hồ sơ sinh viên.");
        }
        return user.entityId();
    }

    private static ApiException notFound(String maSinhVien) {
        return new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND",
                "Không có hồ sơ sinh viên %s.".formatted(maSinhVien));
    }
}
