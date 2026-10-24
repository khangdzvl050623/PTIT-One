package vn.ptit.one.auth.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.shared.routing.SiteContext;

/**
 * Tra danh bạ ở Master để biết một tên đăng nhập thuộc cơ sở nào.
 *
 * <p>Bài toán chỉ có ở Phần 2: {@code TaiKhoan} được phân mảnh theo cơ sở, mà
 * lúc nhận tên đăng nhập thì chưa biết cơ sở nào — nên chưa biết hỏi database
 * nào. {@code DanhBaNguoiDung} ở Master là nơi duy nhất trả lời được, và đó
 * cũng chính là lý do nó tồn tại.
 *
 * <p>Chỉ có ở profile {@code distributed}. Profile {@code central} một
 * database thì không cần phân giải gì cả.
 */
@Component
@Profile("distributed")
public class LoginSiteResolver {

    private final AccountRepository accounts;

    public LoginSiteResolver(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /**
     * Cơ sở để xử lý đăng nhập cho {@code username}.
     *
     * <p>Rơi về {@link SiteContext#MASTER} khi không tra ra cơ sở — hoặc vì
     * tài khoản không tồn tại, hoặc vì đó là Admin Master. Hai trường hợp này
     * đều phải xử lý tại Master, nên gộp là đúng.
     *
     * <p>Với tài khoản không tồn tại, việc rơi về Master còn giữ nguyên hành
     * vi chống dò tài khoản: luồng đăng nhập vẫn băm một mật khẩu giả ở đó,
     * nên thời gian phản hồi không tiết lộ tài khoản nào có thật.
     */
    public String resolve(String username) {
        return SiteContext.runAt(SiteContext.MASTER,
                () -> accounts.findCampusOfUser(username).orElse(SiteContext.MASTER));
    }
}
