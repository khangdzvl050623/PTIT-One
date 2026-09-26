# PTIT One kế hoạch UI UX và chức năng cơ bản

Ngày cập nhật: 23/09/2026. **Đây là bản kế hoạch chính dùng cho nhóm.** Kế hoạch được chia thành hai phần: Phần 1 triển khai UI/UX và nghiệp vụ trên **một database tập trung**; Phần 2 liên quan CSDL phân tán, sẽ lập kế hoạch sau.

Giữ khung 8 tuần chung của dự án, không chia sprint cố định theo từng tuần. Bản này chỉ phân công và nghiệm thu Phần 1, chưa phân bổ số tuần giữa hai phần. Mỗi người tự ước lượng và cam kết hạn bàn giao của mình theo phụ thuộc; không mặc định dành trọn 8 tuần cho Phần 1\. Chưa có tên thành viên và ngày bắt đầu/hạn nộp nên dùng TV1–TV6, không tự gán ngày.

# Phần 1 UI UX và chức năng cơ bản trên một CSDL tập trung

## 1\. Kết quả cần đạt

Một ứng dụng PTIT One dùng được từ trình duyệt, có giao diện cho sinh viên, giảng viên và quản trị. Người dùng đăng nhập/đăng ký tài khoản theo chính sách cấp hồ sơ; SV xem điểm, đăng ký/hủy học phần và xem thời khóa biểu; GV xem lớp, danh sách/sĩ số và nhập điểm; Admin cấu hình môn tiên quyết, lớp và đợt đăng ký. Nghiệm thu bằng dữ liệu lưu trong một database SQL Server thật. Import điểm Excel là phần làm thêm khi các luồng cơ bản ổn định.

**Kiến trúc Phần 1:** `Trình duyệt → React/Vite → API Spring Boot → PTITONE_CENTRAL`.

**Mô hình làm việc: mỗi máy chạy đủ bộ.** Không dùng VPS, không cần VPN cho việc hằng ngày. Ai viết code đều cài SQL Server + API + web trên máy mình, mỗi người một `PTITONE_CENTRAL` riêng dùng chung migration và seed. Cách cài: [hướng dẫn cài Phần 1](PTIT-One-Cai-Dat-Phan-1.md). Các DB cá nhân này là môi trường phát triển độc lập, không phải site phân tán và không đồng bộ với nhau.

`PTITONE_CENTRAL` là tên đề xuất cho database tập trung mới, chưa được tạo. Toàn bộ tài khoản, danh mục, môn tiên quyết, hồ sơ SV/GV, lớp, lịch, ghi danh và điểm cùng nằm trong database này. Backend chỉ cấu hình **một DataSource**, các transaction đều là transaction cục bộ. Đăng nhập không cần phân giải database theo cơ sở; sửa môn/tiên quyết có hiệu lực sau commit, không chờ nhân bản.

Giữ các trường mã cơ sở đang có nếu cần để tái sử dụng schema và làm metadata, nhưng Phần 1 chỉ thử luồng cùng cơ sở, không có chức năng đăng ký liên cơ sở. Bảng hoặc tên role có chữ Master không có nghĩa ứng dụng kết nối thêm database.

## 2\. Tận dụng dự án hiện có

Đã đối chiếu cả bốn file Markdown trong `D:\javabtap\uisptitv2\docs` với nội dung UC/TC đã đọc trước đó:

| Tài liệu | Nội dung dùng cho Phần 1 | Điều chỉnh theo yêu cầu hiện tại |
| :---- | :---- | :---- |
| `PTIT-One-Thiet-Ke.md` | B1/B3: chức năng và quyền; C1: schema và quy tắc học vụ; D4: giao dịch/tương tranh; J1/J2: tổ chức API/web | Dùng một DB và một DataSource. UI/UX, domain, API và quy tắc nghiệp vụ được làm trước; cơ chế phân tán chưa thuộc Phần 1 |
| `PTIT-One-Sprint-6-Nguoi.md` | Sáu vai trò, owner/reviewer, đầu ra, cách lưu bằng chứng | Thay lịch sprint tuần bằng gói chức năng, phụ thuộc và mốc kết quả. Cổng G3 cũ không chặn triển khai Phần 1 theo yêu cầu mới |
| `PTIT-One-Setup-HCM.md` | Nhật ký ngày 10/09: SQL Server đã cài, schema/seed và replication cục bộ đã được ghi nhận | Tận dụng SQL Server và nội dung schema; chuẩn bị database tập trung độc lập. Nhật ký cũ không chứng minh database mới đã tồn tại |
| `PTIT-One-Cai-Dat-May-Moi.md` | Thông tin môi trường SQL Server, collation, cách kiểm tra và quản lý cấu hình | Hướng dẫn này dành cho HN/ĐN và snapshot, không dùng nguyên quy trình để dựng database tập trung. Chi tiết nhiều máy chuyển sang Phần 2 |

**Điểm lệch tài liệu cần lưu ý:** hướng dẫn máy mới còn ghi chưa chọn riêng được Subscriber, nhưng nhật ký setup và runner hiện có `-Subscribers`. Đây là việc chỉnh tài liệu khi chuẩn bị Phần 2, không phải đầu việc chặn Phần 1\. Công thức điểm, ngưỡng đạt và trần tín chỉ trong thiết kế vẫn được đánh dấu giả định.

### Cách chuẩn bị database tập trung

TV1 và TV2 lập bộ script riêng cho chế độ tập trung, dự kiến ở `db/central/`, rồi dựng `PTITONE_CENTRAL` trên SQL Server hiện có. Tái sử dụng định nghĩa bảng/khóa/dữ liệu mẫu phù hợp, không chỉ đổi tên DB trong toàn bộ script `master/` và `site/`: các script đó có kiểm tra đích chạy và phụ thuộc snapshot.

Các nhóm dữ liệu cần có:  
c

- Danh mục: `CoSo`, `Khoa`, `ChuongTrinhDaoTao`, `CTDT_MonHoc`, `MonHoc`, `MonHocTienQuyet`, `HocKy`, `KhungGioTiet`.  
- Hồ sơ và xác thực: `SinhVien`, `GiangVien`, `TaiKhoan`; có thể giữ `DanhBaNguoiDung` và `TaiKhoanMaster` trong **cùng database** để giảm thay đổi schema. Chốt ánh xạ tài khoản/role và không trùng tên đăng nhập trong contract F01.  
- Vận hành: `DotDangKy`, `LopHocPhan`, `LichHoc`, `DangKyHocPhan`, `Diem`, `SinhVienHocKy`, `DangKyMonHoc`.  
- Dữ liệu kích hoạt tài khoản: thiết kế bổ sung tối thiểu ở F02 nếu schema hiện tại thiếu; ghi rõ migration và chính sách sử dụng.

