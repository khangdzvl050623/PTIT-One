/* =====================================================================
   PTIT One — 90-demo-nhan-ban.sql
   ---------------------------------------------------------------------
   NHÌN TẬN MẮT nhân bản một chiều Master → HCM đang chạy.

   File này KHÔNG cài đặt gì cả. Nó chỉ thêm một dòng dữ liệu thử, cho bạn
   xem dòng đó tự chạy sang bản sao, rồi xoá đi. Chạy lại bao nhiêu lần
   cũng được.

   ---------------------------------------------------------------------
   CÁCH CHẠY TRONG SSMS
   ---------------------------------------------------------------------
   1. Mở file này trong SSMS, nối vào DESKTOP-85V5Q0S\PTITONE.
      KHÔNG cần bật SQLCMD Mode. KHÔNG cần chọn database nào ở ô dropdown —
      mọi câu lệnh đều ghi rõ tên database đầy đủ.
   2. ⭐ ĐỪNG bấm F5 cả file. Hãy BÔI ĐEN TỪNG KHỐI rồi bấm F5, đọc kết
      quả, hiểu xong mới sang khối sau. Mỗi khối kết thúc bằng chữ GO.

   ---------------------------------------------------------------------
   ⚠️ VÌ SAO FILE NÀY GHI THẲNG TÊN DATABASE
   ---------------------------------------------------------------------
   Mọi script cài đặt trong db/ đều lấy tên database từ db/config.ps1 qua
   biến SQLCMD, và tuyệt đối không hardcode. File này là NGOẠI LỆ CÓ CHỦ Ý,
   vì hai lý do đo được:

     1. SSMS KHÔNG giữ giá trị :setvar giữa hai lần bấm F5. Chạy từng khối
        một — đúng cách dùng của file này — thì khối nào cũng báo
        "Variable DbMaster is not defined".
     2. Khi chạy qua sqlcmd, :setvar trong file lại ĐÈ LÊN -v của dòng lệnh
        (đã thử nghiệm). Nên biến SQLCMD ở đây không hề giúp đổi đích được:
        nó ghim cứng giá trị y như hardcode, mà còn bắt bật SQLCMD Mode.

   Tức là biến SQLCMD chỉ thêm rắc rối chứ không mua được tính linh hoạt
   nào. Đây là file để ĐỌC và BẤM TAY, không phải bước cài đặt.

   👉 Bạn ở HN hoặc DN: Ctrl+H, thay toàn bộ PTITONE_HCM thành PTITONE_HN
      (hoặc PTITONE_DN). Đó là thay đổi duy nhất cần làm.
   ===================================================================== */

/* =====================================================================
   PHẦN 1 — AI LÀ BẢN GỐC, AI LÀ BẢN SAO

   Bạn sẽ thấy 25 dòng chia ba nhóm:

     "1. Nhan ban tu Master" → 9 bảng có mặt ở CẢ HAI database.
                               Sửa ở Master, HCM tự có theo.
     "2. Chi o Master"       → TaiKhoanMaster. CỐ TÌNH không nhân bản:
                               đó là tài khoản của Admin Master, site
                               không cần và không được thấy.
     "3. Chi o HCM"          → 15 bảng nghiệp vụ của riêng cơ sở HCM.
                               Sinh viên, lớp học phần, đăng ký… Master
                               KHÔNG hề biết chúng tồn tại.

   Đây chính là hình dạng của cả đồ án: dữ liệu dùng chung thì nhân bản,
   dữ liệu nghiệp vụ thì phân mảnh về từng cơ sở.
   ===================================================================== */

SELECT  CASE WHEN m.name IS NOT NULL AND h.name IS NOT NULL
                 THEN N'1. Nhan ban tu Master'
             WHEN m.name IS NOT NULL
                 THEN N'2. Chi o Master'
             ELSE N'3. Chi o HCM'
        END                              AS [Nhom],
        COALESCE(m.name, h.name)         AS [Bang]
  FROM       (SELECT name FROM PTITONE_MASTER.sys.tables WHERE is_ms_shipped = 0) m
  FULL JOIN  (SELECT name FROM PTITONE_HCM.sys.tables WHERE is_ms_shipped = 0) h
         ON  h.name = m.name
 /* Bỏ qua bảng metadata do chính replication tạo ra ở Subscriber
    (MSreplication_subscriptions, MSsubscription_agents…) — chúng là
    đồ nghề của SQL Server, không phải bảng của mình. */
 WHERE  COALESCE(m.name, h.name) NOT LIKE 'MS%'
   AND  COALESCE(m.name, h.name) NOT LIKE 'sqlagent%'
   AND  COALESCE(m.name, h.name) NOT LIKE 'queue_messages%'
 ORDER BY [Nhom], [Bang];
