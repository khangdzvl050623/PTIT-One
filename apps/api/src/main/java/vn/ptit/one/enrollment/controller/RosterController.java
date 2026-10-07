package vn.ptit.one.enrollment.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.enrollment.dto.RemoveEnrollmentRequest;
import vn.ptit.one.enrollment.model.ClassRoster;
import vn.ptit.one.enrollment.service.EnrollmentRemovalService;
import vn.ptit.one.enrollment.service.RosterService;

/** Danh sách sinh viên của lớp (F05). Cùng tiền tố {@code /api/classes} nhưng thuộc module ghi danh. */
@RestController
@RequestMapping("/api/classes")
@Profile("central")
public class RosterController {

    private final RosterService rosters;
    private final EnrollmentRemovalService removals;

    public RosterController(RosterService rosters, EnrollmentRemovalService removals) {
        this.rosters = rosters;
        this.removals = removals;
    }

    @GetMapping("/{maLopHP}/students")
    public ClassRoster students(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP) {
        return rosters.roster(user, maLopHP);
    }

    /**
     * Admin cơ sở gỡ một sinh viên khỏi lớp (F08).
     *
     * <p>Khác {@code POST /api/classes/{maLopHP}/cancel}: cái đó huỷ cả lớp.
     * Đây gỡ đúng một người, lớp vẫn mở và dôi ra một chỗ.
     *
     * <p>KHÔNG kiểm đợt đăng ký còn mở — đó chính là lý do endpoint này tồn
     * tại. Giới hạn nằm ở tình trạng lớp và ở việc đã có điểm chưa.
     *
     * <p>Dùng {@code POST} chứ không {@code DELETE}: cần thân request cho lý
     * do, và {@code DELETE} có thân thì một số proxy bỏ mất. Cùng dạng với
     * {@code POST /api/classes/{maLopHP}/cancel} đã có.
     *
     * @return lớp sau khi gỡ, để giao diện cập nhật sĩ số ngay
     */
    @PostMapping("/{maLopHP}/students/{maSinhVien}/remove")
    public ClassSection removeStudent(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP, @PathVariable String maSinhVien,
            @Valid @RequestBody RemoveEnrollmentRequest body) {
        return removals.remove(user, maLopHP, maSinhVien, body.lyDo());
    }
}