Phần 1 không cần bảng saga, Outbox hoặc mirror để chạy. Không đưa routing nhiều DataSource, Linked Server, MS DTC, replication hay trạng thái đồng bộ vào điều kiện khởi động/đăng nhập/nghiệp vụ của bản tập trung. Không có yêu cầu gỡ hệ thống Master/HCM cũ để lập hoặc thực hiện kế hoạch này.

Giữ tên bảng/cột nghiệp vụ, mã định danh ổn định và tách SQL khỏi controller. Quy tắc tiên quyết, điểm, lịch và tín chỉ nằm ở lớp nghiệp vụ, tránh gắn cứng vào địa chỉ SQL Server; đây là phần có thể dùng lại về sau mà không phải hiện thực cơ chế phân tán ngay.

## 3\. Phân công sáu người cho Phần 1

| Người | Việc tự chịu trách nhiệm | Đầu ra phải bàn giao | Người kiểm lại |
| :---- | :---- | :---- | :---- |
| TV1(KHẢI PHÁT) | Môi trường một DB, cấu hình chạy API/web, quyền kết nối, hướng dẫn cài và sao lưu/khôi phục bản demo | Hướng dẫn dựng `PTITONE_CENTRAL`, cấu hình mẫu, kiểm kết nối, cách chạy bản tích hợp | TV2 |
| TV2 | Schema tập trung, migration/seed, ràng buộc, SQL nghiệp vụ và đăng ký/hủy nguyên tử | Script, tham số/kết quả thủ tục, fixture và truy vấn đối soát | TV4; TV1 kiểm dựng lại |
| TV3 | Yêu cầu, nội dung UI, UC/TC, quy tắc học vụ, hồ sơ kiểm thử và hướng dẫn dùng | Ma trận yêu cầu–chức năng–test, nội dung thông báo, bằng chứng và kịch bản demo | TV4 |
| TV4 | Luồng nghiệp vụ, hợp đồng API, ma trận quyền, kiểm thử tích hợp và điều phối nghiệm thu | Contract được các bên review, ca thử, lỗi có owner và biên bản nghiệm thu | TV5; chủ phần liên quan đối soát |
| TV5(KHANG) | Backend tập trung, xác thực, API học vụ, transaction và xử lý lỗi | API chạy thật trên một DB, kiểm thử và ví dụ request/response | TV4; TV2 kiểm tác động DB |
| TV6(KHẢI) | UI/UX, prototype, bộ component, màn hình và tích hợp API | Thiết kế luồng, UI responsive, trạng thái tương tác, kết quả kiểm UI | TV4 kiểm luồng; TV5 kiểm contract; TV3 kiểm câu chữ |

TV3 không kiêm code. Người thực hiện kỹ thuật tự chạy và lưu bằng chứng, TV3 tập hợp. TV4 không sửa thay các module. TV5/TV6 bàn giao theo từng chức năng; không chờ toàn bộ backend hoặc toàn bộ UI hoàn thành mới ghép.

## 4\. Gói UI UX cần hoàn thành

| Mã | Gói việc | Owner / reviewer | Phụ thuộc | Đầu ra và nghiệm thu |
| :---- | :---- | :---- | :---- | :---- |
| U01 | Luồng người dùng và cấu trúc màn hình | TV6 / TV4, TV3 | Danh sách chức năng và role; không chờ DB/API | Sơ đồ điều hướng SV/GV/Admin, thao tác chính, trạng thái thành công/lỗi/rỗng; không thiếu bước đăng ký, hủy hoặc công bố điểm |
| U02 | Bộ quy chuẩn giao diện và component dùng chung | TV6 / TV4 | Hướng giao diện và cấu trúc U01 | Màu, chữ, khoảng cách, Button/Field/Table/Dialog/Badge, navigation và thông báo nhất quán; dùng lại ở mọi màn |
| U03 | Prototype và màn hình tĩnh theo vai trò | TV6 / TV4, TV3 | U01/U02; hợp đồng dữ liệu từng màn | Prototype bấm được các luồng chính; thể hiện lỗi nghiệp vụ, loading/empty/error và responsive; dữ liệu mẫu có nhãn |

UI/UX đi cùng việc triển khai chức năng. Khi U02 có bộ component đầu tiên, TV6 có thể dựng và nối từng màn; không cần chờ mọi prototype hoàn tất. Một prototype được duyệt không đồng nghĩa chức năng đã chạy với database.

### Danh sách màn hình tối thiểu

| Nhóm | Màn hình cần có | Điểm UX cần thể hiện |
| :---- | :---- | :---- |
| Chung | Đăng nhập, đăng ký/kích hoạt, thông tin tài khoản và đăng xuất | Label/lỗi rõ, trạng thái đang gửi, hết phiên, không cho tự chọn quyền quản trị |
| SV | Danh sách lớp mở, chi tiết lớp, các lớp đã đăng ký | Môn, tín chỉ, lịch, GV, sĩ số/còn chỗ; lý do thiếu tiên quyết/trùng lịch/vượt tín chỉ; phản hồi khi đăng ký/hủy |
| SV | Bảng điểm và thời khóa biểu | Chọn học kỳ; “Chưa có điểm” khác 0; lịch tuần và danh sách, phòng và tiết dễ đọc |
| GV | Lớp phụ trách, danh sách SV/sĩ số, bảng nhập điểm | Tìm SV, ô điểm hợp lệ, phân biệt lưu nháp/công bố/đã khóa, cảnh báo còn thay đổi chưa lưu |
| Admin | Hồ sơ/tài khoản tối thiểu, danh mục môn/tiên quyết | Đặt nhiều tiên quyết, báo môn gây chu trình; cấp quyền theo vai trò, không phải theo việc UI đang mở |
| Admin | Lớp học phần, phân công GV, lịch và đợt đăng ký | Mở/đóng đợt rõ ràng; cảnh báo dữ liệu thiếu, sức chứa hoặc lịch không hợp lệ |

**Yêu cầu UX chung:** thao tác được bằng bàn phím, focus/label rõ; lỗi không chỉ biểu đạt bằng màu; bảng dài có tìm/lọc phù hợp; trên màn nhỏ không che nút chính. Nút đang xử lý hạn chế bấm lặp nhưng backend vẫn phải chống ghi trùng. Giữ dữ liệu form khi lỗi có thể sửa, xác nhận trước hủy đăng ký/công bố điểm, tải lại dữ liệu liên quan sau commit. Không đưa thuật ngữ database, site, replica hoặc đồng bộ vào luồng người dùng của bản tập trung.

Các quyền quản trị giữ tương thích thiết kế cũ: `ADMIN_MASTER` quản lý danh mục/tiên quyết, `ADMIN_CO_SO` quản lý hồ sơ/lớp/đợt. UI gọi là Quản trị danh mục và Quản trị đào tạo; cả hai truy cập cùng một DB. SV/GV vẫn chỉ đọc hoặc sửa bản ghi thuộc quyền của mình.

## 5\. Chức năng và quan hệ phụ thuộc

