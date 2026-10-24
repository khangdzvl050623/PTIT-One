<#
    PTIT One — config.local.psd1 (BẢN MẪU)
    ---------------------------------------------------------------------
    Chép file này thành `config.local.psd1` cùng thư mục rồi điền.
    `.gitignore` đã chặn `config.local.psd1` — ĐỪNG commit nó.

    VÌ SAO CẦN FILE NÀY:
    `run.ps1` mặc định dùng Windows Authentication (`-E`). Cách đó chỉ chạy
    được với SQL Server nằm trên CHÍNH máy này, hoặc các máy cùng domain.
    Bốn máy của nhóm nằm trong workgroup và nối nhau qua Radmin VPN, nên
    mọi máy site đều phải đăng nhập bằng SQL Server login.

    Khai site nào ở đây thì `run.ps1` dùng `-U/-P` cho site đó. Site không
    khai thì vẫn dùng `-E` như cũ — nhờ vậy máy Master (cục bộ) không phải
    khai gì cả.
#>
@{
    Logins = @{
        # Tài khoản do bạn giữ máy tạo theo hướng dẫn cài đặt:
        #     CREATE LOGIN ptitone_admin WITH PASSWORD = '...', CHECK_POLICY = OFF;
        #     ALTER SERVER ROLE sysadmin ADD MEMBER ptitone_admin;
        #
        # HN = @{ User = 'ptitone_admin'; Password = '<mat khau ban HN dat>' }
        # DN = @{ User = 'ptitone_admin'; Password = '<mat khau ban DN dat>' }
        # HCM = @{ User = 'ptitone_admin'; Password = '<mat khau ban HCM dat>' }
    }
}
