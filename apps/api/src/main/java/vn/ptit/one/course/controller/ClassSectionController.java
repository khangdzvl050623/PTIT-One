package vn.ptit.one.course.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.course.dto.AssignTeacherRequest;
import vn.ptit.one.course.dto.CreateClassRequest;
import vn.ptit.one.course.dto.UpdateClassRequest;
import vn.ptit.one.course.model.ClassSection;
import vn.ptit.one.course.service.ClassSectionService;

/**
 * Lớp học phần (F04).
 *
 * <p>Không có tham số {@code maCoSo}: phạm vi cơ sở lấy từ principal đã ký.
 * Lịch học nằm ở module {@code timetable} ({@code /api/classes/{id}/schedule}) —
 * tách ra để {@code course} không phụ thuộc ngược vào {@code timetable}.
 */
@RestController
@RequestMapping("/api/classes")
@Profile("central")
public class ClassSectionController {

    private final ClassSectionService classes;

    public ClassSectionController(ClassSectionService classes) {
        this.classes = classes;
    }

    @GetMapping
    public List<ClassSection> search(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String maHocKy,
            @RequestParam(required = false) String maMonHoc,
            @RequestParam(required = false) String maGiangVien) {
        return classes.search(user, maHocKy, maMonHoc, maGiangVien);
    }

    @GetMapping("/{maLopHP}")
    public ClassSection detail(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP) {
        return classes.detail(user, maLopHP);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassSection create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateClassRequest body) {
        return classes.create(user, body.maMonHoc(), body.maHocKy(), body.soLuongToiDa(),
                body.hinhThucHoc(), body.choPhepLienCoSo(), body.maGiangVien());
    }

    @PutMapping("/{maLopHP}")
    public ClassSection update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP, @Valid @RequestBody UpdateClassRequest body) {
        return classes.update(user, maLopHP, body.soLuongToiDa(), body.trangThai(),
                body.hinhThucHoc(), body.choPhepLienCoSo());
    }

    @PutMapping("/{maLopHP}/teacher")
    public ClassSection assignTeacher(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP, @RequestBody AssignTeacherRequest body) {
        return classes.assignTeacher(user, maLopHP, body.maGiangVien());
    }
}
