package vn.ptit.one.shared.routing;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Địa chỉ của bốn nút. Giá trị thật nằm ở biến môi trường, không ở file được
 * commit — xem {@code application-distributed.properties}.
 *
 * @param master   database danh mục; cũng là nơi mặc định khi chưa biết cơ sở
 * @param nodes    mã cơ sở (HCM/HN/DN) → kết nối của cơ sở đó
 * @param poolSize số kết nối tối đa cho MỖI nút. Nhỏ thôi: bốn pool nhân lên
 *                 thì tổng số kết nối tăng nhanh, mà tải của đồ án thì nhẹ
 */
@ConfigurationProperties("ptitone.sites")
public record SiteProperties(
        Node master,
        Map<String, Node> nodes,
        @DefaultValue("5") int poolSize) {

    /**
     * Một nút. Mỗi cơ sở có tài khoản riêng vì chúng là các máy khác nhau, do
     * người khác nhau quản — không giả định dùng chung một mật khẩu.
     */
    public record Node(String url, String username, String password) {
    }
}