“Contract” là hợp đồng dữ liệu/API gồm field, kiểu dữ liệu, tham số, role, trạng thái và lỗi. “Fixture” là dữ liệu thử có thể tái tạo. Cột bắt đầu cho phép làm sớm theo contract/fixture; cột nghiệm thu yêu cầu các phần liên quan đã chạy thật. Mọi gói mới ban đầu là Backlog.

| Mã | Gói chức năng | Chủ trì / kiểm lại | Cần trước khi bắt đầu | Cần để nghiệm thu |
| :---- | :---- | :---- | :---- | :---- |
| F00 | Một DB tập trung, seed, contract và khung ứng dụng | TV4 / TV1 | Thiết kế nghiệp vụ và môi trường SQL Server | Dựng được `PTITONE_CENTRAL`, API một DataSource, web gọi được API |
| F01 | Đăng nhập, đăng xuất và phân quyền | TV5 / TV4 | Contract tài khoản F00, tài khoản seed | F00; xác thực và quyền theo bản ghi thật |
| F02 | Hồ sơ tối thiểu và đăng ký/kích hoạt tài khoản | TV5 / TV4 | Schema F00; contract F01; chính sách cấp tài khoản | F01; kích hoạt một lần rồi đăng nhập được |
| F03 | Danh mục môn, kỳ, chương trình và tiên quyết | TV2 / TV4 | Schema/contract F00 | F01; Admin sửa cùng DB, chặn dữ liệu sai/chu trình |
| F04 | Lớp học phần, phân công GV, lịch và đợt đăng ký | TV5 / TV4 | Contract F03; fixture môn/kỳ/GV | F01 \+ F03; có hồ sơ GV hợp lệ từ seed hoặc F02; lớp/đợt được tạo và dùng thật |
| F05 | GV xem lớp, danh sách SV và sĩ số | TV6 / TV4 | Contract F01/F04; fixture ghi danh | F01 \+ F04; đọc đúng lớp của GV. Ca thay đổi sĩ số chờ F08 |
| F06 | Nhập, công bố và khóa điểm | TV5 / TV4 | Contract F05; schema điểm/quy tắc điểm; fixture ghi danh | F01 \+ F04 \+ F05; lưu/công bố/khóa và kiểm quyền thật |
| F07 | SV xem bảng điểm | TV6 / TV4 | Contract F01/F06; fixture điểm | F01 và dữ liệu thật; ca điểm mới công bố cần F06 |
| F08 | Đăng ký và hủy học phần | TV2 / TV4 | Contract F01/F03/F04; schema điểm F06; fixture đủ điều kiện | F01 \+ F03 \+ F04, điểm đã công bố; kiểm transaction/tương tranh. Demo liên thông cần F06 |
| F09 | SV xem thời khóa biểu | TV6 / TV4 | Contract F04/F08; fixture lịch và ghi danh | F01 \+ F04; ca thêm/bớt lịch sau đăng ký/hủy cần F08 |
| X01 | Import điểm Excel nếu đủ thời gian | TV6 / TV4 | Contract và validation F06; mẫu file | F06; xem trước, báo lỗi từng dòng, lưu đúng quyền |
| H01 | UC/TC, nội dung, hướng dẫn và bằng chứng | TV3 / TV4 | Yêu cầu hiện có; đầu ra của từng người | Theo từng gói; ghi rõ đã làm/chưa làm/giả định |
| H02 | Tích hợp và bàn giao Phần 1 | TV4 / TV3 | Có từng luồng hoàn chỉnh để bắt đầu ghép | U01–U03, F00–F09, H01; không phụ thuộc X01 hoặc Phần 2 |

F04 có thể dùng GV seed trước, nhưng bàn giao toàn Phần 1 phải có F02 để quản trị thêm hồ sơ/tài khoản tối thiểu theo luồng đã chốt. X01 không chặn H02. Tất cả màn chức năng sử dụng U02; U03 được hoàn thiện cuốn chiếu cùng chức năng.

### Sơ đồ thứ tự tích hợp

flowchart TD

    U01\["U01 Luồng và cấu trúc màn hình"\] \--\> U02\["U02 Component và quy chuẩn UI"\]

    U02 \--\> U03\["U03 Prototype theo từng chức năng"\]

    F00\["F00 Một DB và contract"\] \--\> F01\["F01 Đăng nhập và quyền"\]

    F01 \--\> F02\["F02 Hồ sơ và kích hoạt tài khoản"\]

    F01 \--\> F03\["F03 Danh mục và tiên quyết"\]

    F03 \--\> F04\["F04 Lớp, lịch, GV và đợt"\]

    F04 \--\> F05\["F05 GV xem lớp và sĩ số"\]

    F05 \--\> F06\["F06 Nhập và công bố điểm"\]

    F06 \--\> F07\["F07 Xem điểm cập nhật"\]

    F03 \--\> F08\["F08 Đăng ký và hủy"\]

    F04 \--\> F08

    F06 \-. "Dữ liệu điểm; có thể seed khi phát triển" .-\> F08

    F08 \--\> F09\["F09 Lịch cập nhật theo đăng ký"\]

    F08 \-. "Kiểm lại sĩ số sau ghi" .-\> F05

    F06 \--\> X01\["X01 Import Excel tùy chọn"\]

    U03 \-. "Ghép UI theo từng chức năng" .-\> H02\["H02 Bàn giao Phần 1"\]

    F02 \--\> H02

    F07 \--\> H02

    F09 \--\> H02

Mũi tên liền chỉ thứ tự tích hợp chính, không bắt buộc chờ hoàn tất UI tiền nhiệm mới viết phần tiếp theo. Mũi tên nét đứt chỉ đầu vào dữ liệu hoặc kiểm lại sau tích hợp; F08 → F05 không tạo vòng chờ, vì F05/F06 bắt đầu bằng ghi danh seed. Bảng phụ thuộc phía trên là danh sách đầy đủ.

**Các việc làm song song:** U01/U02 đi cùng F00; F01 dùng tài khoản seed không chờ F02; F05 dùng ghi danh seed không chờ F08; F07 dùng điểm seed không chờ UI nhập điểm; F08 dùng điểm công bố seed không chờ F06 xong; F09 dựng trước theo contract lịch. Nghiệm thu liên thông phải thay mock bằng API/DB thật. TV3 cập nhật H01 xuyên suốt, không đợi code xong.

## 6\. Việc cụ thể và nghiệm thu từng chức năng

### F00 Nền tập trung

**TV1:** môi trường SQL Server, tạo DB, quyền và cấu hình ngoài mã nguồn, hướng dẫn chạy/backup. **TV2:** bộ schema/migration/seed tập trung, bảng và khóa cần dùng, dữ liệu thử. **TV4:** contract, ma trận quyền và tiêu chí lỗi. **TV5:** khung backend, một DataSource, xử lý lỗi chung. **TV6:** khung web/API client dùng chung. **TV3:** ghi khác biệt so với thiết kế nhiều DB cũ.

