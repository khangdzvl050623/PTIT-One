# PTIT One — Kịch bản thuyết trình cho 2 người

Thời lượng: **12–15 phút**. Demo **hoàn toàn trên giao diện**, không mở Swagger.

> Lý do bỏ Swagger: Swagger chứng minh *có endpoint*, không chứng minh *hệ thống
> dùng được*. Người chấm nhìn một màn hình JSON thì vẫn phải tin lời mình nói.
> Nhìn một sinh viên đăng ký trượt vì lớp đầy thì không cần tin ai cả.

---

## 0. Chuẩn bị — làm trước buổi trình bày 30 phút

### Đưa dữ liệu về đúng trạng thái seed

**Bắt buộc.** Mọi lượt bấm thử trước đó đều làm lệch sĩ số lớp, và kịch bản
dưới đây dựa vào con số chính xác của seed.

```powershell
sqlcmd -S <server> -d PTITONE_CENTRAL -E -N -C -b -I -i db\central\seed\99-reset-fixture.sql
sqlcmd -S <server> -d PTITONE_CENTRAL -E -N -C -b -I -i db\central\seed\20-hoc-vu-seed.sql
```

### Khởi động đúng cách

```powershell
.\scripts\dev-api.ps1          # KHÔNG dùng mvnw spring-boot:run — nó không nạp .env
cd apps\web; npm run dev
```

⚠️ `apps\web\.env.local` phải có `VITE_API_MODE=api`. Thiếu dòng này thì giao
diện chạy **dữ liệu giả** và mọi thứ trông vẫn đúng — đây là cách hỏng demo
khó phát hiện nhất.

### Ba phép thử nhanh trước khi vào phòng

| Kiểm | Kỳ vọng |
|---|---|
| `GET /api/health/db` | `{"status":"UP","database":"PTITONE_CENTRAL"}` |
| `GET /api/health/media` | `{"status":"UP","cloudName":"…"}` — nếu `DOWN` thì bỏ phần ảnh đại diện |
| Ô đăng nhập | **không** có dòng "Chế độ demo, chưa nối API" |

### Tài khoản — mật khẩu tất cả là `PtitOne@2026`

| Tài khoản | Vai | Dùng ở phần |
|---|---|---|
| `B26DCCN001` | Sinh viên, cơ sở HCM | Người 1 |
| `GVHCM001` | Giảng viên, cơ sở HCM | Người 1 |
| `admin.hcm` | Quản trị đào tạo cơ sở HCM | Người 2 |
| `admin.master` | Quản trị danh mục toàn trường | Người 2 |
| `B26DCCN003` | **Cố ý chưa kích hoạt** — dùng để chứng minh bị chặn | Người 2 |

### Mở sẵn 2 cửa sổ trình duyệt

Một cửa sổ thường và một cửa sổ **ẩn danh**. Hai vai đăng nhập cùng lúc, không
phải đăng xuất đăng nhập lại giữa buổi — thao tác đó tốn 20 giây mỗi lần và
làm đứt mạch kể chuyện.

---

## 1. Phân công

| | Người 1 | Người 2 |
|---|---|---|
| Vai kể | **Người dùng** — sinh viên và giảng viên | **Nhà trường** — quản trị, rồi database |
| Thời gian | 6–7 phút | 6–8 phút |
| Cửa sổ | thường: `B26DCCN001` · ẩn danh: `GVHCM001` | thường: `admin.hcm` · ẩn danh: `admin.master` |

**Thông điệp chung:** *Phần 1 đã xong — nghiệp vụ đầy đủ chạy trên một database
tập trung. Phần 2 là cơ sở dữ liệu phân tán, đã có kế hoạch và đang vào giai
đoạn cổng chặn kỹ thuật.*

---

## 2. Người 1 — Vòng đời một lượt đăng ký

### Mở đầu (30 giây)

> Em demo bằng giao diện thật, không qua công cụ API. Câu chuyện là một sinh
> viên đăng ký học phần, và những gì hệ thống **từ chối** — vì phần từ chối mới
> là phần khó.

### Bước 1 · Đăng nhập và xem hồ sơ — màn **Thông tin**

| Thao tác | Chỉ cho người xem thấy |
|---|---|
| Đăng nhập `B26DCCN001` | Vào thẳng màn **Thông tin** |
| Chỉ bảng hồ sơ | Tên, ngày sinh, cơ sở, chương trình đào tạo, tín chỉ tích luỹ — **đọc từ database, không phải dữ liệu mẫu** |
| Chỉ thanh menu | Chỉ có mục của sinh viên. **Không có** mục quản trị |

