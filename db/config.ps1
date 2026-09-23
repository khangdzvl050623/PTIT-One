# =====================================================================
#  PTIT One — Cấu hình topology
# =====================================================================
#  ĐÂY LÀ NƠI DUY NHẤT khai báo tên máy chủ và tên database.
#  Mọi script SQL nhận các giá trị này qua biến SQLCMD, không hardcode.
#
#  Đổi topology (thêm cơ sở, đổi tên máy, gộp về 1 máy) → sửa DUY NHẤT file này.
#
#  KHÔNG chứa mật khẩu. Mặc định dùng Windows Authentication (-E).
# =====================================================================

@{
    # -----------------------------------------------------------------
    # Máy chủ SQL Server.
    #
    # Phương án 3 máy (mặc định): MASTER nằm chung instance với HCM.
    # Phương án 4 máy          : đổi Master thành 'SRV-MASTER'.
    # Plan B — tất cả một máy  : dùng named instance, ví dụ '.\SITE_HN'.
    # -----------------------------------------------------------------
    Servers = @{
        MASTER = 'SRV-HCM'
        HCM    = 'SRV-HCM'
        HN     = 'SRV-HN'
        DN     = 'SRV-DN'
    }

    # -----------------------------------------------------------------
    # Tên database. Tiền tố PTITONE_ theo tên dự án.
    # -----------------------------------------------------------------
    Databases = @{
        MASTER = 'PTITONE_MASTER'
        HCM    = 'PTITONE_HCM'
        HN     = 'PTITONE_HN'
        DN     = 'PTITONE_DN'
    }

    # -----------------------------------------------------------------
    # Mã cơ sở vận hành. KHÔNG gồm MASTER — Master là vai trò, không phải cơ sở.
    # Thêm cơ sở mới: thêm mã vào đây, thêm mục ở Servers và Databases.
    # -----------------------------------------------------------------
    Sites = @('HCM', 'HN', 'DN')

    # -----------------------------------------------------------------
    # Replication
    # -----------------------------------------------------------------
    Replication = @{
        # ⚠️ BẮT BUỘC là UNC share, KHÔNG để đường dẫn local.
        #    Share phải nằm trên máy chạy DISTRIBUTOR, và tài khoản chạy
        #    SQL Server Agent phải đọc/ghi được. Đây là lỗi số một giết các nhóm.
        SnapshotFolder = '\\SRV-HCM\repldata'

        PublicationName = 'PUB_ThamChieu'

        # Hai tham số điều khiển HAI cơ chế hết hạn KHÁC NHAU:
        #   DistributionRetention : lệnh được GIỮ bao lâu trong distribution db
        #   SubscriptionRetention : subscription HẾT HẠN sau bao lâu không đồng bộ
        #
        # Đặt bằng nhau là QUY ƯỚC CỦA NHÓM cho dễ vận hành, không phải
        # yêu cầu của SQL Server. Đặt lệch nhau vẫn hợp lệ.
        #
        # ⚠️ Không coi con số này là bảo đảm "tắt N ngày vẫn bắt kịp".
        #    Nó là giới hạn TRÊN, còn thực tế phụ thuộc lịch chạy cleanup
        #    job và snapshot còn dùng được hay không. Vẫn phải giám sát
        #    bằng Replication Monitor.
        #    720 giờ = 30 ngày.
        DistributionRetentionHours = 720
        SubscriptionRetentionHours = 720
    }

    # -----------------------------------------------------------------
    # Linked Server — tên logic dùng trong T-SQL bốn phần
    # -----------------------------------------------------------------
    LinkedServers = @{
        HN = 'SRV_HN'
        DN = 'SRV_DN'
    }

    # Collation cho toàn bộ database
    Collation = 'Vietnamese_CI_AS'
}