**Nghiệm thu:** người khác chạy theo hướng dẫn được; mọi API truy cập một DB; dựng mới không cần snapshot/Agent/site khác. Seed có tài khoản cho SV, hai GV và hai nhóm quyền Admin; có SV đạt/trượt/chưa có điểm, lớp trống/đầy, lịch trùng/khác tuần. Script chạy lại không nhân đôi fixture hoặc xóa dữ liệu ngoài fixture. Tài khoản chạy API không dùng sysadmin.

**Bàn giao:** bộ script, file cấu hình mẫu, cách dựng lại, contract đầu tiên, fixture có mã rõ và phiên bản code/schema khớp nhau.

Các đầu việc chuẩn bị được tách rõ ở phụ lục A: ENV-01 đến ENV-05 hỗ trợ F00/F01; ENV-06 phục vụ bản demo H02; ENV-07 cập nhật hướng dẫn H01. Đây là thẻ con của các gói hiện có, không phải sprint bổ sung.

### F01 Đăng nhập đăng xuất và quyền

**TV2:** dữ liệu và truy vấn tài khoản. **TV5:** xác thực, băm mật khẩu, phiên/JWT, `/me`, logout và quyền từng bản ghi; chốt cách vô hiệu phiên khi logout. **TV6:** giao diện đăng nhập, điều hướng theo role, lỗi và hết phiên. **TV4:** thử trực tiếp API, không chỉ thử nút trên UI.

**Nghiệm thu:** các vai trò vào đúng chức năng; sai mật khẩu có lỗi chung; SV không đọc/sửa điểm/lịch của người khác; GV không thao tác lớp khác; Admin chỉ thao tác phần được cấp. Tài khoản quản trị và tài khoản cơ sở nếu lưu hai bảng vẫn cùng `PTITONE_CENTRAL`; không tra một database khác để xác thực.

**Bàn giao:** cấu trúc danh tính/role, contract phiên, tài khoản thử, mã lỗi và các ca sai quyền cho F02–F09.

### F02 Hồ sơ và đăng ký kích hoạt tài khoản

**Phạm vi đề xuất:** Admin đào tạo cấp hồ sơ SV/GV và tài khoản tối thiểu; SV đăng ký/kích hoạt trên hồ sơ được cấp bằng thông tin kích hoạt dùng một lần. Chỉ biết mã SV không đủ để nhận tài khoản, không cho tự nhận vai trò GV/Admin. Đây là giả định nghiệp vụ bổ sung so với UC/TC cũ, phải ghi trong contract trước khi code.

**TV2:** schema lưu hồ sơ, trạng thái kích hoạt và mã băm/hạn dùng nếu cần bổ sung. **TV5:** tạo hồ sơ, cấp/kích hoạt, chống trùng; các thay đổi tài khoản/danh bạ/trạng thái cùng transaction trong một DB. **TV6:** form quản trị tối thiểu và màn SV kích hoạt. **TV3:** mô tả cách cấp mã demo; chưa cần email/SMS.

**Nghiệm thu:** tạo hồ sơ hợp lệ; kích hoạt một lần và đăng nhập được; mã sai/hết hạn/đã dùng bị từ chối; không chiếm hồ sơ hoặc tăng quyền; lỗi giữa các bước không để lại tài khoản nửa hoàn tất; không hiển thị trạng thái chờ đồng bộ. Khóa/ngừng tài khoản phải được API kiểm theo chính sách phiên đã chốt.

**Bàn giao:** fixture hợp lệ/sai/hết hạn, hướng dẫn cấp thông tin kích hoạt và test tạo trùng/rollback. Không lấy tài khoản seed làm bằng chứng đã hoàn thành F02.

### F03 Danh mục và môn tiên quyết

**TV2:** schema/truy vấn/thủ tục cho danh mục và đồ thị tiên quyết. **TV5:** API, quyền, validation và transaction. **TV6:** tìm/sửa môn, chọn nhiều môn tiên quyết, xem/xóa/thay tập phụ thuộc. **TV3:** nội dung quy tắc.

**Nghiệm thu:** đặt `(THCS2, THCS1)` nghĩa là học THCS2 cần đạt THCS1; nhiều tiên quyết mặc định phải đạt tất cả. Chặn tự phụ thuộc, chu trình dài và hai lần sửa đồng thời tạo chu trình. Thay tập tiên quyết là một transaction, lỗi giữ nguyên tập cũ. Không xóa môn đang được tham chiếu làm mất lịch sử; danh mục và tiên quyết đọc lại được ngay sau commit.

**Quy tắc bản đầu:** không thay tiên quyết của môn có đợt đăng ký đang mở; thao tác mở đợt và sửa tiên quyết phải kiểm cùng trạng thái trong cơ chế transaction/khóa đã thống nhất để không lọt khi làm đồng thời. Không tự hủy đăng ký cũ khi đổi chính sách. Điều kiện OR hoặc học song hành nằm ngoài basic.

**Bàn giao:** danh mục môn/kỳ/CTĐT, tập tiên quyết, contract và fixture cho F04/F08. Không có bước “đợi HCM nhận danh mục”.

### F04 Lớp lịch giảng viên và đợt đăng ký

**TV2:** dữ liệu/ràng buộc lớp, buổi học và đợt. **TV5:** API tạo/sửa/mở/đóng, quyền quản trị đào tạo. **TV6:** form và danh sách quản trị. **TV4:** ca dữ liệu sai và thay đổi khi đã có đăng ký.

**Nghiệm thu:** môn/kỳ/GV tồn tại; sức chứa hợp lệ; phân công GV; lịch theo thứ/tiết/khoảng tuần/phòng; đợt mở trước đóng và thuộc kỳ hợp lệ. Không hạ sức chứa dưới sĩ số thực. Phát hiện trùng GV/phòng với dữ liệu phòng được chuẩn hóa; khóa sửa lịch ảnh hưởng đăng ký đang hiệu lực trong bản basic. Chưa làm tự động chuyển lịch hàng loạt.

**Bàn giao:** lớp/GV/lịch/đợt mẫu, contract đọc và trạng thái. Với khóa lịch hiện tại, một bộ lớp/thứ/tiết bắt đầu chỉ có một khoảng tuần; nếu cần nhiều khoảng tách rời phải ghi thay đổi schema trước.

### F05 GV xem lớp danh sách và sĩ số

**TV2:** truy vấn lớp/ghi danh/đối soát. **TV5:** API theo danh tính GV. **TV6:** lọc học kỳ, danh sách lớp/SV, tìm mã hoặc tên và sĩ số/còn chỗ.

**Nghiệm thu:** chỉ thấy lớp được phân công; danh sách đúng ghi danh còn hiệu lực; sĩ số khớp đối soát; có trường hợp rỗng/lỗi. Sửa ID lớp trong request không mở được lớp của GV khác. Sau F08 đăng ký/hủy, tải lại thấy sĩ số/danh sách đúng.

**Bàn giao:** contract lớp/SV và fixture ghi danh cho F06; đọc dữ liệu không cần chờ màn đăng ký tín chỉ hoàn tất.

### F06 Nhập công bố và khóa điểm