GO

/* =====================================================================
   PHẦN 2 — XEM NHÂN BẢN CHẠY NGAY TRƯỚC MẮT

   Ba khối 2a → 2b → 2c. Chạy từng khối, đọc kết quả rồi mới sang khối sau.
   ===================================================================== */

/* --- 2a. Thêm một khoa mới vào BẢN GỐC (Master). ---------------------
   Chú ý: ta ghi vào PTITONE_MASTER, KHÔNG ghi vào HCM. */

INSERT PTITONE_MASTER.dbo.Khoa (MaKhoa, TenKhoa)
VALUES ('DEMO', N'Khoa Thu Nghiem');

SELECT N'Vua ghi vao BAN GOC' AS [Buoc], TenKhoa
  FROM PTITONE_MASTER.dbo.Khoa WHERE MaKhoa = 'DEMO';
GO

/* --- 2b. Đếm NGAY LẬP TỨC ở bản sao HCM. -----------------------------
   Thường vẫn là 0: dòng đó chưa kịp chạy sang. Đây là "nhất quán cuối" —
   bản sao đuổi theo bản gốc sau một khoảng trễ, không tức thời.

   Nếu bạn bấm chậm quá và đã thấy 1 luôn thì cũng không sao, chỉ nghĩa
   là nhân bản nhanh hơn tay bạn. */

SELECT N'Ngay lap tuc' AS [Buoc],
       COUNT(*)        AS [HCM_da_thay_DEMO_chua]
  FROM PTITONE_HCM.dbo.Khoa WHERE MaKhoa = 'DEMO';
GO

/* --- 2c. Đợi 15 giây rồi đếm lại. ------------------------------------
   Bây giờ phải là 1. Bạn KHÔNG hề gõ INSERT nào vào HCM cả — SQL Server
   tự chuyển sang. Đó là toàn bộ ý nghĩa của nhân bản.

   SSMS sẽ đứng im 15 giây, đừng bấm Stop.

   Nếu vẫn là 0: không hỏng gì cả, chỉ là hôm nay máy chậm hơn thường lệ.
   Bôi đen lại khối 2b rồi F5 vài lần, sẽ thấy nó nhảy lên 1. Độ trễ là
   biến thiên, không phải hằng số — chính vì vậy phần 3 mới phải đo bằng
   công cụ chuẩn thay vì bấm giờ bằng tay. */

WAITFOR DELAY '00:00:15';

SELECT N'Sau 15 giay' AS [Buoc],
       COUNT(*)       AS [HCM_da_thay_DEMO_chua]
  FROM PTITONE_HCM.dbo.Khoa WHERE MaKhoa = 'DEMO';
GO

/* =====================================================================
   PHẦN 3 — ĐO ĐỘ TRỄ BẰNG CON SỐ CHÍNH THỨC

   "Tracer token" là một dấu mốc rỗng SQL Server gửi xuyên qua đường ống
   nhân bản rồi bấm giờ từng chặng. Đây là cách đo chuẩn, và là số liệu
   để đưa vào benchmark B6 của báo cáo.

   Đọc kết quả:
     distributor_latency = từ Master tới Distributor (mấy giây)
     subscriber_latency  = từ Distributor tới HCM
     overall_latency     = tổng — đây là con số đưa vào báo cáo

   Khối này chờ 20 giây. Nếu subscriber_latency trả về NULL nghĩa là dấu
   mốc chưa tới nơi — chạy lại cả khối sau vài giây nữa.
   ===================================================================== */

DECLARE @token INT;

EXEC PTITONE_MASTER.sys.sp_posttracertoken
     @publication      = 'PUB_ThamChieu',
     @tracer_token_id  = @token OUTPUT;

WAITFOR DELAY '00:00:20';

EXEC PTITONE_MASTER.sys.sp_helptracertokenhistory
     @publication = 'PUB_ThamChieu',
     @tracer_id   = @token;
GO

/* =====================================================================
   PHẦN 4 — CHỨNG MINH NHÂN BẢN LÀ *MỘT CHIỀU*

   Phần quan trọng nhất của cả file. Chạy 4a → 4b → 4c theo thứ tự.
   ===================================================================== */