> Menu dựng theo vai trò lấy từ phiên đăng nhập. Nhưng ẩn menu chỉ là trải
> nghiệm — quyền thật do backend kiểm ở từng request. Em sẽ chứng minh ở bước 5.

### Bước 2 · Đăng ký thành công — màn **Đăng ký học phần**

| Thao tác | Kỳ vọng |
|---|---|
| Mở **Đăng ký học phần** | Thấy danh sách lớp học kỳ hiện tại, kèm sĩ số dạng `2/3` |
| Đăng ký lớp `BAS1150-2026-1-HCM01` | Thành công; **sĩ số tăng đúng 1**; tín chỉ đã đăng ký tăng |

> Bộ đếm sĩ số do ứng dụng sở hữu, cập nhật bằng một câu `UPDATE` có điều kiện
> chứ không phải đếm rồi ghi — chi tiết ở bước 4.

### Bước 3 · Ba kiểu từ chối — phần đáng xem nhất

Làm liên tiếp, mỗi ca 20 giây. Đây là bốn lớp ràng buộc của nghiệp vụ đăng ký.

| Thử đăng ký | Hệ thống báo | Chứng minh |
|---|---|---|
| `INT1154-2026-1-HCM01` | **Lớp đã đủ chỗ** | chống vượt sức chứa |
| `INT1445-2026-1-HCM01` | **Lớp chưa mở** | chỉ lớp trạng thái `MO` mới nhận |
| Lớp khác **cùng môn** vừa đăng ký | **Đã đăng ký môn này trong kỳ** | chống trùng môn — `UNIQUE(SV, lớp)` không chặn được ca này |

> Ba thứ này không kiểm ở trình duyệt. Server kiểm lại toàn bộ, vì giao diện có
> thể bị sửa còn server thì không.

### Bước 4 · Nói về tương tranh (không demo trực tiếp, 40 giây)

> Đợt đăng ký là lúc hàng trăm người tranh cùng một dòng dữ liệu. Nhóm có một
> ca kiểm thử tự động: **100 luồng cùng đăng ký vào lớp 30 chỗ**. Kết quả đúng
> 30 lượt thành công, 70 lượt bị từ chối, và năm nguồn số liệu đối soát khớp
> nhau. Cách làm là `UPDATE` có điều kiện rồi đọc `@@ROWCOUNT`, **không** phải
> `SELECT COUNT` rồi `IF` — cách đó là race condition.

Nếu còn thời gian, mở `EnrollmentConcurrencyIntegrationTest` và chỉ tên ca test.

### Bước 5 · Quyền — thử phá hệ thống (30 giây)

| Thao tác | Kỳ vọng |
|---|---|
| Gõ thẳng `/quan-tri/ho-so` lên URL | Vào màn **Không đủ quyền** |

> Sinh viên không có menu đó, nhưng gõ tay URL thì vẫn tới được tuyến. Chặn
> thật nằm ở backend: mọi request đều kiểm vai trò từ token đã ký, không tin
> tham số client gửi lên.

### Bước 6 · Đổi cửa sổ — giảng viên nhập điểm

| Thao tác (cửa sổ ẩn danh, `GVHCM001`) | Kỳ vọng |
|---|---|
| Mở **Lớp phụ trách** | Chỉ thấy lớp mình dạy, **không thấy lớp giảng viên khác** |
| Mở **Nhập điểm**, chọn một lớp | Bảng điểm theo danh sách sinh viên của lớp |
| Nhập chuyên cần / giữa kỳ / cuối kỳ rồi lưu | **Điểm tổng kết do server tính**, không nhập tay |
| Bấm **Công bố điểm** | Trạng thái bảng điểm đổi |

> Nếu còn thiếu điểm thành phần của một sinh viên, hệ thống **không cho công
> bố**. Thứ tự bắt buộc là nhập → công bố → khoá, và sau khi khoá thì không ai
> sửa được nữa, kể cả qua đường API khác.

### Bước 7 · Quay lại cửa sổ sinh viên (30 giây)

| Thao tác | Kỳ vọng |
|---|---|
| Mở **Bảng điểm** | Thấy điểm giảng viên vừa công bố |
| Chỉ môn chưa có điểm | Hiện **"Chưa có điểm"**, không hiện `0` |