**TV3:** ghi nguồn hoặc giả định công thức/ngưỡng/học lại. **TV2:** lưu điểm, version, công bố/khóa và ràng buộc. **TV5:** tính điểm phía server, quyền theo lớp, transaction và phát hiện sửa đồng thời. **TV6:** bảng nhập điểm, lỗi từng ô, lưu nháp/công bố/đã khóa.

**Nghiệm thu:** chỉ nhập cho SV có ghi danh; điểm trong 0–10; NULL khác 0; GV chỉ sửa lớp mình. Điểm nháp chưa lộ cho SV, công bố có thời điểm và dữ liệu hợp lệ; lớp khóa không sửa. Hai lần sửa cùng version không ghi đè âm thầm. Chốt người được khóa và điều kiện khóa trong contract; mở khóa/cải chính riêng ngoài basic.

**Bàn giao:** schema và contract điểm, quy tắc công bố, version, fixture biên cho F07/F08/X01. Công thức 10/30/60, ngưỡng 4.0 và trần 24 trong tài liệu nguồn chỉ dùng như cấu hình demo khi chưa xác minh; không trình bày thành quy chế chính thức.

### F07 SV xem điểm

**TV2:** query điểm đã công bố theo SV/kỳ. **TV5:** API lấy danh tính từ phiên. **TV6:** bảng điểm, lọc kỳ, kết quả đạt/chưa đạt và “Chưa có điểm”.

**Nghiệm thu:** chỉ xem điểm của mình; điểm nháp không xuất hiện; không biến NULL thành 0; kết quả đạt đúng chính sách. F06 công bố xong thì SV tải lại thấy điểm mới ngay từ cùng DB. Chưa thêm GPA/xếp hạng khi chưa chốt công thức.

**Bàn giao:** contract, màn hình và ca quyền; có thể phát triển bằng fixture trước UI nhập điểm.

### F08 Đăng ký và hủy học phần

**TV2:** transaction/thủ tục SQL, khóa, bộ đếm và đối soát. **TV5:** use case/API, danh tính, lỗi và xử lý gửi lặp. **TV6:** tìm lớp, điều kiện đăng ký, kết quả, danh sách đã đăng ký và hủy. **TV4:** kiểm đồng thời/rollback.

**Nghiệm thu nghiệp vụ:** SV hoạt động, đúng kỳ/CTĐT, đợt mở và lớp hợp lệ; đã đạt mọi môn tiên quyết bằng điểm đã công bố; không trùng môn, trùng lịch theo cả thứ/tiết/khoảng tuần; không vượt tín chỉ và sức chứa. Báo rõ nguyên nhân từ chối, ví dụ “Chưa đạt THCS1”. Backend tự kiểm, không tin cờ đủ điều kiện do client gửi.

**Nghiệm thu transaction:** khóa SV/kỳ trước các phép kiểm; UPDATE sức chứa có điều kiện; ghi danh, quyền môn và tín chỉ cùng transaction `PTITONE_CENTRAL`; lỗi rollback toàn bộ. Mọi luồng dùng thứ tự khóa lớp trước ghi danh; không trigger tăng sĩ số thêm lần nữa. Đăng ký cục bộ chỉ báo thành công sau commit; trạng thái đang gửi của UI không đòi một saga.

**Hủy bản đầu:** chỉ khi đợt cho phép và chưa có điểm; trả chỗ/tín chỉ, cập nhật quyền môn cùng transaction. PK SV/kỳ/môn hiện có yêu cầu tái sử dụng dòng quyền môn đã hủy khi đăng ký lại. Không xóa điểm để hủy. Điểm thiếu/nháp không được coi là đã đạt tiên quyết; học lại dùng chính sách đã ghi nhận, không xóa kết quả cũ.

**Ca bắt buộc:** SV chưa đạt/đã đạt/nhiều tiên quyết; hai lớp cùng môn; lịch cùng tiết khác tuần; hết đợt; vượt tín chỉ; lớp đầy; double-click; hủy rồi đăng ký lại; lỗi giữa transaction. Với lớp trống 30 chỗ và 100 SV khác nhau đều hợp lệ, đúng 30 ghi danh thành công và đối soát không lệch. Test này là kiểm tương tranh một DB, vẫn cần trong Phần 1\.

**Bàn giao:** contract đăng ký/hủy, mã lỗi, fixture, log đồng thời và đối soát cho F05/F09. Phát triển được bằng điểm seed; demo liên thông phải dùng F06.

### F09 SV xem thời khóa biểu

**TV2:** query lịch từ ghi danh còn hiệu lực. **TV5:** API theo SV/kỳ/tuần. **TV6:** lịch tuần và dạng danh sách, môn/GV/phòng/tiết, trạng thái rỗng.

**Nghiệm thu:** đúng SV/kỳ/tuần; không có môn đã hủy; đăng ký thành công F08 thì lịch xuất hiện, hủy hợp lệ thì biến mất sau tải lại; không truy cập lịch người khác. Tất cả dữ liệu đọc từ DB tập trung, không cần mirror hoặc thời điểm đồng bộ.

**Bàn giao:** contract lịch, màn hình và các ca ghép F08; có thể dựng UI bằng fixture trước.

### X01 Import điểm Excel nếu còn thời gian

**TV6:** upload và xem trước. **TV5:** đọc file, validation dùng chung F06, xác nhận lưu. **TV2:** hỗ trợ transaction. **TV4:** file hợp lệ/lỗi/trùng.

**Nghiệm thu:** GV chọn lớp trước; mẫu có mã SV và điểm thành phần; chặn SV ngoài lớp, dòng trùng, điểm sai; báo lỗi từng dòng. Đề xuất bản đầu chỉ lưu khi toàn file hợp lệ, kiểm lại quyền/khóa/version lúc lưu; không tự công bố ngay khi import. F06 phải hoàn thành trước khi nghiệm thu X01.

## 7\. Mốc nghiệm thu của Phần 1

| Mốc | Kết quả cần thấy | Người xác nhận |
| :---- | :---- | :---- |
| M0 Thiết kế và nền chạy | U01/U02 có đầu ra dùng được; F00 dựng được một DB, seed, API/web; contract ban đầu đã review | TV4, TV1; TV6 bàn giao UI |
| M1 Luồng cơ bản đọc và quản trị | Auth/quyền, danh mục/lớp/lịch/đợt và màn đọc bằng dữ liệu DB hoạt động; U03 bao phủ các màn chính | TV4; từng chủ chức năng sửa lỗi |
| M2 Luồng nghiệp vụ liên thông | F01–F09 chạy đủ, có tài khoản mới, điểm công bố, tiên quyết, đăng ký/hủy, lịch và sĩ số cập nhật; sai quyền/tương tranh/rollback đạt | TV4; TV2/TV5 đối soát |
| M3 Bàn giao Phần 1 | Hướng dẫn dựng lại, migration/seed, API, UI, test, bằng chứng và hạn chế đầy đủ; người khác chạy theo được | TV4 điều phối, TV3 đóng gói, TV1 kiểm chạy |