/* --- 4a. Thử sửa ở BẢN SAO (HCM). ------------------------------------
   ⚠️ Lệnh này HIỆN TẠI CHẠY ĐƯỢC — và đó là một thiếu sót đã biết:
      site/14-role-grant.sql (chưa viết) mới là chỗ đặt
      DENY INSERT/UPDATE/DELETE lên 9 bảng nhân bản.
      Chạy nó ở đây chính là để bạn thấy tận mắt vì sao cần DENY đó. */

UPDATE PTITONE_HCM.dbo.Khoa
   SET TenKhoa = N'TEN NAY DO HCM TU SUA'
 WHERE MaKhoa = 'DEMO';
GO

/* --- 4b. So hai bên. -------------------------------------------------
   BẢN GỐC KHÔNG HỀ ĐỔI. Thay đổi ở bản sao không chảy ngược về Master —
   đó là ý nghĩa của "một chiều". Không có merge, không có xử lý xung đột. */

SELECT N'MASTER (ban goc)' AS [Noi], TenKhoa
  FROM PTITONE_MASTER.dbo.Khoa WHERE MaKhoa = 'DEMO'
UNION ALL
SELECT N'HCM (ban sao)',            TenKhoa
  FROM PTITONE_HCM.dbo.Khoa WHERE MaKhoa = 'DEMO';
GO

/* --- 4c. Giờ sửa ở BẢN GỐC. ------------------------------------------
   Ít giây sau, hai bên lại giống nhau — và cái tên bạn vừa tự sửa ở HCM
   BỊ GHI ĐÈ, MẤT TRẮNG, không một lời cảnh báo.

   Đây là lý do kỹ thuật khiến Subscriber PHẢI chỉ đọc: ghi vào bản sao
   không sai ngay, nó sai âm thầm vào một lúc nào đó sau này.

   (Nếu HCM vẫn còn hiện tên cũ, chạy lại khối 4b — lệnh ghi đè đang trên
   đường sang. Chậm vài giây không đổi kết luận: sớm muộn Master cũng thắng.) */

UPDATE PTITONE_MASTER.dbo.Khoa
   SET TenKhoa = N'Ten do Master quyet dinh'
 WHERE MaKhoa = 'DEMO';

WAITFOR DELAY '00:00:15';

SELECT N'MASTER (ban goc)' AS [Noi], TenKhoa
  FROM PTITONE_MASTER.dbo.Khoa WHERE MaKhoa = 'DEMO'
UNION ALL
SELECT N'HCM (ban sao)',            TenKhoa
  FROM PTITONE_HCM.dbo.Khoa WHERE MaKhoa = 'DEMO';
GO

/* ⚠️⚠️ ĐỪNG THỬ: xoá một dòng nhân bản ở HCM.
   UPDATE ở bản sao thì bị ghi đè rồi thôi. Nhưng nếu bạn DELETE một dòng
   ở HCM mà sau đó Master lại UPDATE chính dòng đó, Distribution Agent
   tìm không thấy dòng để sửa và NGỪNG TOÀN BỘ NHÂN BẢN với lỗi "row not
   found". Lúc đó phải khởi tạo lại subscription mới chạy tiếp được.
   Biết để tránh, đừng biểu diễn. */

/* =====================================================================
   PHẦN 5 — NHÌN VÀO BÊN TRONG ĐƯỜNG ỐNG

   Database `distribution` là cái ống dẫn giữa Master và các site.
   Mỗi thay đổi ở Master biến thành "lệnh" nằm chờ trong đó, rồi
   Distribution Agent đẩy sang từng subscriber và ghi lại nhật ký.

   ⚠️ ĐỌC TRƯỚC KHI CHẠY — hai cái bẫy ở phần này:

   1. Nhật ký ĐẦY DÒNG RÁC. Distribution Agent cứ 5 phút lại ghi một dòng
      thống kê dạng <stats state="1" work="7" idle="6913">… Không lọc thì
      TOP 10 toàn là mấy dòng đó, không đọc được gì. Câu dưới đã lọc.

   2. Dòng "N transaction(s) with M command(s) were delivered." là DÒNG
      TRẠNG THÁI HIỆN TẠI, agent ghi đè liên tục. Chạy xong PHẦN 2/PHẦN 4
      một lúc lâu rồi mới xem thì nó đã bị thay bằng "No replicated
      transactions are available." (= đang rảnh, không còn gì để đẩy).

      👉 Muốn thấy nó: chạy khối 2a rồi chạy NGAY khối này.
      Còn thấy dòng "Delivered snapshot…" và "Bulk copied data into
      table…" thì đó là lần nạp dữ liệu ban đầu — cũng đáng xem.

   Tên `distribution` do SQL Server đặt, không phải tên database của dự án,
   nên ở HN/DN cũng giữ nguyên như vậy.
   ===================================================================== */
