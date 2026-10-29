package vn.ptit.one.auth.security;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import vn.ptit.one.auth.model.RefreshToken;
import vn.ptit.one.auth.repository.AccountRepository;
import vn.ptit.one.auth.repository.RefreshTokenRepository;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.shared.routing.SiteProperties;

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

    private static final Logger log = LoggerFactory.getLogger(LoginSiteResolver.class);

    private final AccountRepository accounts;
    private final RefreshTokenRepository refreshTokens;
    private final SiteProperties sites;

    public LoginSiteResolver(AccountRepository accounts, RefreshTokenRepository refreshTokens, SiteProperties sites) {
        this.accounts = accounts;
        this.refreshTokens = refreshTokens;
        this.sites = sites;
    }

    /**
     * Nút đang giữ refresh token. Refresh chạy đúng lúc access token đã hết hạn,
     * nên không còn JWT nào cho biết cơ sở — chỉ còn cách hỏi từng nút. Bốn lượt
     * tìm theo khoá, mỗi 15 phút một lần cho mỗi người: rẻ.
     */
    public String siteOfRefreshToken(String presentedToken) {
        byte[] hash = RefreshToken.hashOf(presentedToken);
        List<String> nodes = new ArrayList<>(sites.nodes().keySet());
        nodes.add(0, SiteContext.MASTER);
        for (String site : nodes) {
            try {
                if (SiteContext.runAt(site, () -> refreshTokens.findByHash(hash).isPresent())) {
                    return site;
                }
            } catch (RuntimeException ex) {
                /* Một nút tắt không được chặn người dùng ở nút khác gia hạn phiên. */
                log.warn("Không hỏi được nút {} khi tìm refresh token: {}", site, ex.toString());
            }
        }
        return SiteContext.MASTER;
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

    /**
     * Cho luồng CHƯA đăng nhập ngoài login (kích hoạt, khôi phục mật khẩu):
     * không có JWT để định tuyến nên phải tra danh bạ. Tên không thuộc cơ sở
     * nào thì trả {@code ifNoSite} ngay — Master không có {@code MaKichHoat}/
     * {@code MaXacThuc}, chạy ở đó là 503 khác hẳn 400, thành công cụ dò tài khoản.
     */
    public <T> T atUserSite(String username, Supplier<T> body, T ifNoSite) {
        String site = resolve(username);
        return SiteContext.MASTER.equals(site) ? ifNoSite : SiteContext.runAt(site, body);
    }
}