Không gắn các mốc với tuần cố định; không yêu cầu bất kỳ hạng mục phân tán nào để đạt M3. X01 là tùy chọn. Hoàn tất Phần 1 chưa đồng nghĩa hoàn tất toàn bộ đồ án CSDL phân tán.

**Demo nghiệm thu:** Admin cấp hồ sơ SV → SV kích hoạt và đăng nhập → Admin cấu hình THCS2 yêu cầu THCS1, lớp/kỳ/đợt → SV chưa đạt bị từ chối → GV công bố điểm đạt THCS1 của kỳ trước → SV xem điểm rồi đăng ký THCS2 kỳ hiện tại → lịch SV và sĩ số GV cập nhật → hủy hợp lệ trả chỗ/tín chỉ. Thử thêm lỗi quyền, lớp đầy, trùng lịch, điểm khóa và request đồng thời.

## 8\. Cách mỗi người tự nhận và hoàn thành việc

Tách gói thành thẻ có **một owner**: F08.DB của TV2, F08.API của TV5, F08.UI của TV6, F08.TEST của TV4, F08.DOC của TV3. Chủ trì gói theo dõi việc ghép các thẻ, không phải tự làm mọi tầng.

Mã và tên thẻ:

Owner / reviewer:

Phạm vi phải làm:

Phụ thuộc trước khi bắt đầu: \[thẻ \+ contract/schema/fixture cần nhận\]

Phần có thể làm trước bằng mock/fixture:

Phụ thuộc để nghiệm thu tích hợp:

Đầu ra bàn giao: \[script/API/màn hình/test/hướng dẫn\]

Tiêu chí nghiệm thu: \[đầu vào, thao tác, kết quả đúng và ca lỗi\]

Ước lượng và hạn owner tự cam kết:

Kết quả tự kiểm:

Bằng chứng: \[PR/commit/log/ảnh/cách chạy lại\]

Trạng thái: Backlog / Ready / In progress / In review / Done / Blocked

Nếu Blocked: thiếu gì, cần ai bàn giao, ảnh hưởng thẻ nào

Ready khi đủ đầu vào và rõ nghiệm thu; In review khi owner đã tự kiểm/bàn giao; Done khi reviewer xác nhận. Cả chức năng chỉ Done khi DB/API/UI đã tích hợp và ca lỗi phù hợp đạt. Không báo hoàn thành bằng ảnh mockup, dữ liệu giả trên UI hoặc chỉ chạy kiểm cú pháp SQL.

Khi thay contract, schema hay quy tắc điểm phải báo người phụ thuộc và cập nhật mẫu dữ liệu trước khi merge. Tích hợp ngay khi có một luồng đủ đầu ra; không chờ cuối 8 tuần. H01 theo dõi yêu cầu → thẻ → ca thử → bằng chứng và phân biệt rõ dữ liệu demo/giả định.

## 9\. Các quyết định phải ghi nhận ở đầu việc

| Quyết định | Người chuẩn bị | Cần trước |
| :---- | :---- | :---- |
| Schema tập trung và ánh xạ các bảng từ thiết kế cũ; script chạy lại | TV2 \+ TV1 | F00 bàn giao |
| Cách cấp hồ sơ, kích hoạt một lần và lưu trữ mã kích hoạt | TV5 \+ TV2; TV3 ghi nghiệp vụ | F02 hiện thực |
| Ma trận quyền hai nhóm Admin, cơ chế phiên/logout/khóa tài khoản | TV5 \+ TV4 | F01/F02 nghiệm thu |
| Công thức điểm, ngưỡng đạt, học lại, trần tín chỉ | TV3 \+ TV5, TV2 | F06/F08 nghiệm thu; chưa có nguồn thì ghi cấu hình demo |
| Quyền công bố/khóa điểm; điều kiện hủy; sửa lịch/tiên quyết khi đang có đợt | TV4 \+ TV5 \+ TV2 | F03/F04/F06/F08 hiện thực |
| Hướng giao diện, điều hướng và nội dung thông báo | TV6 \+ TV3, TV4 kiểm luồng | U02/U03 bàn giao |

Những lựa chọn này là đầu vào của người thực hiện, không phải lý do chặn các gói độc lập. Mọi điều chỉnh chỉ mô tả trong kế hoạch hiện tại; chưa sửa code/schema trong repo hoặc database đang chạy.

# Phần 2 Cơ sở dữ liệu phân tán sẽ lập kế hoạch sau

Phần 2 được giữ như hướng tiếp theo của đồ án, **chưa chia chức năng, nhân sự, phụ thuộc chi tiết hoặc lịch triển khai trong bản này**. Khi lập kế hoạch sẽ đối chiếu lại yêu cầu phân mảnh, nhân bản, giao dịch/truy vấn phân tán, liên cơ sở và xử lý lỗi với tài liệu thiết kế và quy định môn học.

Đầu vào dùng lại là sản phẩm Phần 1: UI/UX, contract API, quy tắc nghiệp vụ, schema tập trung, dữ liệu kiểm thử và bằng chứng tính đúng trên một DB. Việc chuyển sang nhiều DB cần kế hoạch migration/ownership riêng; không giả định chỉ đổi connection string là xong. Các tài liệu setup nhiều máy hiện có được giữ làm nguồn tham khảo cho thời điểm đó.

# Phụ lục A Khung repo và môi trường làm việc

> **Trạng thái: kế hoạch, chưa thực hiện.** Các thẻ ENV dưới đây khởi đầu ở Backlog, thuộc Phần 1\. Không dùng lại mã S0/S1/S4 hoặc cổng chặn G3 của lịch cũ. Căn cứ nội bộ: README về cấu trúc repo và Git; thiết kế J1–J4, I1; yêu cầu mới về một database tập trung. Những cấu hình dưới đây chưa được áp dụng vào repo.

## A1. Quyết định đề xuất