> Chi tiết nhỏ nhưng quan trọng: điểm chưa công bố để trống chứ không để 0. Để
> 0 là nói sai về một sinh viên chưa được chấm.

Chuyển lời: *"Phần người dùng là vậy. Bạn em trình bày phía nhà trường vận hành."*

---

## 3. Người 2 — Nhà trường vận hành, và database

### Bước 1 · Danh mục môn học — màn **Danh mục và tiên quyết** (`admin.master`)

| Thao tác | Kỳ vọng |
|---|---|
| Thêm một môn mới | Xuất hiện trong danh mục |
| Thêm môn tiên quyết cho nó | Lưu được |
| Thử tạo **vòng** `A → B → A` | Bị từ chối, **tập tiên quyết cũ còn nguyên** |
| Thử xoá một môn **đang được dùng** | Bị từ chối, nêu rõ lý do |

> Xoá môn có ba cổng chặn theo thứ tự: đang là tiên quyết của môn khác → đã mở
> lớp → đang nằm trong chương trình đào tạo. Chỉ môn **chưa từng được dùng** mới
> xoá được.

### Bước 2 · Cấp tài khoản — màn **Hồ sơ và tài khoản** (`admin.master`)

| Thao tác | Kỳ vọng |
|---|---|
| Cấp hồ sơ sinh viên mới, **bỏ trống email** | Hiện **mã kích hoạt một lần** |
| Chỉ vào dòng cảnh báo | Mã chỉ hiện **đúng một lần** — database chỉ lưu bản băm |
| Mở `/kich-hoat` ở cửa sổ ẩn danh, dùng mã đó đặt mật khẩu | Đặt được, rồi đăng nhập được |

> Quản trị **không bao giờ biết mật khẩu của sinh viên**. Hệ thống phát mã kích
> hoạt dùng một lần; sinh viên tự đặt mật khẩu. Nếu cấp kèm email thì mã **chỉ
> đi qua thư** và quản trị không nhìn thấy — lúc đó việc kích hoạt được cũng
> chính là bằng chứng người đó sở hữu hòm thư.

### Bước 3 · Tài khoản bị chặn (20 giây)

| Thao tác | Kỳ vọng |
|---|---|
| Thử đăng nhập `B26DCCN003` | Bị từ chối |

> Tài khoản này ở trạng thái chờ kích hoạt. Thông báo lỗi **cố ý giống hệt** ca
> sai mật khẩu — để form đăng nhập không thành công cụ dò xem tài khoản nào có
> thật.

### Bước 4 · Mở lớp và xếp lịch — màn **Lớp học phần** (`admin.hcm`)

| Thao tác | Kỳ vọng |
|---|---|
| Tạo một lớp học phần | **Mã lớp do server sinh**, mang mã cơ sở của admin |
| Xếp lịch trùng giờ một giảng viên đang dạy | Bị từ chối |
| Xếp lịch trùng phòng | Bị từ chối |
| Hạ sức chứa xuống **dưới** sĩ số hiện tại | Bị từ chối |

> Không endpoint ghi nào nhận tham số cơ sở từ client. Cơ sở luôn lấy từ token
> đã ký — đây là chủ ý chống leo thang đặc quyền, không phải thiếu sót.

### Bước 5 · Thống kê — màn **Tổng quan**

| Thao tác | Kỳ vọng |
|---|---|
| Xem thống kê với `admin.hcm` | Chỉ số liệu **cơ sở HCM** |
| Thử lọc sang cơ sở khác | Bị từ chối |
| Đổi sang `admin.master` (cửa sổ ẩn danh) | Thấy **toàn trường**, lọc được từng cơ sở |

> Đây là chỗ Phần 2 sẽ thay đổi nhiều nhất: hiện mỗi truy vấn chạy trên một
> database. Sang Phần 2, cùng màn hình này sẽ gom số liệu từ ba cơ sở qua
> Linked Server.

### Bước 6 · Database (2 phút) — mở SSMS hoặc ảnh ERD

