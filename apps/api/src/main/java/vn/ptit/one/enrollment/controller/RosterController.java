package vn.ptit.one.enrollment.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.AuthenticatedUser;
import vn.ptit.one.enrollment.model.ClassRoster;
import vn.ptit.one.enrollment.service.RosterService;

/** Danh sách sinh viên của lớp (F05). Cùng tiền tố {@code /api/classes} nhưng thuộc module ghi danh. */
@RestController
@RequestMapping("/api/classes")
@Profile("central")
public class RosterController {

    private final RosterService rosters;

    public RosterController(RosterService rosters) {
        this.rosters = rosters;
    }

    @GetMapping("/{maLopHP}/students")
    public ClassRoster students(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String maLopHP) {
        return rosters.roster(user, maLopHP);
    }
}