| Nội dung | Cách áp dụng trong Phần 1 |
| :---- | :---- |
| Số repo | Một monorepo cho PTIT One: `apps/api`, `apps/web`, `db/`, `docs/`, `scripts/`, `bench/`. Dùng repo `uisptitv2` hiện có; không cần tạo repo thứ hai hoặc đổi tên thư mục đang làm |
| IDE | Không ép thống nhất. Backend dùng IntelliJ hoặc VS Code với bộ extension Java; frontend dùng VS Code. Build/chạy bằng wrapper và script chung để không phụ thuộc tính năng riêng của IDE |
| Cách mở project | Backend mở `apps/api` hoặc import `pom.xml`; frontend mở `apps/web`. Có thể mở gốc repo hoặc workspace nhiều thư mục khi sửa xuyên module; chỉ cần terminal/run configuration dùng đúng working directory |
| Java và Maven | Đề xuất JDK 21; ghim `java.version` trong `pom.xml`, IDE và Maven dùng cùng JDK. Commit Maven Wrapper cho cả Windows và hệ khác; TV5 ghim phiên bản Spring Boot/dependency tương thích khi bootstrap |
| Node và npm | Đề xuất Node 24 LTS; khi ENV-03 bàn giao ghi bản vá cụ thể vào `.nvmrc` và hướng dẫn. Commit `package-lock.json`; sau khi có lockfile dùng `npm ci` để dựng lại |
| Dev | Vite tại 5173, Spring Boot tại 8080; frontend gọi đường dẫn tương đối `/api`, Vite proxy tới API. Bật `host: true` khi cần truy cập Vite từ máy khác; địa chỉ proxy là địa chỉ API mà máy chạy Vite truy cập được |
| Demo | Build web, chép sản phẩm build vào vùng static dành riêng cho web trong backend, đóng gói JAR. Chạy một server ứng dụng và một cổng 8080; SQL Server vẫn là dịch vụ CSDL riêng |
| Database | `db/central/` dành cho `PTITONE_CENTRAL`; backend có một DataSource. Các thư mục phân tán đã có được giữ, không gọi trong setup Phần 1 |
| Git | `feature/<module>-<task> → dev → main`; ví dụ `feature/web-f09`, `feature/db-f08`, `feature/api-f01`. Merge theo chức năng/mốc nghiệm thu, không theo tuần |