| Chỉ vào | Nói |
|---|---|
| Nhóm bảng danh mục | `MonHoc`, `Khoa`, `ChuongTrinhDaoTao`, `HocKy` — **dùng chung toàn trường**, Phần 2 sẽ nhân bản một chiều từ Master xuống các cơ sở |
| Nhóm bảng theo cơ sở | `SinhVien`, `GiangVien`, `LopHocPhan`, `DotDangKy` — **phân mảnh ngang theo cơ sở** ở Phần 2 |
| `DangKyHocPhan`, `Diem` | **Phân mảnh dẫn xuất**: đi theo `LopHocPhan`, tức theo cơ sở **mở lớp**, không theo cơ sở của sinh viên |
| Số migration | `V1`–`V10`, chạy tự động bằng Flyway khi API khởi động |

> Chi tiết dẫn xuất ở dòng thứ ba là quyết định thiết kế quan trọng nhất của
> lược đồ phân mảnh: điểm của một sinh viên học liên cơ sở nằm ở **cơ sở dạy**,
> rồi mới đồng bộ về cơ sở nhà.

### Bước 7 · Phần 2 và lời kết (1 phút)

| Yêu cầu bắt buộc | Trạng thái |
|---|---|
| Phân mảnh ngang | Lược đồ xong, script viết xong, **chưa triển khai** |
| Nhân bản một chiều | Script xong, **chờ cổng chặn kỹ thuật** |
| Giao dịch phân tán (2PC) | Đặt ở nghiệp vụ **chuyển cơ sở sinh viên** — chưa cài |
| Tình huống tương tranh | ✅ **đã xong ở Phần 1** |
| Truy vấn phân tán | Thiết kế xong, chưa cài |

> Việc tiếp theo của nhóm là **cổng chặn kỹ thuật**: kiểm nhân bản và giao dịch
> phân tán chạy được giữa hai máy. Hai thứ đó nếu hỏng thì phải đổi phương án,
> nên nhóm kiểm trước chứ không để tới cuối.
>
> Em xin hết. Nhóm sẵn sàng nhận câu hỏi.

---

## 4. Câu hỏi dự kiến

| Câu hỏi | Trả lời ngắn |
|---|---|
| Sao chưa làm phân tán? | Nhóm đảo thứ tự có chủ ý: làm xong nghiệp vụ trên một database để khi tách ra còn biết **đúng sai dựa vào đâu**. Phần 2 đã có kế hoạch theo giai đoạn. |
| 2PC đặt ở đâu, vì sao không đặt ở đăng ký? | Đặt ở **chuyển cơ sở sinh viên**. Đăng ký là đường nóng tranh chấp cao, giữ khoá qua mạng sẽ giết thông lượng — chỗ đó dùng saga, vì trạng thái "đang xử lý" là trung gian an toàn. Chuyển cơ sở thì **không có** trạng thái trung gian an toàn nào. |
| Chống đăng ký trùng thế nào? | Bốn lớp: `UPDATE` có điều kiện cho sức chứa, unique filtered index cho trùng môn, bản sao lịch tại cơ sở nhà cho trùng giờ, và khoá tuần tự hoá **trước** mọi phép kiểm. |
| Dữ liệu demo có thật không? | Là dữ liệu seed cố định, nằm trong repo, dựng lại được bằng một lệnh. |
| Ảnh đại diện lưu ở đâu? | Dịch vụ ảnh ngoài; database chỉ lưu URL. Ảnh không phải dữ liệu nghiệp vụ, và Phần 2 nhân bản bảng đó nên cột nhị phân sẽ làm phình mọi bản sao. |

---

## 5. Checklist 5 phút trước giờ

- [ ] Đã chạy reset fixture **và** seed lại
- [ ] API khởi động bằng `scripts\dev-api.ps1`, không phải `mvnw`
- [ ] `VITE_API_MODE=api` — ô đăng nhập **không** có dòng "Chế độ demo"
- [ ] `/api/health/db` trả `UP`
- [ ] Hai cửa sổ trình duyệt đã đăng nhập sẵn hai vai
- [ ] Phóng to chữ trình duyệt lên **125–150%**
- [ ] Tắt thông báo hệ thống và ứng dụng chat
- [ ] Đã bấm thử trọn kịch bản **ít nhất một lần** hôm trước

> ⚠️ Mục cuối không phải thủ tục. Bốn màn hình trong kịch bản này —
> `/kich-hoat`, `/tai-khoan/email`, cấp lại mật khẩu, và ảnh đại diện của giảng
> viên — **mới dựng xong và chưa ai bấm thử bằng trình duyệt**. Phải chạy thử
> trước, nếu màn nào hỏng thì bỏ bước đó khỏi kịch bản chứ đừng sửa tại chỗ.
