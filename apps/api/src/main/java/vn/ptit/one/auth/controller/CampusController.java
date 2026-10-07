package vn.ptit.one.auth.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ptit.one.auth.model.Campus;
import vn.ptit.one.auth.repository.AccountRepository;

/**
 * Danh sách cơ sở đang hoạt động — ô chọn cơ sở trên giao diện.
 *
 * <p>Cần cho hai chỗ: Admin Master chọn phạm vi thống kê, và cấp hồ sơ sinh
 * viên/giảng viên (F02) phải chọn cơ sở nhà.
 *
 * <p>Chỉ cần đăng nhập: tên cơ sở không phải thông tin cần che, và ai cũng
 * thấy được cơ sở của mình trên giao diện rồi. Phạm vi THAO TÁC vẫn do từng
 * endpoint nghiệp vụ kiểm theo JWT, không dựa vào danh sách này.
 */
@RestController
@RequestMapping("/api/campuses")
@Profile("central")
public class CampusController {

    private final AccountRepository accounts;

    public CampusController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<Campus> campuses() {
        return accounts.listCampuses();
    }
}