Node 20 trong bản ghi chú ban đầu đã EOL; Node 24 đang LTS tại thời điểm kiểm tra 23/09/2026. Vì vậy dùng Node 24 cho khung mới thay vì ghim Node 20\. Xem [lịch phát hành Node.js](https://nodejs.org/en/about/previous-releases). Yêu cầu runtime của template phải được kiểm lại khi bootstrap theo [hướng dẫn Vite](https://vite.dev/guide/).

JDK 21 là lựa chọn của nhóm cho kế hoạch này, nằm trong dải Java được tài liệu [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) hiện tại hỗ trợ. Không để các thành viên tự chọn phiên bản Boot hoặc thư viện khác nhau chỉ vì đều chạy được trong IDE.

## A2. Cấu trúc repo mục tiêu

PTIT-One/                              tên sản phẩm; thư mục repo hiện là uisptitv2

├── AGENTS.md

├── CLAUDE.md                           tham chiếu AGENTS.md

├── README.md

├── .gitignore

├── .gitattributes

├── .editorconfig

├── .github/

│   └── CODEOWNERS

├── docs/                               thiết kế, báo cáo, diagrams, screenshots

├── db/

│   ├── central/                        nguồn SQL dùng trong Phần 1

│   │   ├── 00-create-database.sql

│   │   ├── migrations/                 thay đổi schema có phiên bản

│   │   ├── seed/                       dữ liệu demo, chạy chủ động

│   │   └── tests/                      ca lỗi, transaction và đối soát

│   ├── master/                         giữ từ repo hiện tại

│   ├── site/                           giữ từ repo hiện tại

│   └── replication/                    giữ từ repo hiện tại

├── apps/

│   ├── api/                            Spring Boot và Maven

│   │   ├── pom.xml

│   │   ├── mvnw

│   │   ├── mvnw.cmd

│   │   ├── .mvn/wrapper/

│   │   ├── .env.example

│   │   └── src/main/

│   │       ├── java/vn/ptit/one/

│   │       │   ├── domain/

│   │       │   ├── application/

│   │       │   ├── infrastructure/

│   │       │   └── interfaces/rest/

│   │       └── resources/

│   │           └── static/             chỉ dành cho web sinh từ build-demo

│   └── web/                            React, TypeScript và Vite

│       ├── package.json

│       ├── package-lock.json

│       ├── .nvmrc

│       ├── vite.config.ts

│       └── src/

│           ├── app/                    router, providers, layout

│           ├── features/               auth, đăng ký, lịch, điểm, quản trị

│           ├── shared/                 UI, API client, tiện ích

│           └── styles/                 design tokens

├── scripts/

│   ├── dev-api.ps1                     nạp cấu hình rồi chạy API

│   └── build-demo.ps1                  build web, chép assets, package API

└── bench/                              kiểm tương tranh tập trung nếu cần

Đây là cây mục tiêu, không phải danh sách đã tạo. Chỉ thêm `.gitkeep` cho thư mục thật sự cần giữ khi rỗng. Không tạo sẵn khung routing, saga, worker hoặc các thư mục phân tán mới để đáp ứng Phần 1\. Quy tắc backend giữ lớp nghiệp vụ độc lập hạ tầng; frontend dùng `app → features → shared`, API client chung và component tái sử dụng theo J1/J2.

**Một nguồn migration:** TV2 giữ SQL phiên bản tại `db/central/migrations/`; TV5 cấu hình build Maven đưa cùng nguồn này vào resource migration của API. Không duy trì hai bản DDL khác nhau ở `db/` và `apps/api/`. Script tạo database chạy trước migration; seed demo chạy chủ động, không tự xóa/nạp lại dữ liệu mỗi lần ứng dụng khởi động.

## A3. Cấu hình Git và kết thúc dòng

`.gitignore` tối thiểu bao phủ `.idea/`, `*.iml`, `.vscode/`, `target/`, `node_modules/`, `dist/`, `.env` và các biến thể cấu hình chứa secret. Nếu dùng mẫu `.env.*` phải có ngoại lệ giữ `.env.example`. Commit wrapper, lockfile và cấu hình mẫu không có secret.

Không commit bản sao web trong `apps/api/src/main/resources/static/` sau build. Thư mục này được quy ước chỉ chứa web sinh tự động; ignore nội dung sinh, có thể giữ `.gitkeep`. Các tài nguyên viết tay của web nằm ở `apps/web` để không bị lần build sau ghi đè.

Đề xuất `.gitattributes`:

\* text=auto

\*.md text eol=lf

\*.sql text eol=lf

\*.java text eol=lf

\*.ts text eol=lf

\*.tsx text eol=lf

\*.json text eol=lf

\*.yml text eol=lf

\*.yaml text eol=lf

mvnw text eol=lf

\*.sh text eol=lf

\*.cmd text eol=crlf

\*.ps1 text eol=crlf

`.editorconfig` thống nhất UTF-8, newline cuối file, quy tắc thụt dòng và EOL tương ứng. Kiểm tra hiện trạng trước khi chuẩn hóa các file CRLF; tách thay đổi định dạng khỏi thay đổi nghiệp vụ để review được. Không chạy chuẩn hóa toàn repo kèm một task chức năng nhỏ.

README hiện mô tả `feature/*` nhưng một số ví dụ dùng `db/schema`, `api/core`; ENV-07 sẽ thống nhất ví dụ theo quy ước ở A1. CODEOWNERS hiện có owner mặc định và các vùng còn chú thích: ENV-01 kiểm lại, giữ quyền hiện có, bổ sung tài khoản thật khi nhóm cung cấp; không commit tên TV1–TV6 như username GitHub. Tài liệu nói `dev` cần PR, `main` cần approval CODEOWNERS; phải kiểm quy tắc GitHub thực tế khi triển khai, không coi việc viết phụ lục là đã đổi branch protection.

## A4. Gói việc và phụ thuộc

| Mã | Gói cha | Owner / reviewer | Việc cần làm | Phụ thuộc và điều kiện nhận |
| :---- | :---- | :---- | :---- | :---- |
| ENV-01 | F00 | TV4 / TV1 | Cấu hình repo, ignore/EOL/editor, CODEOWNERS, hướng dẫn agent và thư mục cần thiết | Đọc config hiện có trước khi sửa; clone mới có cấu trúc, không chứa secret, không thay quyền Git ngoài yêu cầu; bàn giao cho ENV-02/03/04 |
| ENV-02 | F00 | TV5 / TV4 | Dựng `apps/api` với Web, Maven Wrapper và package nền; ghim Java/Boot | Sau cấu trúc ENV-01; `mvnw.cmd spring-boot:run` chạy được khi chưa có JDBC/DB; không dựng logic phân tán. Đây là bước bootstrap ngắn, chưa đạt F00 đầy đủ |
| ENV-03 | F00 \+ U02/U03 | TV6 / TV5 | Dựng Vite `react-ts`, cấu trúc frontend, Node/lockfile, proxy `/api` | Sau cấu trúc ENV-01; làm song song ENV-02/04; `npm ci`, dev và build chạy; mock có nhãn; thử proxy khi API sẵn sàng |
| ENV-04 | F00 | TV2 / TV1 | Bộ SQL `db/central/`, tạo một DB, migration/seed/test và hướng dẫn đích chạy | Sau quy ước ENV-01; tận dụng schema đã đọc; dựng lại được `PTITONE_CENTRAL`, không cần snapshot/site khác; bàn giao URL, schema và quyền cho ENV-05 |
| ENV-05 | F00 \+ F01 | TV5 / TV4 | Tích hợp JDBC, driver SQL Server, validation, migration và cấu hình một DataSource; thêm Security và cơ chế phiên theo F01 | ENV-02 \+ DB/config ENV-04; tài khoản chạy API và tài khoản chạy migration được cấp đúng quyền. Chốt thư viện JWT nếu dùng JWT, không mặc định thêm jjwt khi chưa chọn. Có test truy vấn/rollback và quyền; không đợi S4 cũ |
| ENV-06 | H02 | TV1 / TV5, TV6 | `build-demo.ps1`, cách chạy JAR và kiểm luồng SPA/API | Có bản web build được từ ENV-03 và API tích hợp ENV-05; thực hiện theo từng mốc, bản cuối dùng chức năng thật; một URL ứng dụng, assets/API/router đều hoạt động |
| ENV-07 | H01 | TV3 / TV4 | Cập nhật README, phụ lục và hướng dẫn dùng công cụ/Git cho đúng Phần 1 | Bắt đầu bằng kế hoạch; hoàn thiện từ kết quả ENV-01–06. Người khác làm theo được, thông tin phiên bản và biến môi trường khớp code |

Thứ tự: **ENV-01 → ENV-02 và ENV-03 và ENV-04 làm song song → ENV-05 nối DB → ENV-06 đóng gói**. ENV-07 ghi tài liệu xuyên suốt. UI không chờ JDBC; nghiệp vụ không chờ phụ lục phân tán.

F00 chỉ hoàn thành khi nối được DB theo ENV-05, không đánh dấu F00 Done từ việc skeleton khởi động. Security/phiên được nghiệm thu đầy đủ ở F01. Không giữ nhận định “thêm dependency sớm chắc chắn không khởi động được”; khả năng khởi động phụ thuộc cấu hình auto-configuration và DataSource. Kế hoạch tách hai bước để có đầu vào DB rõ ràng, không để trì hoãn backend tới Phần 2\.

## A5. Cấu hình dev và đóng gói demo

**Biến môi trường:** giữ file riêng tư ở `apps/api/.env`, mẫu tại `apps/api/.env.example` chỉ chứa tên biến và ghi chú. Spring Boot mặc định đọc biến môi trường hệ điều hành và cấu hình application; đặt `.env` cạnh `pom.xml` không tự khiến nó được nạp. Chọn một cách chung: script `dev-api.ps1` nạp các cặp tên/giá trị vào môi trường tiến trình rồi gọi wrapper; khi chạy trực tiếp trong IDE, cấp cùng biến qua run configuration. Script phải xử lý giá trị như dữ liệu, không thực thi nội dung `.env`, không in secret ra log. Cách ánh xạ biến theo [tài liệu cấu hình Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).

Frontend dùng `/api` qua proxy, chưa cần `.env` chứa thông tin kết nối DB. Không đưa mật khẩu, JWT signing secret hoặc thông tin truy cập DB vào biến `VITE_*`; các biến được phơi ra phía client có thể nằm trong bundle. Xem [Vite về biến môi trường](https://vite.dev/guide/env-and-mode).

**Lệnh dev trên Windows, sau khi các thẻ ENV được hiện thực:** web chạy `npm ci`, rồi `npm run dev` trong `apps/web`; API chạy qua `scripts/dev-api.ps1` hoặc `mvnw.cmd spring-boot:run` trong `apps/api` khi đã cấp đủ biến. File `.nvmrc` ghi phiên bản yêu cầu, không tự cài/chuyển Node trên mọi máy; hướng dẫn cần cách kiểm `node --version`, `java -version` và JDK mà Maven đang dùng. Bootstrap chưa có lockfile dùng `npm install` một lần để tạo lock, sau đó commit lock và dùng `npm ci`.

**Quy trình script demo dự kiến:** kiểm công cụ/cấu hình → build web từ lockfile → thay vùng static sinh tự động bằng `dist` mới → package API bằng Maven Wrapper → bàn giao JAR và hướng dẫn chạy với cấu hình một DB. Script chạy được từ vị trí bất kỳ bằng đường dẫn dựa trên vị trí script, dừng ngay khi một bước lỗi; kiểm đúng đường dẫn vùng sinh trước khi dọn assets cũ. Không xóa tài nguyên viết tay hoặc database trong quá trình build.

**Nghiệm thu bản demo:** mở trang gốc, đăng nhập, gọi `/api`, tải CSS/JS và mở trực tiếp/tải lại một đường dẫn sâu như trang bảng điểm đều hoạt động. Nếu dùng router phía client phải cấu hình fallback cho các route UI phù hợp; không chuyển lỗi `/api` hoặc asset thiếu thành HTML. Chạy lại build không mang theo assets lỗi thời; Git không xuất hiện file build/secret để commit. “Một server, một cổng” ở đây chỉ là tầng web/API, không bao gồm tiến trình SQL Server.

PR chạm nhiều vùng phải mời reviewer của các vùng đó. Quy trình Git và repo là công cụ để từng người làm việc độc lập rồi tích hợp theo chức năng; các thẻ ENV vẫn chỉ là kế hoạch cho tới khi có kết quả chạy và review.