SELECT TOP 10
       CONVERT(VARCHAR(19), [time], 120) AS [Luc],
       LEFT(comments, 150)               AS [Distribution_Agent_noi_gi]
  FROM distribution.dbo.MSdistribution_history
 /* Bỏ dòng thống kê 5 phút/lần, chỉ giữ dòng người đọc được. */
 WHERE comments NOT LIKE '<stats%'
 ORDER BY [time] DESC;

/* --- Có lệnh nào đang XẾP HÀNG chưa giao tới HCM không? --------------
   Đây mới là con số "tồn đọng" thật. Hệ thống khoẻ thì nó bằng 0.
   Nếu số này lớn dần mà không tụt: site đang tắt, hoặc Distribution
   Agent chết — vào Replication Monitor xem ngay.

   (Ở HN/DN: @publisher phải là tên máy Master, không phải @@SERVERNAME.) */
DECLARE @mayNay SYSNAME = @@SERVERNAME;

EXEC distribution.dbo.sp_replmonitorsubscriptionpendingcmds
     @publisher         = @mayNay,
     @publisher_db      = 'PTITONE_MASTER',
     @publication       = 'PUB_ThamChieu',
     @subscriber        = @mayNay,
     @subscriber_db     = 'PTITONE_HCM',
     @subscription_type = 0;          -- 0 = push

/* --- Còn đây KHÔNG phải hàng đợi, đừng đọc nhầm. ---------------------
   MSrepl_commands là các lệnh distribution database còn GIỮ LẠI theo
   retention 720 giờ, kể cả lệnh đã giao xong từ lâu. Nó tồn tại để một
   site tắt vài ngày bật lên còn bắt kịp, không phải để đo tồn đọng.
   Số này khác 0 trong khi tồn đọng ở trên bằng 0 là hoàn toàn bình thường. */
SELECT COUNT(*) AS [So_lenh_con_giu_theo_retention]
  FROM distribution.dbo.MSrepl_commands;
GO

/* =====================================================================
   PHẦN 6 — DỌN DẸP

   Xoá dòng thử ở BẢN GỐC. Lệnh xoá cũng chảy sang HCM y như lệnh thêm.
   Sau khối này cả hai bên đều còn đúng 5 khoa như trước khi bạn bắt đầu.
   ===================================================================== */

DELETE PTITONE_MASTER.dbo.Khoa WHERE MaKhoa = 'DEMO';

WAITFOR DELAY '00:00:15';

SELECT (SELECT COUNT(*) FROM PTITONE_MASTER.dbo.Khoa) AS [Master_so_khoa],
       (SELECT COUNT(*) FROM PTITONE_HCM.dbo.Khoa) AS [HCM_so_khoa],
       (SELECT COUNT(*) FROM PTITONE_HCM.dbo.Khoa
         WHERE MaKhoa = 'DEMO')                      AS [Con_sot_DEMO];
GO

/* =====================================================================
   XEM BẰNG GIAO DIỆN, KHÔNG CẦN GÕ LỆNH

   Trong Object Explorer của SSMS (nối vào DESKTOP-85V5Q0S\PTITONE):

     Replication → Local Publications → [PTITONE_MASTER]: PUB_ThamChieu
        → mở ra thấy đúng 9 article và 1 subscription tới PTITONE_HCM
        → chuột phải → View Snapshot Agent Status

     Chuột phải Replication → Launch Replication Monitor
        → cửa sổ theo dõi chính thức: mọi mục phải xanh

     SQL Server Agent → Jobs
        → 3 job của replication: Snapshot Agent, Log Reader Agent,
          Distribution Agent. Tên job Snapshot Agent là
          DESKTOP-85V5Q0S\PTITONE-PTITONE_MASTER-PUB_ThamChieu-1
          — để ý: TRONG TÊN KHÔNG HỀ CÓ CHỮ "Snapshot".
          Chính chỗ này từng làm script 31/32 dò trượt job.
   ===================================================================== */
