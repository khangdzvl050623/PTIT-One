# PTIT One — Thiết kế hệ thống đăng ký tín chỉ đa cơ sở

> **Đồ án cuối kỳ — Cơ sở dữ liệu phân tán.**
> **Đây là tài liệu duy nhất của dự án.** Cấu trúc bám sát mục 2–3 của đề bài để nạp thẳng vào báo cáo.
>
> ✅ **THIẾT KẾ ĐÃ CHỐT.** Mọi thay đổi từ đây phải qua thảo luận nhóm và ghi vào bảng quyết định (0.1).
> Xem sơ đồ toàn cảnh các luồng hoạt động ở [README](../README.md#toàn-cảnh-luồng-hoạt-động).

---

## Mục lục — cần gì đọc mục nào

| Bạn đang cần… | Đọc mục |
|---|---|
| ⭐ **Biết đâu là phần KHÔNG ĐƯỢC PHÉP THIẾU** | **0.1b — năm yêu cầu bắt buộc của giảng viên** |
| Nắm nhanh dự án trong 5 phút | **0.1** bảng quyết định · **0.2** điểm đặc biệt · **C0** hai chế độ ghi |
| Viết mục 2.1 báo cáo (Đặt vấn đề) | **Phần A** |
| Viết mục 2.2.1 báo cáo (Phân tích) | **Phần B** — đặc biệt **B2** bảng tần suất và **B3** ma trận phân quyền |
| Viết mục 2.2.2 báo cáo (Thiết kế) | **Phần C** — C1 bảng · C2 ownership · C3 phân mảnh · C4 ánh xạ · C5 định vị · C6 kiến trúc |
| Làm cài đặt vật lý (mục 3 đề bài) | **Phần F** chi tiết · **I5** checklist tick nhanh |
| Hiểu cơ chế phân tán để trả lời câu hỏi | **Phần D** — riêng giao dịch phân tán xem **D8** |
| Cho sinh viên gọi API từ máy khác / qua internet | **C7b** |
| Làm phần X-Ray | **0.2** và **Phần E** |
| Đo đạc, benchmark, test | **Phần G** |
| Xem lịch, phân vai | **Phần H** |
| **Bắt đầu viết code — thứ này nằm ở đâu?** | **Phần J** khung mã nguồn |
| Tra nhanh một bảng nào đó | **I6** danh sách bảng |
| Biết còn gì chưa chốt | **I4** việc còn treo |
| Lo về máy móc, chi phí, uptime | **I2b** vận hành máy chủ |

### Mục lục đầy đủ

| Phần | Nội dung |
|---|---|
| **0** | Cách đọc · **0.1** Bảng quyết định (D1–D16) · **0.1b ⭐ NĂM YÊU CẦU BẮT BUỘC** · **0.2** ⭐ X-Ray Phân Tán |
| **A** — Đặt vấn đề | A1 nhu cầu · A2 vì sao phân tán · A3 vị trí & dữ liệu · A4 đối tượng sử dụng |
| **B** — Phân tích | B1 chức năng · **B2 bảng tần suất** · B3 ma trận phân quyền · B4 chức năng từng vị trí · B5 máy trạm/máy chủ · B6 ERD |
| **C** — Thiết kế | **C0 hai chế độ ghi** · C1 CSDL quan hệ · C2 ownership · C3 lược đồ phân mảnh · C4 lược đồ ánh xạ · C5 sơ đồ định vị · C6 Client/Server · C7 front/back-end · **C7b truy cập mạng** · C8 abstraction boundary · C9 chiến lược ID · C10 read model & Outbox · C11 nền tảng vận hành |
| **D** — Cơ chế phân tán | D1 nhân bản · D2 Linked Server & tối ưu · D3 saga liên cơ sở · D4 giao dịch & tương tranh · **D8 ⭐ GIAO DỊCH PHÂN TÁN (2PC)** · D5 trigger & phân quyền · D6 xử lý sự cố · D7 các mức trong suốt |
| **E** — ⭐ X-Ray | E1 vấn đề · E2 thiết kế · E3 ba chế độ · E4 giới hạn phạm vi |
| **F** — Cài đặt vật lý | F1 VPN · F2 link mạng · F3 SQL Server · F4 Agent · **F4b ⭐ MS DTC** · F5 Linked Server · F6 Publication · F7 thử giao tác |
| **G** — Kiểm thử & đo đạc | G1 seed · G2 benchmark · G3 tương tranh · G4 sự cố · G5 2PC vs saga · G6 phân mảnh dọc · G7 deadlock |
| **H** — Lộ trình | 8 tuần + phân vai 5 người |
| **J** — Khung mã nguồn ➕ | J1 backend Hexagonal · J2 frontend Feature-Sliced · J3 CSDL `db/` · J4 thứ tự dựng |
| **I** — Phụ lục | I1 repo · I2 plan B · I2b vận hành máy chủ · I3 rủi ro · I4 việc còn treo · **I5 checklist tick nhanh** · **I6 danh sách bảng** · I7 nguồn tham khảo |

---

## 0. Cách đọc tài liệu

| Ký hiệu | Nghĩa |
|---|---|
| **In đậm** | **Bắt buộc theo đề bài môn học.** Ưu tiên tuyệt đối, ước lượng ~75% điểm |
| ➕ | Phần mở rộng ngoài yêu cầu tối thiểu. Chỉ làm sau khi phần in đậm đã xong |
| ⚠️ | Rủi ro hoặc điểm dễ mất điểm |
| ⭐ | Điểm đặc biệt của dự án |
| 🔶 | Giả định — nhóm cần xác nhận lại |

**Nguyên tắc thi công bất di bất dịch:** không viết dòng code ứng dụng nào trước khi Phần F (cài đặt vật lý) đã PASS và đã chụp đủ screenshot.

---

## 0.1 Bảng quyết định đã chốt

| Mã | Quyết định | Chốt |
|---|---|---|
| D1 | Đăng ký liên cơ sở (Home/Host) | ✅ Làm |
| D2 | Master cho dữ liệu tham chiếu | ✅ **Database `PTITONE_MASTER` riêng biệt, đặt trên hạ tầng SRV-HCM** — Master là một *vai trò*, không phải một cơ sở (xem C0) |
| **D14** | **Tách Master khỏi CSDL vận hành** | ✅ **Có.** `PTITONE_MASTER` là Publisher; cả ba CSDL vận hành (`PTITONE_HCM`, `PTITONE_HN`, `PTITONE_DN`) đều là Subscriber → topology **đối xứng hoàn toàn** |
| **D15** | **Master colocate hay chạy máy riêng** | ⏳ **Quyết ở tuần 3 theo số máy thật.** 3 máy → colocate trên SRV-HCM · 4 máy → nút `SRV-MASTER` riêng. **Kiến trúc logic không đổi trong cả hai trường hợp** (xem C0) |
| D3 | Chủ sở hữu `DangKyHocPhan` | ✅ **Host Campus** (cơ sở mở lớp) |
| **D4** | **Distributed transaction / 2PC** | ✅ **BẮT BUỘC có một luồng** — đặt tại **chuyển cơ sở sinh viên** (D8). Không đặt vào đăng ký học phần vì đó là đường nóng có tranh chấp |
| **D16** | **Truy cập mạng** | ✅ **CSDL kín trong VPN · chỉ tầng API mở công khai** qua tunnel miễn phí — đúng hình dạng hệ thống thật (C7b) |
| D5 | Saga đồng bộ hay bất đồng bộ | ✅ Thử đồng bộ; timeout thì rơi về `CHO_DUYET` |
| D6 | Truy cập dữ liệu | ✅ **JdbcTemplate** cho đăng ký, cross-site và benchmark |
| **D7** | **Phân giải cơ sở của sinh viên** | ✅ **`DanhBaNguoiDung` — danh bạ toàn cục nhân bản từ Master.** Mã SV *không* mã hóa cơ sở |
| D8 | Chiến lược ID chống trùng | ✅ **Không dùng `IDENTITY`.** Chọn khóa **theo từng aggregate**: mã nghiệp vụ toàn trường cho `SinhVien`/`MonHoc`, khóa nhúng mã cơ sở cho `LopHocPhan`/`DotDangKy`, khóa kép cho `DangKyHocPhan`/`Diem`, UUID cho idempotency key (xem C9) |
| D9 | VPN | ✅ **Radmin** (bám tài liệu GV); Tailscale là dự phòng |
| D10 | Kiểu instance SQL Server | ⚠️ Quyết cùng D12 — 2 site thì default instance; ≥3 site trên ít máy thì named instance |
| D11 | `READ_COMMITTED_SNAPSHOT` | ✅ Bật |
| **D12** | **Số cơ sở** | ✅ **Thiết kế cho N · triển khai 3 (HCM · HN · ĐN) · dự phòng 2.** Chốt cuối tuần 1 sau spike |
| D13 | Phân quyền ở tầng CSDL | ✅ **Trigger = toàn vẹn theo site · Database role = quyền theo vai trò · Ứng dụng = quyền theo dòng** |
| — | Phân mảnh dọc / hỗn hợp | ✅ **Không làm.** Đề bài chỉ liệt kê ngang, dẫn xuất, nhân bản. Có phân tích bác bỏ tại G6 |
| — | Tính sẵn sàng cho dữ liệu liên cơ sở | ✅ **Local projection (read model)** ở tầng ứng dụng, không dùng publication chiều ngược |
| — | Đồng bộ điểm về cơ sở nhà | ✅ **BẮT BUỘC** — qua Outbox + worker + upsert idempotent có `Version` |
| — | Abstraction boundary | ✅ Đúng **3 port**: `CrossSiteQuery`, `GlobalReport`, `CatalogHealth` |
| — | Linked Server topology | ✅ **Hình sao từ SRV-HCM**, N−1 định nghĩa; báo cáo chạy trong ngữ cảnh `PTITONE_HCM` |

🔶 **Giả định đang dùng, cần xác nhận:** quy mô 8.000 SV HCM / 5.000 HN / 3.000 ĐN · 6 học phần mỗi kỳ · đợt đăng ký dồn 3 ngày · 2% sinh viên học liên cơ sở · tài liệu Replication của giảng viên hướng dẫn theo wizard SSMS.

---

## 0.1b ⭐ NĂM YÊU CẦU BẮT BUỘC CỦA GIẢNG VIÊN

> **Đây là danh sách phải-có. Thiếu một mục là mất điểm nặng.** Mọi thứ khác trong tài liệu này đều là phần thêm; năm dòng dưới đây là phần không được phép thiếu.

| # | Yêu cầu | Hiện thực ở đâu | Chứng minh bằng | Trạng thái |
|---|---|---|---|---|
| 1 | **1 phương pháp phân mảnh** | **Phân mảnh ngang** theo cơ sở — `SinhVien`, `GiangVien`, `TaiKhoan`, `DotDangKy`, `LopHocPhan` (C3) *(có thêm dẫn xuất bậc 1 và bậc 2 — phần dư)* | F7b-1, F7b-2 | ✅ |
| 2 | **1 phương pháp replication** | **Transactional Replication** một chiều `PTITONE_MASTER` → mọi Subscriber (D1) | F7b-3, F7b-4 | ✅ |
| 3 | **1 distributed transaction** | **2PC / MS DTC cho nghiệp vụ chuyển cơ sở sinh viên** (D8) — ba CSDL trong một giao dịch nguyên tử | F7e | ⚠️ **PHẢI LÀM** |
| 4 | **1 tình huống concurrency** | **100 luồng tranh 30 chỗ** khi đăng ký học phần — `UPDATE` có điều kiện + 4 lớp ràng buộc (D4) | G3 | ✅ |
| 5 | **1 distributed query** | **`OPENQUERY` thống kê toàn hệ thống** qua Linked Server (D2) | F7c, B1 | ✅ |

### ⚠️ Yêu cầu 3 làm thay đổi thiết kế, không phải bổ sung nhỏ

Bản trước cố tình **tránh** 2PC, thay bằng saga. Nay 2PC là bắt buộc, nên thiết kế có **cả hai**, dùng đúng chỗ:

| | **Chuyển cơ sở sinh viên** | **Đăng ký liên cơ sở** |
|---|---|---|
| Cơ chế | **Distributed transaction (2PC)** | **Saga + idempotent receiver** |
| Tần suất | Rất hiếm — vài lần mỗi kỳ | 32.000 lượt/ngày cao điểm |
| Tranh chấp | Không — mỗi sinh viên một dòng riêng | Cao — hàng trăm người tranh một dòng lớp |
| Cần nguyên tử tuyệt đối? | **Có.** Không tồn tại trạng thái trung gian an toàn: sinh viên không thể "nửa ở HCM nửa ở HN" | Không — trạng thái `CHO_DUYET` là trung gian an toàn |
| Giữ lock qua mạng | Không đáng kể | Thảm hoạ cho thông lượng |

> ⭐ **Đây là điểm mạnh, không phải điểm vá.** Trả lời được *"vì sao chỗ này dùng 2PC mà chỗ kia không"* — kèm số đo ở **B5** — mạnh hơn nhiều so với chỉ cài một trong hai. Đó là câu trả lời thể hiện **phán đoán thiết kế**, không chỉ là kỹ năng cài đặt.

### Cặp đối chiếu thứ hai — cùng ghi hai nơi, khác cách xử lý

| | **Tạo sinh viên mới** | **Chuyển cơ sở** |
|---|---|---|
| Cũng ghi 2–3 CSDL | Có | Có |
| Cơ chế | **Outbox + eventual consistency** | **2PC** |
| Vì sao khác | Có **trạng thái trung gian an toàn**: `TrangThai = 'CHO_KICH_HOAT'` khiến sinh viên chưa đăng nhập được, nên chờ vài giây là chấp nhận được | **Không có** trạng thái trung gian an toàn nào |

---

## 0.2 ⭐ Điểm đặc biệt: **X-Ray Phân Tán**

Môn CSDLPT dạy toàn những thứ **không nhìn thấy được**: dữ liệu nằm ở đâu, bao nhiêu dòng đi qua mạng, truy vấn chạy tại site nào, bản sao trễ bao lâu. Hầu hết đồ án chứng minh chúng bằng cách *nói rằng chúng có thật*.

**X-Ray Phân Tán làm chúng hiện ra theo thời gian thực.** Mọi response của API mang theo một trace; giao diện vẽ lại đúng đường đi của request đó qua hệ thống.

```
┌─ X-RAY ──────────────────────────────────────────────┐
│  GET /api/lich-hoc/me            SV: N22DCCN001       │
│                                                        │
│      [HCM] ●━━━━━━━━━━  1 truy vấn ·  8 dòng ·  11ms  │
│      [HN ] ○  không chạm tới                          │
│      [ĐN ] ○  không chạm tới                          │
│                                                        │
│  Dòng qua mạng: 0          Đọc từ: mảnh cục bộ        │
│  Replica dùng: MonHoc      Linked Server: không       │
└────────────────────────────────────────────────────────┘
```

Cùng con mắt đó, khi Admin bấm thống kê toàn hệ thống:

```
┌─ X-RAY ──────────────────────────────────────────────┐
│  GET /api/thong-ke/mon-hoc?ma=INT1339                 │
│                                                        │
│      [HCM] ●━━━━━━━  cục bộ      ·  1 dòng ·   9ms    │
│      [HN ] ●━━━━━━━  OPENQUERY   ·  1 dòng ·  87ms    │
│      [ĐN ] ●━━━━━━━  OPENQUERY   ·  1 dòng ·  91ms    │
│                                                        │
│  Dòng qua mạng: 2   (đã GROUP BY tại từng site)       │
│  Chiến lược: OPENQUERY      [ đổi sang four-part ]    │
└────────────────────────────────────────────────────────┘
```

Bấm *"đổi sang four-part"* — cùng câu hỏi, hiện ngay **84.213 dòng qua mạng · 3.412ms**. Chênh lệch không còn là một dòng trong bảng báo cáo, nó là thứ giám khảo tự tay bấm ra.

> ⚠️ Chỉ số *"dòng qua mạng"* giữa hai SQL Server **không đo được từ tầng ứng dụng** — nó lấy từ actual execution plan và phải được ghi nhãn `derived`. Xem E2 về ranh giới measured/derived.

### Ba thành phần

1. **Trace từng request** — chạm site nào, bao nhiêu câu SQL, bao nhiêu dòng qua mạng, thời gian từng site, có dùng replica hay Linked Server không.
2. **Phòng điều khiển** — tắt/bật một site, giả lập độ trễ link, tạm dừng replication, xem độ sâu Outbox và độ trễ nhân bản.
3. **So sánh trực tiếp** — cùng một báo cáo chạy bằng ba chiến lược (four-part · `OPENQUERY` · backend merge), số liệu hiện cạnh nhau.

### Vì sao nó đắt giá

- Chứng minh **cả ba mức trong suốt cùng lúc**: cùng một request, hai sinh viên khác cơ sở, X-Ray chỉ ra hai site khác nhau được chạm — hiển thị chứ không phải tuyên bố.
- Biến toàn bộ Phần G thành thứ **chạy được trực tiếp**, không phải bảng tĩnh chép vào báo cáo.
- Là công cụ debug thật trong lúc phát triển: nhìn ra ngay khi một request lỡ chạm site thừa.
- **Độc lập với SQL Server** — bản chất là distributed tracing tự viết ở tầng ứng dụng, không phụ thuộc hệ quản trị nào.
- Chi phí: một proxy quanh `DataSource` + một filter + một panel. Không thư viện ngoài, không hạ tầng thêm.

---

# PHẦN A — ĐẶT VẤN ĐỀ
*(mục 2.1 đề bài)*

## A1. Nhu cầu và tầm quan trọng

Một trường đại học có nhiều cơ sở đào tạo đặt tại các tỉnh/thành khác nhau. Mỗi cơ sở vận hành gần như độc lập hằng ngày: tuyển sinh, mở lớp, giảng dạy, nhập điểm. Nhưng vẫn là **một trường**: chung chương trình đào tạo, chung danh mục môn học, chung quy chế, và ban giám hiệu cần nhìn được số liệu toàn hệ thống.

Hệ thống đăng ký tín chỉ là thứ **mọi sinh viên dùng hằng ngày** và **chịu tải cực đại theo đợt**: trong ba ngày mở đăng ký, hàng chục nghìn giao dịch dồn vào cùng vài trăm dòng dữ liệu lớp học phần. Đây vừa là bài toán phân tán theo địa lý, vừa là bài toán tranh chấp cao tại chỗ.

## A2. Vì sao bắt buộc phải dùng CSDL phân tán

**Nếu dùng một CSDL tập trung duy nhất:**

| Vấn đề | Hệ quả cụ thể |
|---|---|
| Độ trễ mạng | Mọi thao tác của sinh viên Hà Nội phải đi ~1.700 km về server TP.HCM. Trong đợt đăng ký, độ trễ này nhân với hàng chục nghìn request |
| Điểm chết đơn lẻ | Server trung tâm sập → **toàn bộ mọi cơ sở** ngừng hoạt động, kể cả những việc thuần túy nội bộ |
| Tải dồn | 100% tải đổ vào một máy, trong khi tải thực tế vốn phân tán tự nhiên theo cơ sở |
| Không khớp tổ chức | Quyền quản trị dữ liệu của mỗi cơ sở bị hòa tan vào một nơi |

**Bốn lý do bắt buộc phải phân tán — xếp theo sức nặng:**

1. **Địa lý.** Các cơ sở cách nhau ~1.700 km. Mọi thao tác của sinh viên Hà Nội phải vượt khoảng cách đó nếu dữ liệu chỉ nằm ở TP.HCM. Đây là ràng buộc **vật lý**, không thể tối ưu bằng phần cứng.
2. **Tự chủ quản trị.** Mỗi cơ sở có Phòng đào tạo riêng, tự mở lớp, tự quản sinh viên của mình, tự chịu trách nhiệm về dữ liệu đó. Ranh giới quyền quản trị dữ liệu phải trùng với ranh giới tổ chức.
3. **Cô lập lỗi.** Với CSDL tập trung, một sự cố ở trung tâm làm **toàn bộ mọi cơ sở** ngừng hoạt động, kể cả những việc thuần túy nội bộ. Đây là rủi ro không chấp nhận được với hệ thống mà mọi sinh viên dùng hằng ngày.
4. **Yêu cầu của môn học** — đề tài yêu cầu triển khai CSDL phân tán đa vị trí.

> ⚠️ **Lưu ý về lập luận tải.** Với quy mô giả định (~92.000 lượt/ngày ≈ 1 req/s trung bình, cao điểm ~5–10 req/s), **một máy chủ CSDL tầm trung hoàn toàn xử lý được**. Vì vậy **không dùng khối lượng truy cập làm lý do phải phân tán** — đó là lập luận yếu và dễ bị bác.
>
> Vai trò thật của bảng tần suất (B2) là khác: nó không chứng minh *có cần phân tán hay không*, mà chứng minh **phân tán thế nào cho đúng** — 99,4% truy cập là cục bộ nên phải local-first; tỉ lệ đọc/ghi 5.300:1 của danh mục nên phải nhân bản; cross-site chỉ 0,5% nên Linked Server chỉ dành cho báo cáo.

**Đặc điểm dữ liệu phù hợp với các kỹ thuật phân tán:**

- **Dữ liệu tự nhiên phân hoạch theo cơ sở** → phân mảnh ngang
- **Truy cập chủ yếu cục bộ (>99%)** → local-first
- **Một phần nhỏ phải dùng chung và thống nhất**, đọc nhiều ghi hiếm → nhân bản
- **Có nhu cầu tổng hợp toàn hệ thống**, tần suất thấp, một nhóm người dùng → truy vấn phân tán

⚠️ **Lưu ý khi mở rộng số cơ sở:** lập luận trên dựa trên **khoảng cách địa lý thật**. Nếu thêm cơ sở phụ *trong cùng thành phố*, lập luận độ trễ không còn dùng được và phải chuyển sang lập luận **quyền tự chủ quản trị + phân tải**. Vì vậy khi triển khai 3 site, chọn **ba thành phố khác nhau** (TP.HCM · Hà Nội · Đà Nẵng).

## A3. Vị trí, nhiệm vụ và dữ liệu

Hệ thống có **ba cơ sở vận hành** cộng **một vai trò Master** tách biệt (xem C0):

| Vị trí | Vai trò | Dữ liệu do vị trí này chịu trách nhiệm ghi |
|---|---|---|
| **`PTITONE_MASTER`** *(vai trò, đặt trên hạ tầng SRV-HCM)* | Publisher + Distributor. **Không chứa dữ liệu vận hành** | **Toàn bộ danh mục dùng chung + danh bạ người dùng toàn trường** |
| **Site HCM** (`PTITONE_HCM`) | Chi nhánh vận hành · Subscriber | SV có cơ sở nhà HCM · GV HCM · Lớp mở tại HCM · Đăng ký & điểm của các lớp đó · Đợt đăng ký HCM |
| **Site HN** (`PTITONE_HN`) | Chi nhánh vận hành · Subscriber | SV có cơ sở nhà HN · GV HN · Lớp mở tại HN · Đăng ký & điểm của các lớp đó · Đợt đăng ký HN |
| **Site ĐN** (`PTITONE_DN`) | Chi nhánh vận hành · Subscriber | Tương tự HN |

> Ba cơ sở vận hành **hoàn toàn đối xứng** — không cơ sở nào có đặc quyền lên dữ liệu của cơ sở khác.

## A4. Đối tượng sử dụng

| Đối tượng | Quy mô 🔶 | Nhiệm vụ chính | Phạm vi dữ liệu |
|---|---|---|---|
| **Sinh viên** | 8.000 HCM · 5.000 HN · 3.000 ĐN | Đăng ký học phần, xem lịch học, xem điểm, đăng ký lớp liên cơ sở | Chủ yếu cơ sở nhà |
| **Giảng viên** | ~300 · ~200 · ~120 | Xem danh sách lớp, nhập điểm | Chỉ lớp mình dạy, tại cơ sở mình |
| **Admin cơ sở** (Phòng đào tạo cơ sở) | ~5 mỗi cơ sở | Mở lớp học phần, mở đợt đăng ký của cơ sở mình | Chỉ cơ sở mình |
| **Admin Master** (Phòng đào tạo trung tâm) | ~3, đặt tại HCM | Quản lý danh mục dùng chung, quản lý danh bạ người dùng, thống kê toàn hệ thống | Danh mục (ghi) + toàn hệ thống (đọc tổng hợp) |

---

# PHẦN B — PHÂN TÍCH
*(mục 2.2.1 đề bài)*

## B1. Các chức năng chính truy cập vào dữ liệu

| # | Chức năng | Đối tượng | Bảng chạm tới | Kiểu truy cập |
|---|---|---|---|---|
| **1** | **Đăng nhập, phân giải cơ sở** | Tất cả | `DanhBaNguoiDung` (replica), `TaiKhoan` | Cục bộ |
| **2** | **Xem lịch học** | SV | `DangKyHocPhan`, `LopHocPhan`, `MonHoc`(replica), `YeuCauHocLienCoSo` | Cục bộ |
| **3** | **Đăng ký học phần tại cơ sở mình** | SV | `LopHocPhan`, `DangKyHocPhan`, `DotDangKy` | Cục bộ · ghi · tranh chấp cao |
| **4** | **Xem điểm / bảng điểm** | SV | `Diem`, `BangDiemMirror` | Cục bộ |
| **5** | **Xem danh sách lớp** | GV | `LopHocPhan`, `DangKyHocPhan` | Cục bộ |
| **6** | **Nhập điểm** | GV | `Diem`, `OutboxSuKien` | Cục bộ · ghi |
| **7** | **Mở lớp học phần** | Admin cơ sở | `LopHocPhan`, `MonHoc`(replica) | Cục bộ · ghi |
| **8** | **Quản lý danh mục dùng chung** | Admin Master | `MonHoc`, `Khoa`, `ChuongTrinhDaoTao`, `HocKy`, `DanhBaNguoiDung` | Chỉ tại Master · ghi → nhân bản |
| **9** | **Thống kê toàn hệ thống** | Admin Master | Tổng hợp mọi site | **Cross-site** qua Linked Server |
| 10 | Duyệt lớp mở tại cơ sở khác ➕ | SV | `LopHocPhan` tại site kia | **Cross-site**, đọc trực tiếp qua DataSource |
| 11 | Đăng ký lớp liên cơ sở ➕ | SV | `YeuCauHocLienCoSo` (Home) + `LopHocPhan`,`DangKyHocPhan` (Host) | Saga · 2 giao dịch cục bộ |
| 12 | Đồng bộ điểm về cơ sở nhà ➕ | Hệ thống | `OutboxSuKien` (Host) → `BangDiemMirror` (Home) | Nền · bất đồng bộ |
| **13** | **Chuyển cơ sở sinh viên** ⭐ | Admin Master | `SinhVien`, `TaiKhoan` (2 site) + `DanhBaNguoiDung` (Master) | **GIAO DỊCH PHÂN TÁN (2PC)** — nguyên tử trên 3 CSDL |

## B2. **Bảng tần suất truy cập tại các vị trí**

> ⚠️ Đề bài yêu cầu bảng này đích danh. Nó là **căn cứ chính thức** để biện minh mọi quyết định phân mảnh — trong báo cáo, mỗi quyết định ở Phần C phải trỏ ngược về đây.
> 🔶 Số dưới đây theo giả định A4. Nhóm phải rà lại và ghi rõ giả định trong báo cáo.

### Ngày cao điểm (trong đợt đăng ký)

| # | Chức năng | HCM | HN | ĐN | Bảng chính | Cục bộ / Cross-site |
|---|---|---:|---:|---:|---|---|
| — | Tra cứu danh mục môn học | 40.000 | 25.000 | 15.000 | `MonHoc` (replica) | **Cục bộ nhờ nhân bản** |
| 2 | Xem lịch học | 24.000 | 15.000 | 9.000 | `DangKyHocPhan` | **Cục bộ** |
| 3 | Đăng ký học phần | 16.000 | 10.000 | 6.000 | `LopHocPhan`, `DangKyHocPhan` | **Cục bộ** |
| 1 | Đăng nhập | 12.000 | 7.500 | 4.500 | `DanhBaNguoiDung`, `TaiKhoan` | **Cục bộ** |
| 10 | Duyệt lớp cơ sở khác | 320 | 200 | 120 | `LopHocPhan` site kia | Cross-site |
| 11 | Đăng ký liên cơ sở | 160 | 100 | 60 | Saga 2 site | Cross-site |
| 9 | Thống kê toàn hệ thống | 40 | 0 | 0 | Mọi site | Cross-site |
| | **Tổng lượt** | **92.520** | **57.800** | **34.680** | | |
| | **Trong đó cross-site** | **520** | **300** | **180** | | |
| | **Tỉ lệ cross-site** | **0,56%** | **0,52%** | **0,52%** | | |

### Ngày thường

| # | Chức năng | HCM | HN | ĐN | Cục bộ / Cross-site |
|---|---|---:|---:|---:|---|
| 2 | Xem lịch học | 8.000 | 5.000 | 3.000 | **Cục bộ** |
| 4 | Xem điểm | 6.000 | 3.750 | 2.250 | **Cục bộ** |
| 5 | Xem danh sách lớp (GV) | 900 | 600 | 360 | **Cục bộ** |
| 6 | Nhập điểm (GV) | 400 | 250 | 150 | **Cục bộ** |
| 7 | Mở lớp học phần | 20 | 15 | 10 | **Cục bộ** |
| 8 | Sửa danh mục dùng chung | 15 | **0** | **0** | Chỉ tại Master |
| 9 | Thống kê toàn hệ thống | 25 | 0 | 0 | Cross-site |
| | **Tỉ lệ cross-site** | **~0,16%** | **~0,00%** | **~0,00%** | |

### Bốn kết luận rút ra — đây là *lý do thiết kế* của toàn bộ Phần C

1. **Hơn 99,4% lượt truy cập là cục bộ** → kiến trúc **local-first** là bắt buộc, không phải lựa chọn. Đưa mọi request qua Linked Server nghĩa là trả giá mạng cho 99,4% lượng truy cập vốn không cần.
2. **Tra cứu danh mục là loại truy cập nhiều nhất (80.000 lượt/ngày cao điểm) nhưng gần như không bao giờ bị sửa (15 lượt/ngày, chỉ tại một nơi)** → tỉ lệ đọc/ghi ~5.300:1. Đây là **hồ sơ dữ liệu điển hình của nhân bản**, và là căn cứ định lượng cho quyết định replicate.
3. **Đăng ký học phần là điểm nóng tranh chấp** — 32.000 lượt/ngày dồn vào vài trăm dòng `LopHocPhan` → quyết định đặt `DangKyHocPhan` **cùng site với bộ đếm sức chứa**, để ràng buộc được bảo vệ bằng **một giao dịch cục bộ** (xem C3, D4).
4. **Cross-site chỉ ~0,5% và tập trung vào Admin** → Linked Server chỉ phục vụ báo cáo, không phục vụ nghiệp vụ hằng ngày.

## B3. **Ma trận phân quyền**

`R` = đọc · `W` = ghi · `—` = không quyền · `(L)` = giới hạn trong phạm vi của mình

| Bảng | Sinh viên | Giảng viên | Admin cơ sở | Admin Master |
|---|:---:|:---:|:---:|:---:|
| `SinhVien` | R (L: chính mình) | R (L: SV trong lớp mình) | R/W (L: cơ sở mình) | R (mọi site) |
| `GiangVien` | R (L: GV dạy mình) | R (L: chính mình) | R/W (L: cơ sở mình) | R |
| `TaiKhoan` | W (L: đổi mật khẩu) | W (L: đổi mật khẩu) | R/W (L: cơ sở mình) | R/W |
| `LopHocPhan` | R | R (L: lớp mình dạy) | R/W (L: cơ sở mình) | R (mọi site) |
| `DangKyHocPhan` | R (L: chính mình) · W qua chức năng đăng ký | R (L: lớp mình dạy) | R/W (L: cơ sở mình) | R (mọi site) |
| `Diem` | R (L: chính mình) | R/W (L: lớp mình dạy) | R (L: cơ sở mình) | R (mọi site) |
| `DotDangKy` | R | R | R/W (L: cơ sở mình) | R |
| `MonHoc`, `Khoa`, `ChuongTrinhDaoTao`, `HocKy` | R | R | R | **R/W — chỉ tại Master** |
| `DanhBaNguoiDung` | — | — | R | **R/W — chỉ tại Master** |
| `YeuCauHocLienCoSo` ➕ | R (L: chính mình) · W qua chức năng | — | R (L: cơ sở mình) | R |
| `BangDiemMirror` ➕ | R (L: chính mình) | — | R | R |
| `OutboxSuKien` ➕ | — | — | — | R |
| **Chuyển cơ sở sinh viên** ⭐ | — | — | — | **Chỉ Admin Master** — thao tác duy nhất dùng giao dịch phân tán |

### **D13 — Ba lớp phân quyền, mỗi lớp một trách nhiệm**

Ứng dụng kết nối bằng **một service account cho mỗi DataSource**, nên trigger *không biết* người dùng cuối là ai. Vì vậy tách trách nhiệm rõ ràng:

```
TRIGGER            →  TOÀN VẸN THEO SITE (không cần biết người dùng)
                      • chặn ghi dòng có MaCoSo ≠ mã cơ sở của chính DB này
                      • chặn mọi ghi vào bảng nhân bản tại Subscriber
                      • chặn sửa Diem khi lớp đã khóa điểm

DATABASE ROLE      →  QUYỀN THEO VAI TRÒ (GRANT / DENY)
+ GRANT/DENY          • r_SinhVien / r_GiangVien / r_AdminCoSo / r_AdminMaster
                      • demo: đăng nhập SSMS bằng từng login, thử thao tác bị cấm,
                        chụp màn hình thông báo từ chối

TẦNG ỨNG DỤNG      →  QUYỀN THEO DÒNG
                      • "chỉ xem điểm của chính mình"
                      • lọc theo claim trong JWT
```

⚠️ **Không dùng `SESSION_CONTEXT` để truyền danh tính người dùng xuống trigger** — dễ rò rỉ qua connection pool nếu quên reset, sinh lỗi bảo mật âm thầm.

## B4. Phân tích chức năng của từng vị trí

| | **`PTITONE_MASTER`** (vai trò) | **`PTITONE_HCM` · `PTITONE_HN` · `PTITONE_DN`** (cơ sở vận hành) |
|---|---|---|
| **Vai trò CSDL** | Publisher + Distributor | Subscriber |
| **Ghi được** | Danh mục dùng chung + danh bạ SV | Chỉ dữ liệu vận hành của cơ sở mình |
| **Chỉ đọc** | — | Danh mục dùng chung và danh bạ (bản sao) |
| **Chức năng** | Quản lý danh mục toàn trường · Quản lý danh bạ SV · Nguồn của luồng nhân bản | **Ba cơ sở đối xứng hoàn toàn** — cùng một tập chức năng vận hành |
| **Linked Server** | — | Chỉ `PTITONE_HCM` giữ liên kết tới SRV-HN và SRV-DN, phục vụ báo cáo tổng hợp |

**Bất đối xứng còn lại — và nó nhỏ:** vì `PTITONE_MASTER` nằm cùng instance với `PTITONE_HCM`, máy SRV-HCM chịu thêm tải của Distributor. Tải này cực thấp (~15 lượt ghi/ngày). Hướng mở rộng: chuyển `PTITONE_MASTER` sang một instance riêng — **không thay đổi gì về mặt logic**, chỉ đổi connection string.

⚠️ Trước khi tách database Master, thiết kế cũ gộp danh mục vào chính CSDL vận hành của HCM, khiến "cơ sở HCM" và "vai trò Master" bị lẫn làm một. Việc tách ở C0 xóa bỏ sự nhập nhằng này.

## B5. **Chức năng ở máy trạm và máy chủ**

Hệ thống theo mô hình **Client/Server** — xem C6 để biết lý do không chọn ngang hàng.

```
┌──── MÁY TRẠM (Client) ────────────────────────────────┐
│ • Trình duyệt chạy giao diện React                     │
│ • Nhập liệu, kiểm tra định dạng phía người dùng        │
│ • Hiển thị kết quả, phân trang                         │
│ • Hiển thị bảng X-Ray ⭐                               │
│ • KHÔNG kết nối trực tiếp tới CSDL                     │
│ • KHÔNG biết dữ liệu nằm ở site nào                    │
└────────────────────────────────────────────────────────┘
                        │ HTTPS + JWT
┌──── MÁY CHỦ ỨNG DỤNG ─────────────────────────────────┐
│ • Xác thực, phân quyền theo vai trò                    │
│ • SiteContext: phân giải cơ sở qua DanhBaNguoiDung      │
│ • Định tuyến tới đúng DataSource (local-first)         │
│ • Điều phối saga liên cơ sở                            │
│ • OutboxWorker chạy nền                                │
│ • Thu thập trace X-Ray ⭐                              │
└────────────────────────────────────────────────────────┘
   │ JDBC/VPN       │ JDBC/VPN       │ JDBC/VPN     │ DS_MASTER
   │                │                │              │ (chỉ quản trị danh mục)
┌──▼──────────┐ ┌───▼─────────┐ ┌────▼────────┐ ┌───▼──────────┐
│  PTITONE_HCM    │ │  PTITONE_HN     │ │  PTITONE_DN     │ │ PTITONE_MASTER   │
│ Subscriber  │ │ Subscriber  │ │ Subscriber  │ │ Publisher    │
│ Linked Srv  │ │             │ │             │ │ Distributor  │
│ → HN, ĐN    │ │             │ │             │ │              │
│ Trigger     │ │ Trigger     │ │ Trigger     │ │              │
│ Roles       │ │ Roles       │ │ Roles       │ │ Roles        │
└─────────────┘ └─────────────┘ └─────────────┘ └──────────────┘
  ── SRV-HCM ──                                    ── SRV-HCM ──
                  ── SRV-HN ──    ── SRV-DN ──
       (SQL Server Agent chạy trên cả ba máy chủ)
```

## B6. **Mô hình thực thể liên kết (ERD)**

```
        ┌──────────┐        ┌────────────────────┐
        │   Khoa   │───1:N──│ ChuongTrinhDaoTao  │
        └────┬─────┘        └─────────┬──────────┘
             │ 1:N                    │ 1:N
        ┌────▼─────┐             ┌────▼──────┐
        │  MonHoc  │             │ SinhVien  │
        └────┬─────┘             └────┬──────┘
             │ 1:N                    │ 1:N
             │      ┌───────────┐     │
        ┌────▼──────▼───┐  ┌────▼─────▼─────────┐
        │  LopHocPhan   │──│   DangKyHocPhan    │
        └──┬────────┬───┘  └─────────┬──────────┘
           │        │ N:1            │ 1:1
           │   ┌────▼──────┐   ┌─────▼────┐
           │   │ GiangVien │   │   Diem   │
           │   └───────────┘   └──────────┘
      ┌────▼─────┐   ┌────────────┐
      │  HocKy   │───│ DotDangKy  │
      └──────────┘   └────────────┘

  MonHocTienQuyet : MonHoc ──N:M── MonHoc  (tự quan hệ)
  DanhBaNguoiDung  : danh bạ định vị, 1:1 logic với SinhVien

  ➕ YeuCauHocLienCoSo : SinhVien 1:N — LopHocPhan N:1
  ➕ BangDiemMirror    : projection của Diem ở site khác
  ➕ OutboxSuKien      : hàng đợi sự kiện, không có khóa ngoại
```

---

# PHẦN C — THIẾT KẾ
*(mục 2.2.2 đề bài)*

## C0. Hai chế độ ghi trong hệ thống

> Mục này nên đọc trước tất cả phần còn lại của Phần C, vì nó là khung giải thích cho mọi quyết định phía sau.

Một điều thường bị hiểu lệch: **hệ thống này không phải single-master.** Nó có **hai chế độ ghi cho hai loại dữ liệu khác nhau**.

```
DỮ LIỆU VẬN HÀNH    SinhVien · GiangVien · TaiKhoan · DotDangKy
                    LopHocPhan · DangKyHocPhan · Diem · YeuCau · Outbox

   ▸ MỖI CƠ SỞ LÀ MASTER CỦA CHÍNH MÌNH
   ▸ ghi hoàn toàn độc lập — không phối hợp, không xin phép site nào
   ▸ HCM KHÔNG có bất kỳ quyền ghi nào lên dữ liệu vận hành của HN
   ▸ ~95% khối lượng dữ liệu · ~99,9% lượt ghi

DỮ LIỆU THAM CHIẾU  CoSo · Khoa · ChuongTrinhDaoTao · MonHoc
                    MonHocTienQuyet · HocKy · DanhBaNguoiDung

   ▸ MỘT NƠI GHI DUY NHẤT, NHÂN BẢN RA MỌI NƠI
   ▸ 7 bảng nhỏ · ~15 lượt ghi/ngày · tỉ lệ đọc/ghi ~5.300:1
```

Nói cách khác: **ghi được phân hoạch theo mảnh cho dữ liệu nghiệp vụ, và single-master cho dữ liệu tham chiếu.**

Đây là **một mô hình đã được kiểm chứng** để tách dữ liệu tham chiếu khỏi dữ liệu vận hành — không phải mô hình phổ quát. Nhiều hệ multi-region chọn hướng khác: multi-master có giải quyết xung đột (DynamoDB global tables, Cassandra, CosmosDB multi-write), hoặc phục vụ dữ liệu tham chiếu qua API thay vì nhân bản. Mô hình một-nơi-ghi phù hợp ở đây vì hồ sơ truy cập của dữ liệu tham chiếu rất lệch: đọc 5.300 lần cho mỗi lần ghi, và chỉ một phòng ban có thẩm quyền sửa.

### Vì sao dữ liệu tham chiếu dùng single-master

Single-writer cộng nhiều bản sao chỉ đọc là **hình dạng phổ biến nhất** trong hệ phân tán, không phải một sự thỏa hiệp:

| Hệ thống | Hình dạng |
|---|---|
| PostgreSQL | 1 primary ghi · N read replica |
| MySQL | 1 source · N replica |
| **MongoDB replica set** | **1 primary nhận mọi lệnh ghi · N secondary chỉ đọc** — mặc định, không phải tùy chọn |
| Kafka | mỗi partition có 1 leader · N follower |
| etcd / Consul / ZooKeeper (Raft) | 1 leader nhận ghi · follower nhân bản |
| CDN | origin là nguồn sự thật · edge là bản sao đọc |

Trong doanh nghiệp nó có tên riêng: **Master Data Management** — dữ liệu tham chiếu luôn có một *golden record* và một nơi ghi, vì loại dữ liệu này đọc rất nhiều, ghi rất hiếm, và phải giống nhau ở mọi nơi.

**Khi nào mới cần multi-master?** Khi nhiều vùng cùng phải ghi *cùng một bản ghi* với độ trễ thấp, hoặc client hoạt động offline rồi đồng bộ sau. Cả hai đều không đúng ở đây: danh mục do **một phòng ban** sửa **15 lần/ngày**. Chọn multi-master nghĩa là mua thêm bài toán giải quyết xung đột mà không đổi lấy gì.

### Master là một VAI TRÒ, không phải một cơ sở

Để hai khái niệm này không bị lẫn, dữ liệu tham chiếu được đặt trong một **database riêng biệt**, không nằm chung với database vận hành của cơ sở TP.HCM:

```
SRV-HCM (một instance SQL Server, hai database)
├── PTITONE_MASTER   ← 7 bảng tham chiếu · là PUBLISHER · KHÔNG chứa dữ liệu vận hành
└── PTITONE_HCM      ← mảnh vận hành của cơ sở HCM · là SUBSCRIBER
```

Nhờ vậy **lược đồ logic trở nên đối xứng**: ba cơ sở vận hành có hình dạng giống hệt nhau và **đều là Subscriber**, cộng thêm một vai trò Master tách bạch.

> **Master không phải là một cơ sở. Master là một *vai trò*** — vai trò đó được đặt ở đâu về mặt vật lý là quyết định triển khai riêng biệt (D15).

⚠️ Đối xứng ở đây là **đối xứng logic**. Về mặt vật lý, chừng nào Master còn colocate với SRV-HCM thì máy đó vẫn gánh thêm Distributor và Global Reporting Node — xem "Bốn vai trò" ngay dưới.

Lợi ích:
- Database vận hành `PTITONE_HCM` **không còn là Publisher** → hết bất đối xứng giữa các cơ sở
- Sau này chuyển Master sang máy riêng thì **không thay đổi gì về mặt logic**, chỉ đổi connection string
- Sơ đồ định vị (C5) đọc rõ ràng hơn hẳn

Chi phí: thêm một database và một subscription cục bộ. Khoảng 30 phút ở tuần 3 — và subscriber cùng instance còn dễ cấu hình hơn subscriber qua VPN.

Chi phí phụ: tầng ứng dụng cần thêm một DataSource `DS_MASTER`, **chỉ dùng cho màn hình quản trị danh mục**. Mọi thao tác đọc danh mục vẫn đi vào replica trong database vận hành của site mình.

### Bốn vai trò cần phân biệt — đừng gom hết thành "Master"

Chữ "Master" đang che bốn thứ khác nhau. Tách bạch ra thì mới nói chính xác được, và mới thấy chỗ nào tách được chỗ nào không:

| Vai trò | Bản chất | Tách được không |
|---|---|---|
| **Master Data Authority** | Thẩm quyền quản trị dữ liệu tham chiếu — khái niệm **logic**, không phải máy móc | ❌ Theo định nghĩa là duy nhất |
| **Publisher** | Vai trò trong SQL Server Replication: database công bố các article | Gắn liền với database chứa bản gốc (`PTITONE_MASTER`) |
| **Distributor** | Vai trò trong SQL Server Replication: giữ distribution database, chạy các Agent | ✅ Tách được, nhưng **không nên** — thêm một máy phải bật liên tục |
| **Global Reporting Node** | Nơi định nghĩa Linked Server và chạy truy vấn tổng hợp toàn hệ thống | ✅ Tách được, và **tách thì đối xứng hơn** |

⚠️ **Hệ quả quan trọng:** khi Master colocate với SRV-HCM, `PTITONE_HCM` vẫn phải giữ Linked Server và chạy mọi báo cáo → **HCM vẫn đặc biệt về mặt vật lý**, dù đã tách ở mức database. Đối xứng chỉ trọn vẹn khi Global Reporting Node có máy riêng.

### D15 — Phương án triển khai theo số máy

> **Kiến trúc logic là `1 Master/Publisher → N Subscribers`. Master colocate hay chạy máy riêng chỉ là quyết định triển khai vật lý.**
>
> Phát biểu này khớp đúng thuật ngữ môn học: **lược đồ logic độc lập với lược đồ định vị vật lý.** Vì vậy quyết định dưới đây **không làm thay đổi một lược đồ nào** ở C3, C4, C5 — chỉ đổi cột "đặt ở đâu".

| Số máy khả dụng | Phương án | Đánh đổi |
|---|---|---|
| **2 máy** | 2 site vận hành · Master + Distributor + Reporting colocate trên SRV-HCM | An toàn nhất, nhưng "site còn lại" là duy nhất nên Location Transparency khó chứng minh |
| **3 máy** | **3 site vận hành · Master colocate trên SRV-HCM** | ✅ **Ưu tiên số site.** Phân mảnh, nhân bản 1→N, danh bạ định vị, join 3 chiều đều nằm trong barem; tách tầng Master thì không |
| **4 máy** | **3 site vận hành + 1 nút `SRV-MASTER` riêng** | ⭐ Lý tưởng — đối xứng trọn vẹn. Nhưng mỗi máy thêm vào làm **xác suất đủ mặt trong buổi làm việc giảm rõ rệt** (xem I2b) |

**Nếu chọn 4 máy — hai lập luận kỹ thuật (không chỉ thẩm mỹ):**

1. **Distribution database bị ghi cho mọi giao dịch được nhân bản.** Đặt Distributor chung với `PTITONE_HCM` là thêm tải ghi I/O lên chính site đang chịu 92.000 lượt/ngày. Tải này nhỏ (~15 giao dịch/ngày) nhưng là chi phí thật.
2. **Global report kéo dữ liệu qua mạng rồi tổng hợp cục bộ** — công việc này nên tách khỏi máy đang phục vụ nghiệp vụ giờ cao điểm.

⚠️ **Chọn máy nào làm `SRV-MASTER`:** tiêu chí là **ít bị mang đi lại nhất (ưu tiên máy để bàn), và đĩa khá** — *không phải* "máy nào yếu thì đưa vào đó". Distributor làm việc thật: Log Reader Agent đọc transaction log, distribution database bị ghi liên tục, và lúc sinh snapshot ban đầu thì I/O rất nặng.

⚠️ **Snapshot folder đi theo Distributor, không đi theo Publisher.** UNC share phải nằm trên máy chạy Distributor: 3 máy → share trên SRV-HCM; 4 máy → share trên SRV-MASTER. Ảnh hưởng trực tiếp bước F6.

### Những gì đang đơn giản hóa so với production

> Nêu rõ bốn dòng này trong mục "hạn chế đã nhận diện" luôn được đánh giá cao hơn là im lặng rồi bị hỏi vặn.

| Production làm gì | Hệ thống này làm gì | Có bù không |
|---|---|---|
| Tự động bầu leader / failover khi Master chết | Không có. → **Master là SPOF của *control/reporting plane*, KHÔNG phải SPOF của *data plane*.** Mọi cơ sở vẫn vận hành đầy đủ khi Master chết | Không — nêu đúng bằng câu này; đây là đánh đổi chấp nhận được |
| Master đặt trên hạ tầng riêng | Đã tách ở **mức database**; tách ở mức máy là quyết định triển khai (D15) | **Có, một phần** — và phần còn lại chỉ là đổi connection string |
| Bảo đảm read-your-writes | Admin sửa xong, site khác vài giây sau mới thấy | Không — đo độ trễ (B6), gọi đúng tên là nhất quán cuối |
| Quorum / xác nhận N/2+1 | Không có | Không — ngoài phạm vi |

## C1. **Thiết kế CSDL quan hệ**

Quy ước: `PK` khóa chính · `FK` khóa ngoại · `UQ` duy nhất · **[R]** bảng được nhân bản · **[F]** bảng được phân mảnh · **[P]** read model (projection, không phải nguồn sự thật)

### Nhóm 1 — Dữ liệu tham chiếu, nhân bản một chiều từ Master **[R]**

> Bản gốc nằm trong database **`PTITONE_MASTER`**. Mỗi cơ sở vận hành giữ một bản sao chỉ đọc (xem C0).

| Bảng | Cột chính |
|---|---|
| `CoSo` | `MaCoSo` PK · `TenCoSo` · `ThanhPho` · `LaMaster` · `DiaChi` |
| `Khoa` | `MaKhoa` PK · `TenKhoa` |
| `ChuongTrinhDaoTao` | `MaCTDT` PK · `TenCTDT` · `MaKhoa` FK · `TongTinChi` |
| `MonHoc` | `MaMonHoc` PK · `TenMonHoc` · `SoTinChi` · `MaKhoa` FK |
| `MonHocTienQuyet` | `MaMonHoc` FK · `MaMonTienQuyet` FK · PK kép |
| `HocKy` | `MaHocKy` PK · `TenHocKy` · `NamHoc` · `NgayBatDau` · `NgayKetThuc` |
| `DanhBaNguoiDung` | `TenDangNhap` PK · `MaCoSo` FK *(HCM/HN/DN/**MASTER**)* · `LoaiNguoiDung` *(SINH_VIEN / GIANG_VIEN / ADMIN_CO_SO / ADMIN_MASTER)* · `MaThucThe` UQ *(MaSinhVien hoặc MaGiangVien)* · `TrangThai` · `NgayCapNhat` |
| `TaiKhoanMaster` | `TenDangNhap` PK · `MatKhauHash` · `VaiTro` — **chỉ tồn tại trong `PTITONE_MASTER`**, dành cho Admin Master. Không nhân bản |

> `CoSo` được nhân bản chứ không hardcode — đây là điều làm cho thiết kế đúng cho **N cơ sở**. Thêm một cơ sở = thêm một dòng + một subscription, không sửa code.

### Nhóm 2 — Dữ liệu phân mảnh ngang theo cơ sở **[F]**

| Bảng | Cột chính | Vị từ phân mảnh |
|---|---|---|
| `SinhVien` | `MaSinhVien` PK · `HoTen` · `NgaySinh` · `MaCoSoNha` FK · `MaCTDT` FK · `TrangThai` · `SoTinChiTichLuy` · `SoMonLienCoSo` | `MaCoSoNha = <site>` |
| `GiangVien` | `MaGiangVien` PK · `HoTen` · `MaCoSo` FK · `MaKhoa` FK · `HocVi` | `MaCoSo = <site>` |
| `TaiKhoan` | `TenDangNhap` PK · `MatKhauHash` · `VaiTro` · `MaThucThe` · `MaCoSo` FK | `MaCoSo = <site>` |
| `DotDangKy` | `MaDot` PK · `MaHocKy` FK · `MaCoSo` FK · `ThoiGianMo` · `ThoiGianDong` · `TrangThai` | `MaCoSo = <site>` |
| `LopHocPhan` | `MaLopHP` PK · `MaMonHoc` FK · `MaHocKy` FK · `MaCoSoHost` FK · `MaGiangVien` FK · `SoLuongToiDa` · `SoLuongDaDangKy` · `ThoiGianHoc` · `PhongHoc` · `TrangThai` · `ChoPhepLienCoSo` | `MaCoSoHost = <site>` |

> ⚠️ `SoMonLienCoSo` trên `SinhVien` là **cờ điều kiện fan-out**: chỉ khi cột này > 0 thì hệ thống mới hỏi sang site khác. Nhờ nó, ~98% request đọc là thuần cục bộ.
> ⚠️ `DotDangKy` **phải là bảng cục bộ**, không nhân bản — mỗi cơ sở có lịch đăng ký riêng, mà Subscriber thì không ghi được.

### Nhóm 3 — Dữ liệu phân mảnh ngang dẫn xuất **[F]**

| Bảng | Cột chính | Vị từ dẫn xuất |
|---|---|---|
| `DangKyHocPhan` | `MaLopHP` FK · `MaSinhVien` · `MaCoSoNhaSV` · `HoTenSinhVien` · `NgayDangKy` · `TrangThai` · `MaYeuCau` · PK (`MaLopHP`,`MaSinhVien`) | **Bậc 1** — theo `LopHocPhan.MaCoSoHost` |
| `Diem` | `MaLopHP` FK · `MaSinhVien` · `DiemChuyenCan` · `DiemGiuaKy` · `DiemCuoiKy` · `DiemTongKet` · `Version` · `NgayCongBo` · PK (`MaLopHP`,`MaSinhVien`) | **Bậc 2** — `Diem ⋉ DangKyHocPhan ⋉ LopHocPhan` |

> `MaCoSoNhaSV` và `HoTenSinhVien` được **cố ý phi chuẩn hóa** vào `DangKyHocPhan`. Nếu không, site Host phải bắn một truy vấn chéo site cho **từng sinh viên khách** chỉ để in danh sách lớp — vừa chậm, vừa gãy khi site kia offline. Hai cột copy rẻ hơn nhiều so với một phụ thuộc chéo site.

### Nhóm 3b — Schema học vụ còn thiếu ở bản trước

| Bảng | Cột chính | Vì sao cần |
|---|---|---|
| `CTDT_MonHoc` **[R]** | `MaCTDT` FK · `MaMonHoc` FK · `HocKyGoiY` · `BatBuoc` · PK kép | **Chưa có ở bản trước.** Không có bảng nối này thì không biết một chương trình đào tạo gồm những môn nào → không xét được tiến độ, không gợi ý được môn, không tính được điều kiện tốt nghiệp |
| `LichHoc` **[F]** | `MaLopHP` FK · `Thu` (2–8) · `TietBatDau` · `SoTiet` · `PhongHoc` · `TuanBatDau` · `TuanKetThuc` | **Thay cho cột `ThoiGianHoc` dạng chuỗi.** Một lớp có thể học nhiều buổi/tuần, và kiểm trùng lịch cần so sánh có cấu trúc chứ không so chuỗi |

`LichHoc` phân mảnh dẫn xuất theo `LopHocPhan`, cùng site với lớp.

**Kiểm trùng lịch** trở thành một phép so khoảng có thể viết bằng SQL:

```sql
-- Trùng khi: cùng thứ, khoảng tiết giao nhau, và khoảng tuần giao nhau
WHERE a.Thu = b.Thu
  AND a.TietBatDau < b.TietBatDau + b.SoTiet
  AND b.TietBatDau < a.TietBatDau + a.SoTiet
  AND a.TuanBatDau <= b.TuanKetThuc
  AND b.TuanBatDau <= a.TuanKetThuc
```

### Quy tắc học vụ — phải chốt trước khi viết code

| Quy tắc | Giá trị đề xuất 🔶 |
|---|---|
| Công thức điểm tổng kết | `0.1 × ChuyenCan + 0.3 × GiuaKy + 0.6 × CuoiKy`, làm tròn 1 chữ số |
| Ngưỡng đạt | `DiemTongKet >= 4.0` |
| Điều kiện học lại | Chỉ cho đăng ký lại môn đã có `DiemTongKet < 4.0`; lần học lại **không xoá** bản ghi cũ, điểm cao nhất được tính |
| Trần tín chỉ mỗi kỳ | 24 tín chỉ *(học kỳ chính)* — dùng cho bộ đếm ở D4 |
| Điều kiện tiên quyết | Phải **đạt** môn tiên quyết, không chỉ đã học |

> 🔶 Đây là **giả định**. Nhóm phải đối chiếu quy chế thật của trường và ghi rõ nguồn trong báo cáo — đây là loại chi tiết giám khảo hay hỏi vì nó thuộc nghiệp vụ, không thuộc kỹ thuật.

### Nhóm 4 — Cơ chế liên cơ sở ➕

| Bảng | Cột chính | Ghi chú |
|---|---|---|
| `YeuCauHocLienCoSo` **[F]** | `MaYeuCau` PK · `MaSinhVien` · `MaLopHP` · `MaCoSoHost` · `TrangThai` · `LyDoTuChoi` · `SoLanThu` · **snapshot:** `MaMonHoc`,`TenMonHoc`,`SoTinChi`,`ThoiGianHoc`,`PhongHoc`,`ThoiDiemDongBo` | Đặt tại **cơ sở nhà**. Vừa là trạng thái saga, **vừa là read model của lớp học ở site kia** |
| `BangDiemMirror` **[P]** | `MaSinhVien`+`MaLopHP` PK · `MaMonHoc` · `TenMonHoc` · `SoTinChi` · `DiemTongKet` · `Version` · `LastSyncedAt` · `SyncStatus` | Đặt tại **cơ sở nhà**. **Không phải nguồn sự thật** — nguồn sự thật là `Diem` ở site Host |
| `OutboxSuKien` **[F]** | `EventId` PK · `LoaiSuKien` · `KhoaThucThe` · `NoiDung` (JSON) · `Version` · `TrangThai` · `SoLanThu` · `ThoiDiemTao` · `ThoiDiemXuLy` | Đặt tại **mọi site**. Hàng đợi phát sự kiện ra ngoài |
| `KetQuaXuLyYeuCau` **[F]** *(inbox / outcome)* | `MaYeuCau` PK · `KetQua` (`DA_DUYET`/`TU_CHOI`) · `MaLoi` · `LyDo` · `MaLopHP` · `MaSinhVien` · `ThoiDiemXuLy` | Đặt tại **site Host**. **Bắt buộc để saga idempotent** — xem D3 |

> ⚠️ **Kỷ luật quan trọng:** `YeuCauHocLienCoSo` chỉ giữ **trạng thái saga + snapshot đăng ký**. Không nhét thêm mirror điểm, mirror lịch, mirror gì khác vào đây — nếu cần thì tạo projection riêng như `BangDiemMirror`. Bảng này không được phép biến thành "kho dữ liệu từ xa của sinh viên".
> **Quy ước đặt tên:** mọi bảng read model đều mang hậu tố `Mirror`. Nhìn tên bảng là biết nó không authoritative.

## C2. **Ownership Matrix**

> Nguyên tắc bao trùm: **mỗi dữ liệu quan trọng có đúng một nơi chịu trách nhiệm ghi.**

| Thực thể | Chủ sở hữu (ghi) | Kỹ thuật | Đọc ở đâu | Ghi chú |
|---|---|---|---|---|
| `CoSo` | **`PTITONE_MASTER`** | Nhân bản | Replica cục bộ | Cấu hình topology là **dữ liệu**, không phải code |
| `Khoa`, `ChuongTrinhDaoTao` | **`PTITONE_MASTER`** | Nhân bản | Replica cục bộ | |
| `MonHoc`, `MonHocTienQuyet` | **`PTITONE_MASTER`** | Nhân bản | Replica cục bộ | Bảng bị đọc nhiều nhất hệ thống |
| `HocKy` | **`PTITONE_MASTER`** | Nhân bản | Replica cục bộ | Chỉ lịch chung toàn trường |
| `DanhBaNguoiDung` | **`PTITONE_MASTER`** | Nhân bản | Replica cục bộ | **Danh bạ định vị** — nền tảng của Location Transparency |
| `SinhVien` | **Cơ sở nhà** | Phân mảnh ngang | Cục bộ | Hồ sơ đầy đủ, khác với danh bạ |
| `GiangVien` | **Cơ sở** | Phân mảnh ngang | Cục bộ | |
| `TaiKhoan` | **Cơ sở** | Phân mảnh ngang | Cục bộ | Xác thực tại cơ sở nhà |
| `DotDangKy` | **Cơ sở** | Phân mảnh ngang | Cục bộ | Không nhân bản |
| `LopHocPhan` | **Cơ sở mở lớp (Host)** | Phân mảnh ngang | Cục bộ + cross-site khi duyệt catalog | |
| `DangKyHocPhan` | **Host** | Dẫn xuất bậc 1 | Cục bộ | Đặt cạnh bộ đếm sức chứa |
| `Diem` | **Host** (GV dạy lớp) | Dẫn xuất bậc 2 | Cục bộ | |
| `YeuCauHocLienCoSo` | **Cơ sở nhà** | Phân mảnh ngang | Cục bộ | |
| `BangDiemMirror` | **Cơ sở nhà** *(ghi bởi worker)* | **Projection** | Cục bộ | Nguồn sự thật ở Host |
| `OutboxSuKien` | **Cơ sở phát sinh sự kiện** | Phân mảnh ngang | Cục bộ | |

### Vì sao `DangKyHocPhan` thuộc Host chứ không thuộc Home

Ràng buộc cần bảo vệ là **sức chứa của lớp**, và bộ đếm nằm ở Host. Nếu đăng ký nằm ở Home thì mỗi lần ghi phải cập nhật bộ đếm ở site khác → **bắt buộc phải dùng distributed transaction**.

Đặt đăng ký cạnh bộ đếm thì **toàn bộ nghiệp vụ đăng ký, kể cả liên cơ sở, chỉ còn là một giao dịch cục bộ tại Host.** Đây là quyết định đắt giá nhất của cả thiết kế.

### Vòng đời sinh viên — tạo mới và chuyển cơ sở

Một sinh viên tồn tại ở **hai nơi**: dòng danh bạ tại `PTITONE_MASTER` và hồ sơ đầy đủ + tài khoản tại cơ sở nhà. Hai chỗ này phải được tạo có thứ tự và chịu được lỗi giữa chừng.

**Tạo mới — dùng lại đúng cơ chế Outbox của C10, không thêm cơ chế mới:**

```
┌ Giao dịch cục bộ tại PTITONE_MASTER ──────────────┐
│  INSERT DanhBaNguoiDung (MaSV, MaCoSoNha,      │
│                         TrangThai='CHO_KICH_HOAT')
│  INSERT OutboxSuKien   ('SinhVienDuocTao')    │
└──────────────────── COMMIT ───────────────────┘
                       │
              OutboxWorker (mỗi 10 giây)
                       │
   1. tạo SinhVien + TaiKhoan tại CƠ SỞ NHÀ   ← làm TRƯỚC, idempotent theo MaSV
   2. cập nhật TrangThai='HOAT_DONG' tại Master ← làm SAU
```

Bất biến: **sinh viên chỉ đăng nhập được khi `TrangThai = 'HOAT_DONG'`.** Nhờ vậy, nếu bước 1 chưa chạy xong hoặc thất bại, không có trạng thái mồ côi nào lọt ra ngoài — dòng danh bạ tồn tại nhưng chưa dùng được, và worker sẽ thử lại.

**Chuyển cơ sở — và đây là chỗ thiết kế phân mảnh trả cổ tức lần nữa:**

Vì `DangKyHocPhan` và `Diem` phân mảnh theo **Host** chứ không theo sinh viên, **dữ liệu gốc về đăng ký và điểm không phải di chuyển** — chúng vốn đã nằm đúng chỗ theo lớp đã học, và về mặt nghiệp vụ chúng *phải* ở lại đó (đó là lịch sử học tập tại cơ sở cũ).

**Nhưng vẫn phải di trú toàn bộ trạng thái do Home sở hữu.** Và vì **không tồn tại trạng thái trung gian an toàn** — sinh viên không thể "nửa ở HCM nửa ở HN" — nghiệp vụ này là nơi **bắt buộc dùng distributed transaction** (yêu cầu 3 của giảng viên).

Chia làm ba giai đoạn, và **chỉ giai đoạn 2 nằm trong giao dịch phân tán**:

| Giai đoạn | Việc | Trong 2PC? |
|---|---|---|
| **1. Tiền điều kiện** | Không còn `YeuCauHocLienCoSo` ở trạng thái `CHO_DUYET` · `OutboxSuKien` của SV đã xả hết · không đang trong đợt đăng ký | ❌ kiểm tra trước |
| **2. Giao dịch phân tán** | Xoá `SinhVien` + `TaiKhoan` ở cơ sở cũ · chèn ở cơ sở mới · cập nhật `DanhBaNguoiDung` tại Master | ✅ **nguyên tử trên 3 CSDL** |
| **3. Hậu xử lý** | Dựng lại `BangDiemMirror` từ `Diem` ở các Host · tính lại `SoMonLienCoSo` · chuyển `YeuCauHocLienCoSo` lịch sử | ❌ idempotent, chạy sau |

> ⚠️ **Giữ giao dịch phân tán càng ngắn càng tốt** — đúng 5 câu lệnh. Nhét cả bước dựng lại projection vào trong đó là giữ lock trên ba site lâu không cần thiết.

Chi tiết thủ tục, mã T-SQL và cấu hình MS DTC: **mục D8**.

### ⚠️ Ba vấn đề định tuyến SAU khi chuyển cơ sở

Giao dịch phân tán chỉ lo phần dữ liệu. Ba chỗ dưới đây nằm ngoài nó và phải xử lý riêng:

| # | Vấn đề | Cách xử lý |
|---|---|---|
| 1 | **JWT cũ vẫn mang `homeCampus` cũ** — sinh viên đang đăng nhập sẽ tiếp tục bị định tuyến về site cũ, nơi tài khoản đã bị xoá | Thêm cột `PhienBanTaiKhoan` vào danh bạ; JWT mang theo giá trị này và **mỗi request đối chiếu**. Thủ tục chuyển cơ sở tăng số phiên bản → mọi JWT cũ lập tức vô hiệu, buộc đăng nhập lại. Rẻ hơn nhiều so với danh sách thu hồi token |
| 2 | **Replica danh bạ ở site khác chưa kịp cập nhật** — đăng nhập có thể vẫn được chỉ về site cũ trong vài giây | Quy tắc dự phòng khi đăng nhập: *nếu danh bạ chỉ tới site X mà tài khoản **không tồn tại** ở X → tra thẳng `PTITONE_MASTER` rồi định tuyến lại.* Cùng cơ chế đã dùng cho trường hợp danh bạ chưa nhân bản kịp |
| 3 | **`DangKyHocPhan.MaCoSoNhaSV` ở Host vẫn ghi cơ sở cũ** | ✅ **Đây là đúng, không phải lỗi.** Cột này là **bản chụp tại thời điểm đăng ký**, không phải con trỏ sống. Sinh viên *đã* thuộc cơ sở cũ khi học môn đó — giữ nguyên mới đúng lịch sử. Chỉ các đăng ký **mới** dùng cơ sở mới. Phải ghi rõ ngữ nghĩa này vào từ điển dữ liệu, nếu không người sau sẽ tưởng là dữ liệu rác |

**Ràng buộc vận hành:** chỉ cho chuyển cơ sở **ngoài đợt đăng ký** — thêm vào tiền điều kiện của thủ tục. Chuyển ngay giữa đợt đăng ký làm cả ba vấn đề trên xảy ra cùng lúc với hàng nghìn request đang chạy.

> Nếu trước đó chọn phân mảnh `DangKyHocPhan` theo `SinhVien`, chuyển cơ sở sẽ phải **di trú cả lịch sử đăng ký và điểm** — khối lượng lớn nhất trong hệ thống, và còn phá vỡ ngữ nghĩa "điểm thuộc về nơi dạy". Với vị từ theo `LopHocPhan`, phạm vi giao dịch phân tán **thu về đúng ba bảng nhỏ**. Đây là điều làm cho 2PC ở đây trở nên khả thi và rẻ — đáng nêu trong báo cáo như hệ quả tích cực của quyết định ở C3.

## C3. **Lược đồ phân mảnh**

### Phân mảnh ngang (Horizontal Fragmentation)

Với mỗi cơ sở `c ∈ CoSo`:

```
SinhVien_c    = σ (MaCoSoNha  = c) (SinhVien)
GiangVien_c   = σ (MaCoSo     = c) (GiangVien)
TaiKhoan_c    = σ (MaCoSo     = c) (TaiKhoan)
DotDangKy_c   = σ (MaCoSo     = c) (DotDangKy)
LopHocPhan_c  = σ (MaCoSoHost = c) (LopHocPhan)
YeuCau_c      = σ (SV.MaCoSoNha = c) (YeuCauHocLienCoSo)
```

Tính đúng đắn: **đầy đủ** (mọi dòng có đúng một `MaCoSo`), **tái tạo được** (`R = ⋃ R_c`), **rời nhau** (`R_i ∩ R_j = ∅` với `i ≠ j`).

### Phân mảnh ngang dẫn xuất (Derived Horizontal Fragmentation)

```
Bậc 1:  DangKyHocPhan_c = DangKyHocPhan ⋉ LopHocPhan_c
Bậc 2:  Diem_c          = Diem ⋉ DangKyHocPhan_c
                        = Diem ⋉ (DangKyHocPhan ⋉ LopHocPhan_c)
```

> ⚠️ **Điểm phải nói rõ trong báo cáo — và là câu trả lời hạng A cho "tại sao phân mảnh như vậy?":**
>
> Có hai lựa chọn vị từ dẫn xuất cho `DangKyHocPhan`: theo `SinhVien` (tối ưu cho việc đọc "lịch học của tôi") hoặc theo `LopHocPhan` (tối ưu cho việc ghi có ràng buộc sức chứa). Chúng tôi chọn **theo `LopHocPhan`** vì ràng buộc `SoLuongDaDangKy ≤ SoLuongToiDa` phải được bảo vệ nguyên tử, và đặt đăng ký cùng site với bộ đếm cho phép thực hiện bằng một giao dịch cục bộ. Cái giá phải trả là màn hình "lịch học của tôi" của sinh viên có học liên cơ sở cần dữ liệu từ site khác — chi phí này được khử bằng **read model cục bộ** (C10), và bảng tần suất B2 cho thấy chỉ ~2% sinh viên thuộc diện này.

### Nhân bản (Replication)

```
Nhân bản một chiều, Master → mọi Subscriber:
   CoSo · Khoa · ChuongTrinhDaoTao · MonHoc · MonHocTienQuyet
   HocKy · DanhBaNguoiDung
```

Nhân bản phục vụ **hai mục đích khác nhau** — nên nêu tách bạch trong báo cáo:

1. **Dữ liệu dùng chung** (`MonHoc`, `Khoa`…) → loại bỏ truy vấn chéo site cho loại truy cập nhiều nhất hệ thống (80.000 lượt/ngày).
2. **Danh bạ định vị** (`DanhBaNguoiDung`, `CoSo`) → **chính là thứ làm cho Location Transparency khả thi**. Không có danh bạ, mỗi lần đăng nhập phải dò lần lượt từng site.

### Luồng đăng nhập — bài toán con gà và quả trứng

Muốn biết định tuyến vào CSDL nào thì phải biết người dùng thuộc cơ sở nào; muốn biết điều đó thì phải đọc CSDL. Danh bạ nhân bản chính là lời giải:

```
1. Đọc DanhBaNguoiDung  ──►  từ replica của SITE NÀO CŨNG ĐƯỢC
                             (chúng giống hệt nhau — thực tế đọc từ
                              datasource mặc định của máy chạy API)
      → biết MaCoSo và LoaiNguoiDung

2. Xác thực tại đúng site đó
      SINH_VIEN / GIANG_VIEN / ADMIN_CO_SO  →  TaiKhoan tại site
      ADMIN_MASTER                          →  TaiKhoanMaster tại PTITONE_MASTER

3. Phát JWT mang claim đã ký:
      { "sub": "N22DCCN001", "role": "SINH_VIEN", "homeCampus": "HN" }

4. Mọi request sau đọc claim từ JWT — KHÔNG tra danh bạ lại

5. Chưa thấy trong replica (độ trễ nhân bản) → tra thẳng PTITONE_MASTER
```

> ⚠️ **Bước 4 là chỗ dễ tạo lỗ hổng bảo mật.** Cơ sở phải lấy từ **claim trong JWT đã ký**, tuyệt đối không lấy từ tham số client gửi lên. Nếu tin `?campus=HN` do client truyền thì bất kỳ ai cũng đọc được dữ liệu của cơ sở khác.

> ⭐ **Danh bạ phủ MỌI vai trò, không riêng sinh viên.** Nếu chỉ có danh bạ sinh viên thì giảng viên và Admin buộc phải **tự chọn cơ sở lúc đăng nhập** — tức là bắt người dùng cung cấp thông tin định vị, làm hỏng chính tuyên bố Location Transparency ở D7. Một bảng danh bạ chung giải quyết trọn vẹn, và cho Admin Master một chỗ ở hợp lệ.

## C4. **Lược đồ ánh xạ**

| Quan hệ toàn cục | Mảnh | Vị trí | Vị từ / phép toán | Tái tạo |
|---|---|---|---|---|
| `SinhVien` | `SinhVien_HCM`, `SinhVien_HN`, `SinhVien_DN` | mỗi site | `σ(MaCoSoNha = c)` | `⋃` |
| `GiangVien` | `GiangVien_c` | mỗi site | `σ(MaCoSo = c)` | `⋃` |
| `TaiKhoan` | `TaiKhoan_c` | mỗi site | `σ(MaCoSo = c)` | `⋃` |
| `DotDangKy` | `DotDangKy_c` | mỗi site | `σ(MaCoSo = c)` | `⋃` |
| `LopHocPhan` | `LopHocPhan_c` | mỗi site | `σ(MaCoSoHost = c)` | `⋃` |
| `DangKyHocPhan` | `DangKyHocPhan_c` | mỗi site | `⋉ LopHocPhan_c` | `⋃` |
| `Diem` | `Diem_c` | mỗi site | `⋉ DangKyHocPhan_c` | `⋃` |
| `YeuCauHocLienCoSo` | `YeuCau_c` | mỗi site | `σ(cơ sở nhà = c)` | `⋃` |
| `MonHoc` | bản sao đầy đủ | **mọi site** | nhân bản toàn phần | bản sao giống hệt |
| `Khoa`,`CTDT`,`HocKy`,`CoSo` | bản sao đầy đủ | **mọi site** | nhân bản toàn phần | bản sao giống hệt |
| `DanhBaNguoiDung` | bản sao đầy đủ | **mọi site** | nhân bản toàn phần | bản sao giống hệt |
| `BangDiemMirror` | — | cơ sở nhà | **projection**, không tham gia tái tạo | — |

## C5. **Sơ đồ định vị và đồng bộ hóa**

> ⚠️ **Điểm bảo vệ điểm số:** phải phân biệt ba nhóm bằng ký hiệu khác nhau. Nếu vẽ `BangDiemMirror` giống hệt một mảnh, giám khảo sẽ kết luận nhóm đã nhân bản lung tung và **phá vỡ phân mảnh**.

```
Ký hiệu:
   ★  BẢN GỐC dữ liệu tham chiếu — nơi ghi duy nhất
   ■  MẢNH (fragment)            — dữ liệu gốc, nguồn sự thật tại đây
   ▨  BẢN NHÂN BẢN (replica)     — chỉ đọc, DENY ghi
   □  READ MODEL (projection)    — tầng ứng dụng, KHÔNG phải mảnh,
                                   nguồn sự thật ở site khác

              ┌──────────────────────────────────────────┐
              │  PTITONE_MASTER — VAI TRÒ MASTER             │
              │  PUBLISHER · DISTRIBUTOR                 │
              │  (database TÁCH BIỆT, đặt trên SRV-HCM)  │
              │                                          │
              │  ★ CoSo · Khoa · ChuongTrinhDaoTao       │
              │  ★ MonHoc · MonHocTienQuyet              │
              │  ★ HocKy · DanhBaNguoiDung                │
              │                                          │
              │  Người ghi duy nhất: Admin Master        │
              └────┬──────────────┬──────────────┬───────┘
                   │              │              │
          Transactional Replication một chiều (~vài giây)
                   │              │              │
                   ▼              ▼              ▼
      ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
      │    PTITONE_HCM     │ │    PTITONE_HN      │ │    PTITONE_DN      │
      │  SUBSCRIBER    │ │  SUBSCRIBER    │ │  SUBSCRIBER    │
      │   (SRV-HCM)    │ │   (SRV-HN)     │ │   (SRV-DN)     │
      ├────────────────┤ ├────────────────┤ ├────────────────┤
      │ ■ SinhVien     │ │ ■ SinhVien     │ │ ■ SinhVien     │
      │ ■ GiangVien    │ │ ■ GiangVien    │ │ ■ GiangVien    │
      │ ■ TaiKhoan     │ │ ■ TaiKhoan     │ │ ■ TaiKhoan     │
      │ ■ DotDangKy    │ │ ■ DotDangKy    │ │ ■ DotDangKy    │
      │ ■ LopHocPhan   │ │ ■ LopHocPhan   │ │ ■ LopHocPhan   │
      │ ■ DangKyHocPhan│ │ ■ DangKyHocPhan│ │ ■ DangKyHocPhan│
      │ ■ Diem         │ │ ■ Diem         │ │ ■ Diem         │
      │ ■ YeuCauLienCS │ │ ■ YeuCauLienCS │ │ ■ YeuCauLienCS │
      │ ■ OutboxSuKien │ │ ■ OutboxSuKien │ │ ■ OutboxSuKien │
      │ □ BangDiemMirr │ │ □ BangDiemMirr │ │ □ BangDiemMirr │
      │                │ │                │ │                │
      │ ▨ CoSo Khoa    │ │ ▨ CoSo Khoa    │ │ ▨ CoSo Khoa    │
      │   CTDT MonHoc  │ │   CTDT MonHoc  │ │   CTDT MonHoc  │
      │   HocKy DanhBa │ │   HocKy DanhBa │ │   HocKy DanhBa │
      │  (DENY ghi)    │ │  (DENY ghi)    │ │  (DENY ghi)    │
      └────────────────┘ └────────────────┘ └────────────────┘
            ▲                    ▲                   ▲
            └── Linked Server hình sao (N−1 định nghĩa) ──┘

  GLOBAL REPORTING NODE — phụ thuộc D15:
     • 3 máy (Master colocate) → PTITONE_HCM giữ Linked Server tới SRV-HN, SRV-DN
                                  ⚠️ HCM vẫn đặc biệt về mặt VẬT LÝ
     • 4 máy (Master riêng)    → SRV-MASTER giữ Linked Server tới CẢ BA site
                                  ✅ ba site vận hành đối xứng hoàn toàn

  Linked Server có ĐÚNG HAI công dụng:
    (1) Báo cáo tổng hợp — CHỈ ĐỌC, login uis_report
    (2) Giao dịch phân tán chuyển cơ sở (D8) — CÓ GHI,
        chỉ trên 2 bảng, chỉ bởi login uis_chuyencoso
  KHÔNG bao giờ phục vụ đăng nhập, đăng ký hay xem điểm.
```

> Sơ đồ trên vẽ theo **phương án 3 máy**. Với 4 máy, mũi tên Linked Server chuyển từ `PTITONE_HCM` sang `SRV-MASTER` — **không có gì khác thay đổi**.

> ⭐ **Ba cơ sở vận hành có hình dạng giống hệt nhau.** Đó là điều cần thấy được ngay khi nhìn sơ đồ này: `PTITONE_HCM` không đặc biệt hơn `PTITONE_HN` hay `PTITONE_DN` ở bất kỳ điểm nào. Thứ đặc biệt là `PTITONE_MASTER` — một **vai trò**, không phải một cơ sở.

**Năm loại kết nối trong toàn hệ thống — và chỉ năm:**

| # | Kết nối | Giao thức · cổng | Dùng cho | Tần suất |
|---|---|---|---|---|
| 1 | Người dùng → API | **HTTPS 443** (qua tunnel hoặc LAN/VPN) | Mọi nghiệp vụ của SV/GV/Admin | Toàn bộ lưu lượng người dùng |
| 2 | API → CSDL | **JDBC/TDS 1433** qua VPN | Đọc/ghi nghiệp vụ, saga liên cơ sở, duyệt catalog site khác, Outbox worker | ~500 lượt/ngày cross-site |
| 3 | Master → Subscriber | **Replication** qua VPN (1433) | Đồng bộ danh mục + danh bạ | Liên tục, ~15 sự kiện/ngày |
| 4 | Reporting Node → site | **Linked Server** qua VPN (1433) | **Chỉ** thống kê toàn hệ thống | ~40 lượt/ngày |
| 5 | Site ↔ Site | ⭐ **MS DTC — TCP 135 + RPC động 49152–65535** | **Giao dịch phân tán** (chuyển cơ sở, D8) | Vài lần mỗi kỳ |

> ⚠️ Loại 5 **không đi qua cổng 1433**. Đây là hạ tầng riêng, cấu hình riêng (F4b), và là chỗ dễ quên nhất khi mở firewall.
> ⚠️ **Thiết bị người dùng KHÔNG tham gia VPN.** Chỉ các máy chủ nối với nhau bằng VPN. Người dùng chỉ thấy đúng một URL HTTPS.

Mọi thứ còn lại là cục bộ. Đó là toàn bộ luận điểm của kiến trúc này, và nó đủ ngắn để trình bày trong 30 giây lúc bảo vệ.

## C6. **Thiết kế kiến trúc hệ thống — Client/Server**

> Đề bài yêu cầu tuyên bố: **ngang hàng hay Client/Server**.

**Chọn Client/Server.** Ba lý do:

1. **Có phân vai bất đối xứng rõ ràng** đối với dữ liệu tham chiếu: `PTITONE_MASTER` là Publisher, mọi CSDL vận hành là Subscriber chỉ đọc. Mô hình ngang hàng đòi hỏi mọi nút có vai trò tương đương và cùng quyền ghi lên cùng tập dữ liệu. *(Lưu ý: với dữ liệu vận hành thì ba cơ sở lại hoàn toàn đối xứng — xem C0 về hai chế độ ghi.)*
2. **Máy trạm không kết nối trực tiếp tới CSDL.** Toàn bộ truy cập đi qua một tầng máy chủ ứng dụng, nơi thực hiện xác thực, phân quyền và định tuyến.
3. **Dữ liệu dùng chung chỉ có một nơi ghi.** Đây là quan hệ chủ–tớ, không phải quan hệ đồng cấp. Chọn ngang hàng sẽ kéo theo nhân bản hai chiều và bài toán giải quyết xung đột mà nghiệp vụ vốn không cần.

⚠️ **Hạn chế đã nhận diện:** một tầng ứng dụng tập trung là điểm chết đơn lẻ. Hướng mở rộng: mỗi cơ sở chạy một instance máy chủ ứng dụng, cùng trỏ vào CSDL cục bộ của mình và gọi chéo khi cần.

## C7. **Mô hình front-end / back-end**

### Tại từng chi nhánh

```
Máy trạm SV/GV/Admin cơ sở
        │ HTTPS
Máy chủ ứng dụng ── (định tuyến local-first) ──► CSDL cơ sở đó
```

### Toàn hệ thống

```
                    ┌──────── apps/web (React) ────────┐
                    │  SV · GV · Admin · X-Ray panel ⭐ │
                    └──────────────┬───────────────────┘
                                   │ REST + JWT
                    ┌──────────────▼───────────────────┐
                    │        apps/api (Spring Boot)     │
                    │  interfaces → application → infra │
                    └──┬────────┬────────┬──────────┬──┘
                       │        │        │          │
                   DS_HCM    DS_HN    DS_DN    DS_MASTER
                                               (chỉ dùng cho
                                                quản trị danh mục)
```

> `DS_MASTER` **không** tham gia vào bất kỳ đường đọc nào của sinh viên/giảng viên. Mọi thao tác đọc danh mục đều đi vào bản sao trong CSDL vận hành của site đó — đó chính là Replication Transparency.

## C7b. Truy cập mạng — CSDL kín, API mở

Một hệ thống UIS thật thì sinh viên gọi API qua HTTPS từ bất kỳ đâu. Nhưng **CSDL của nó không bao giờ mở ra internet** — nó nằm trong mạng nội bộ của trường. Kiến trúc thật là:

```
Sinh viên (bất kỳ đâu, bất kỳ máy nào)
      │ HTTPS công khai
      ▼
┌──────────────────┐
│  Tầng API / Web  │   ← CÔNG KHAI
└────────┬─────────┘
         │ mạng nội bộ   ← KHÔNG BAO GIỜ công khai
         ▼
   CSDL các cơ sở
```

> **VPN trong dự án này chính là "mạng nội bộ" đó.** Nên phần VPN không phải chỗ thiếu sót — nó đang mô phỏng đúng. Thứ cần thêm chỉ là **lối vào công khai cho tầng API**.

### ⭐ Phiên bản đồ án: MỘT API trung tâm

| | Phiên bản đồ án (chốt) | Hướng mở rộng — **P1, sau đồ án** |
|---|---|---|
| Tầng ứng dụng | **Một** Spring Boot trên SRV-HCM, React đã build nhúng vào, **một URL duy nhất** | Mỗi cơ sở một instance: `API-HCM → DB-HCM`, `API-HN → DB-HN`… |
| Kết nối CSDL | 4 pool: `DS_MASTER`, `DS_HCM`, `DS_HN`, `DS_DN` | Mỗi instance chủ yếu dùng pool cục bộ của mình |
| Request cục bộ của SV Hà Nội | Đi vòng: máy HN → API ở HCM → CSDL HN (**hai chặng WAN**) | Đi thẳng trong cơ sở |
| Outbox worker | Một worker xử lý outbox của mọi site | Mỗi instance xử lý outbox của site mình — sạch hơn |
| Điểm chết đơn lẻ | **Có** — máy API tắt là toàn bộ website ngừng | Không còn |

> Local-first hiện đúng ở **tầng CSDL**, chưa đúng ở tầng ứng dụng. Đây là hạn chế đã nhận diện, không phải chỗ bỏ sót — và cách sửa chỉ là chạy thêm instance với cấu hình khác, không đổi một dòng thiết kế nào.

### Ba mức truy cập, tuỳ tình huống

| Tình huống | Cách | Địa chỉ |
|---|---|---|
| Demo trong phòng, các máy cùng wifi | Máy chạy API mở firewall cổng 8080 | `http://192.168.x.x:8080` |
| Nhóm làm việc ở nhà, khác địa điểm | Mọi người vào cùng mạng VPN | `http://26.x.x.x:8080` |
| **Truy cập từ bất kỳ đâu qua internet** | **Tunnel miễn phí trên máy chạy API** | `https://<tên>.trycloudflare.com` |

```bash
cloudflared tunnel --url http://localhost:8080
```

Một lệnh, có ngay URL HTTPS công khai. Không cần IP tĩnh, không mở port trên router, không tốn tiền. Máy chạy API vẫn ở trong VPN để với tới các CSDL — **kết quả là CSDL vẫn kín, chỉ API công khai**, đúng hình dạng hệ thống thật.

*(Thay thế: Tailscale Funnel nếu dùng Tailscale làm VPN; ngrok bản miễn phí có trang cảnh báo chen giữa.)*

> ⚠️ **Tunnel chỉ dùng cho demo, không phải cổng vào production.** URL ngẫu nhiên, đổi mỗi lần khởi động lại, không có WAF/rate-limit/giám sát. Bật khi cần trình bày, tắt ngay sau đó.

> ⚠️ **Thiết bị người dùng không tham gia VPN.** Chỉ các máy chủ nối VPN với nhau. Điện thoại 4G của sinh viên chỉ thấy đúng một URL HTTPS — không thấy CSDL, không thấy cổng 1433, không thấy tên máy nội bộ.

### Ba cái bẫy khi chạy nhiều máy

| # | Bẫy | Cách xử lý |
|---|---|---|
| 1 | **Vite chỉ nghe `127.0.0.1`** → máy khác không vào được frontend | `server: { host: true }` và proxy trỏ về **IP máy chạy API**, không phải `localhost` |
| 2 | **Windows Firewall chặn cổng 8080** — chỉ xảy ra khi truy cập qua **LAN hoặc VPN** | `New-NetFirewallRule -DisplayName "UIS API 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow`<br>⚠️ **Dùng Cloudflare Tunnel trên cùng máy thì KHÔNG cần lệnh này** — `cloudflared` gọi `localhost`, không đi qua firewall |
| 3 | **CORS** khi trình duyệt gọi thẳng API khác origin | Không phát sinh nếu gộp frontend vào backend, hoặc đi qua proxy Vite |

> ⭐ **Khuyến nghị cho demo: gộp frontend vào backend.** `npm run build` rồi chép `dist/*` vào `apps/api/src/main/resources/static/`. Một server, một cổng, một URL — không CORS, không proxy, không phải chạy hai tiến trình. Lúc bảo vệ là phương án ít thứ hỏng nhất.

### Nếu mở ra internet thì phải làm ba việc

1. **Mật khẩu CSDL mạnh, `.env` không bao giờ commit** — repo đang công khai
2. **Không để lộ endpoint quản trị** — chặn Swagger UI và Spring Actuator ra ngoài
3. **Chỉ bật tunnel khi cần demo**, tắt lúc không dùng

⚠️ Đây vẫn là phần ➕ — không ảnh hưởng tới bất kỳ mục nào trong năm yêu cầu bắt buộc.

---

## C8. Abstraction boundary — **3 port, không hơn** ➕

Kiến trúc theo **Ports & Adapters (Hexagonal)**. Ba interface là **port** ở tầng `application`; bản cài đặt dùng SQL Server là **adapter** ở tầng `infrastructure`.

```java
// application/port/
interface CrossSiteQuery {           // đọc chéo site theo từng người dùng
    <T> List<T> fanOut(Set<String> sites, SiteReader<T> reader);
}

interface GlobalReport {             // tổng hợp toàn hệ thống
    List<ThongKeRow> thongKeTheoMonHoc(String maMonHoc, String maHocKy);
    // ... 3 báo cáo còn lại
}

interface CatalogHealth {            // tình trạng đồng bộ danh mục
    Instant lastSyncedAt(String maCoSo);
    boolean isStale(String maCoSo, Duration threshold);
}
```

| Port | Adapter cho môn học | Adapter cho tương lai |
|---|---|---|
| `CrossSiteQuery` | Fan-out qua nhiều DataSource | Giữ nguyên — vốn không phụ thuộc SQL Server |
| `GlobalReport` | **`LinkedServerReport`** (`OPENQUERY`) | `BackendMergeReport` |
| `CatalogHealth` | Đọc tracer token / distribution database | Đọc từ cơ chế đồng bộ khác |

⚠️ **Giới hạn cứng: đúng 3 port.** Không tạo `DatabaseProvider`, `RepositoryFactory`, `ReplicationManager`, `SyncEngine`. Khi ai đó nói "tạo abstraction boundary", rủi ro đi kèm luôn là abstract quá tay.

**Phân biệt quan trọng:** `SiteContext`, `RoutingDataSource`, `SiteRegistry`, `OutboxWorker` là **class cụ thể**, không phải interface, và **không tính vào giới hạn 3**. Chúng là cơ chế, không phải điểm thay thế. Riêng `SiteContext` chính là hiện thân của Location Transparency nên bắt buộc phải tồn tại.

### Các design pattern thực sự được dùng

| Pattern | Dùng ở đâu | Giải quyết vấn đề gì |
|---|---|---|
| **Ports & Adapters** | 3 port ở trên | Cô lập công nghệ bắt buộc của môn học |
| **Saga (điều phối)** | Đăng ký liên cơ sở (D3) | Nhất quán qua nhiều site mà không cần 2PC |
| **Transactional Outbox** | Đồng bộ điểm (C10) | Chống mất sự kiện khi phải ghi hai nơi |
| **Idempotent Receiver** | `MaYeuCau` + upsert kiểm `Version` | Retry an toàn, chống ghi đè bằng dữ liệu cũ |
| **Read Model / Projection** | `BangDiemMirror`, snapshot lớp | Tách đường đọc khỏi nguồn sự thật để tăng tính sẵn sàng |
| **Routing DataSource** | `SiteContext` + `SiteRegistry` | Location Transparency |
| **Graceful Degradation** | Báo cáo trả kết quả một phần | Một site chết không kéo sập phần còn lại |
| **Repository** | Truy cập dữ liệu theo từng aggregate | Gom SQL về một chỗ, dễ trỏ vào lúc bảo vệ |

Nguyên tắc: **chỉ gọi tên pattern mà hệ thống thật sự dùng.** Thêm pattern để cho oai chính là chỗ "professional" biến thành "over-engineering".

## C9. Chiến lược ID chống trùng (D8)

> **Nguyên tắc: không dùng `IDENTITY` ở bất kỳ đâu.** Chiến lược khóa chọn **theo từng aggregate**, tùy vào ai sở hữu dữ liệu đó — **không** áp tiền tố cơ sở lên mọi bảng.

| Aggregate | Ai sở hữu | Chiến lược khóa | Lý do |
|---|---|---|---|
| `MaSinhVien`, `MaGiangVien` | Master / cơ sở | Mã nghiệp vụ toàn trường, **không có tiền tố cơ sở** | ⚠️ Bắt buộc — mã SV **không được** mã hóa cơ sở, nếu không sẽ mâu thuẫn với D7 và sinh viên chuyển cơ sở phải đổi mã |
| `MaMonHoc`, `MaKhoa`, `MaCTDT`, `MaHocKy` | `PTITONE_MASTER` | Mã nghiệp vụ toàn cục | Dữ liệu tham chiếu chỉ có một nơi cấp phát → không thể trùng |
| `MaLopHP` | **Host** | `<MaMonHoc>-<MaHocKy>-<MaCoSo><STT>` | Lớp thuộc về một cơ sở cụ thể → nhúng mã cơ sở là **đúng ngữ nghĩa**, không phải mẹo chống trùng |
| `MaDot` | **Cơ sở** | `<MaCoSo>-<MaHocKy>-<STT>` | Tương tự |
| `DangKyHocPhan`, `Diem` | **Host** | **Khóa kép** (`MaLopHP`, `MaSinhVien`) — không có khóa thay thế | `MaLopHP` đã định vị được cơ sở → khóa kép duy nhất toàn cục |
| `MaYeuCau`, `EventId` | Site phát sinh | `uniqueidentifier` sinh tại tầng ứng dụng, **không phải khóa chính cụm** | Là **idempotency key**, cần duy nhất mà không cần phối hợp |

> Quy tắc chung: **nhúng mã cơ sở vào khóa chỉ khi cơ sở là một phần ngữ nghĩa của thực thể đó.** Lớp học phần thuộc về một cơ sở — đúng. Sinh viên thì không, vì sinh viên có thể chuyển cơ sở.

### ⚠️ Vì sao bỏ `IDENTITY(k, N)` — bản trước thiết kế sai chỗ này

Phương án cũ đặt seed/increment lệch nhau: HCM `(1,3)` · HN `(2,3)` · ĐN `(3,3)`. **Nó không mở rộng an toàn.** Với increment = 3, ba lớp thặng dư modulo 3 đã bị chiếm hết. Thêm cơ sở thứ tư thì **không còn seed nào không đụng**, và muốn đổi increment thì phải đổi trên mọi site — trong khi ID cũ đã phát ra rồi.

Rà lại thì phát hiện: **thiết kế hiện tại vốn không cần `IDENTITY` ở đâu cả.** `DangKyHocPhan` và `Diem` đã là khóa kép, `MaLopHP` và `MaDot` đã nhúng mã cơ sở, `MaYeuCau`/`EventId` đã là UUID. Đây là sửa bằng cách **bớt**, không phải thêm.

> ⭐ **Hệ quả: thêm một cơ sở là phép cộng ở MỨC LOGIC.** Không khóa nào phải đổi, không dòng dữ liệu nào phải di trú, không phải phân hoạch lại mảnh nào. Lược đồ chỉ *mọc thêm* một mảnh.
>
> ⚠️ **Nhưng ở mức vận hành thì vẫn là công việc thật**, đừng tuyên bố zero-coordination: cài máy + SQL Server, thêm nút VPN và mở firewall, tạo subscription mới (kèm sinh snapshot — tác động tới Publisher), thêm Linked Server nếu là reporting node, cập nhật cấu hình DataSource và triển khai lại ứng dụng, nạp dữ liệu nền, và cần một cửa sổ bảo trì. Cái rẻ là **lược đồ**, không phải **buổi triển khai**.

⚠️ **Không dùng `uniqueidentifier` làm khóa chính cụm** cho bảng dữ liệu — gây phân mảnh chỉ mục.

## C10. Read model và Outbox ➕

### Nguyên tắc phân vai

```
            AVAILABILITY                 FRESHNESS
                  │                          │
                  ▼                          ▼
          Local read model             Fan-out có điều kiện
     (luôn đọc được, có thể cũ)     (dữ liệu tươi, cần site kia sống)
                  │                          │
                  └────────────┬─────────────┘
                               ▼
                        Tầng nghiệp vụ
```

| Thao tác | Nguồn dữ liệu |
|---|---|
| "Xem lịch học của tôi" | **Read model cục bộ** — thuần local, luôn chạy được |
| "Xem bảng điểm / GPA" | **`BangDiemMirror` cục bộ** — thuần local |
| "Kiểm tra lịch/phòng hiện tại của lớp ở site kia" | **Fan-out** — cần dữ liệu tươi |
| "Duyệt danh mục lớp đang mở ở site kia" | **Fan-out** — sĩ số thay đổi liên tục, không mirror được |
| "Đăng ký lớp ở site kia" | **Host là authoritative** — luôn ghi tại Host |

> **Site chứa dữ liệu gốc luôn là nguồn sự thật. Read model chỉ phục vụ đọc.** Nếu lớp ở HN đổi phòng hay hủy, mirror tại HCM có thể cũ — đó là **độ trễ nhân bản**, không phải dữ liệu sai. Vì vậy mọi bảng mirror đều mang `Version` + `LastSyncedAt` + `SyncStatus`.

⚠️ **Giới hạn phạm vi mirror:** chỉ mirror **dữ liệu của sinh viên thuộc về mình** (cơ sở nhà chịu trách nhiệm về hồ sơ, bảng điểm, xét tốt nghiệp của em đó). **Không** mirror dữ liệu vận hành của site khác (danh sách lớp, sĩ số, sinh viên của họ). Mirror hết là hết phân mảnh.

### Vì sao không dùng publication chiều ngược

Nếu nhân bản `DangKyHocPhan` từ HN về HCM bằng SQL Server Replication thì **HN cũng phải trở thành Publisher** → N publication, cấu hình distributor phức tạp hơn, gấp N lần thứ có thể hỏng ở tuần 3. Trong khi saga **đã** mang kết quả từ HN về HCM rồi — chỉ cần ghi thêm mấy cột trong đúng giao dịch cục bộ đó.

**Không phát sinh thêm network round-trip so với saga hiện tại.** (Round-trip HCM→HN→HCM vẫn có, và nó là round-trip vốn đã thuộc về saga.)

### Transactional Outbox cho đồng bộ điểm

Đồng bộ điểm đi **ngược chiều** với saga: không ai đang chờ, và site Host phải chủ động đẩy về. Vì vậy **không thể** làm trong một giao dịch.

```
❌ SAI — dual write:
   BEGIN TRAN (HN) → UPDATE Diem → gọi sang HCM → COMMIT
   HCM chết giữa chừng thì mất nhất quán mà không ai biết

✅ ĐÚNG — Outbox:
   ┌ Giao dịch cục bộ tại HN ──────────┐
   │  UPDATE Diem                       │
   │  INSERT OutboxSuKien (DiemDaChot)  │
   └────────────── COMMIT ──────────────┘
                    │
          OutboxWorker (mỗi 10 giây)
                    │
   1. upsert BangDiemMirror tại HCM   ← LÀM TRƯỚC
   2. đánh dấu SENT tại HN            ← LÀM SAU
```

**Thứ tự hai bước là điều làm cho nó đúng:**

| Tình huống | Kết quả |
|---|---|
| Chết giữa bước 1 và 2 | Lần sau gửi lại → upsert idempotent → vô hại |
| Chết trước bước 1 | Sự kiện vẫn `PENDING` → gửi lại |
| Đảo thứ tự hai bước | ⚠️ **Mất sự kiện vĩnh viễn** |

At-least-once delivery + idempotent upsert = **effectively-once**, không cần giao dịch phân tán.

⚠️ **Không dùng `MERGE`.** `MERGE` của SQL Server có lịch sử lỗi dài và **không tự lấy khóa phù hợp**, nên dưới tương tranh vẫn có thể ném lỗi trùng khóa. Dùng mẫu `UPDATE` trước, `INSERT` sau:

```sql
BEGIN TRAN;

UPDATE BangDiemMirror
   SET DiemTongKet = @Diem, Version = @Version, LastSyncedAt = SYSUTCDATETIME()
 WHERE MaSinhVien = @MaSV AND MaLopHP = @MaLopHP
   AND Version < @Version;          -- chỉ ghi đè nếu sự kiện MỚI HƠN

IF @@ROWCOUNT = 0 AND NOT EXISTS (
        SELECT 1 FROM BangDiemMirror WITH (UPDLOCK, HOLDLOCK)
         WHERE MaSinhVien = @MaSV AND MaLopHP = @MaLopHP)
BEGIN
    INSERT INTO BangDiemMirror (...) VALUES (...);
END
-- @@ROWCOUNT = 0 mà bản ghi ĐÃ tồn tại  →  sự kiện cũ, bỏ qua (đúng ý)

COMMIT;
```

Thiếu điều kiện `Version < @Version` thì một sự kiện cũ bị retry muộn sẽ ghi đè điểm mới — bug rất khó tái hiện, nên chặn bằng lược đồ. `UPDLOCK, HOLDLOCK` chặn hai worker cùng chèn một dòng.

> ⭐ **Chỉ phát sự kiện Outbox cho *sinh viên khách*.** Sinh viên có cơ sở nhà trùng với site đang nhập điểm thì `Diem` đã nằm đúng chỗ — không cần mirror, không cần event. Với tỉ lệ liên cơ sở ~2%, điều này cắt ~98% lượng event.

**Với kiến trúc một máy chủ ứng dụng, worker không cần HTTP:** nó chỉ đọc `OutboxSuKien` qua `DS_HN` rồi ghi `BangDiemMirror` qua `DS_HCM`. Không message broker, không service thứ hai. Outbox tồn tại ở **mọi site** và worker xử lý đối xứng theo mọi chiều bằng cùng một đoạn code.

---

## C11. Nền tảng vận hành — làm gì, và dừng ở đâu ➕

Thiết kế hiện tại là **production-minded, chưa production-ready**. Đó là mức đúng cho một đồ án 8 tuần. Nhưng có vài thứ rẻ tới mức không làm thì phí, và vài thứ phải **nêu rồi loại** thay vì im lặng.

### Làm — rẻ và có giá trị ngay cho đồ án

| Hạng mục | Vì sao đáng làm | Chi phí |
|---|---|---|
| **Flyway migration** | Toàn bộ DDL được đánh phiên bản và tái tạo được. Dựng lại CSDL từ đầu sau khi hỏng chỉ là một lệnh — mà chuyện này **sẽ xảy ra** ở tuần 3. Đồng thời cho báo cáo một phụ lục DDL sạch | ~2 giờ |
| **Bí mật ra khỏi git** | Mật khẩu SA, mật khẩu `uis_link_ro` đưa vào biến môi trường / `.env` không commit. `.gitignore` phải chặn từ commit đầu tiên | ~30 phút |
| **TLS trên JDBC** | `encrypt=true;trustServerCertificate=true` — dữ liệu sinh viên đi qua VPN công cộng | 1 dòng cấu hình |
| **Backup + RPO/RTO** | `.bak` hằng tuần cho mọi database + trước mỗi mốc lớn. Nêu rõ trong báo cáo: **RPO 7 ngày · RTO ~1 giờ** cho môi trường lab | ~1 giờ |
| **Audit ghi điểm** | Một bảng `NhatKySuaDiem` (ai, khi nào, từ giá trị nào sang giá trị nào) ghi bằng trigger trên `Diem`. Vừa là yêu cầu thực tế, vừa **bổ trợ trực tiếp cho phần phân quyền** ở B3/D5 | ~2 giờ |

### Nêu rồi loại — ghi vào báo cáo là "đã nhận diện, ngoài phạm vi"

Kiểm thử bảo mật (pentest, SQL injection scan) · Observability stack đầy đủ (metric, log tập trung, tracing, alerting) · Tự động failover và HA · Mã hóa dữ liệu tại chỗ (TDE) · Quản lý bí mật tập trung (Vault) · Tối ưu chỉ mục tự động · Kiểm thử khôi phục thảm họa.

> ⚠️ Ôm hết nhóm thứ hai là **lấn thẳng vào 40% điểm của Phần F**. Nhận diện được chúng và nói rõ lý do loại chính là dấu hiệu của người hiểu ranh giới phạm vi — mạnh hơn nhiều so với làm dở dang.

### Định vị dự án

> **Phân hệ đăng ký tín chỉ và hồ sơ học tập đa cơ sở** — hạt nhân để mở rộng thành một UIS đầy đủ.

Hình thái mã nguồn: **modular monolith** (Spring Boot, phân tầng theo C8). **Không** microservices — không có ranh giới nghiệp vụ nào trong phạm vi này đủ độc lập để tách thành dịch vụ riêng, và mỗi dịch vụ tách ra là một điểm chết mới lúc demo.

---

# PHẦN D — CƠ CHẾ PHÂN TÁN

## D1. **Nhân bản một chiều**

| Hạng mục | Lựa chọn |
|---|---|
| Loại | **Transactional Replication** |
| Publisher | **database `PTITONE_MASTER`** (đặt trên SRV-HCM) |
| Distributor | **Local** — cùng instance SRV-HCM, bớt một máy phải bật |
| Subscriber | **`PTITONE_HCM`** (cục bộ, cùng instance) · `PTITONE_HN` · `PTITONE_DN` · và mọi site thêm sau |
| Kiểu subscription | **Push** — Distribution Agent chạy tại Distributor, dễ giám sát tập trung |
| Bài báo (article) | `CoSo`, `Khoa`, `ChuongTrinhDaoTao`, `MonHoc`, `MonHocTienQuyet`, `HocKy`, `DanhBaNguoiDung` |
| Bảo vệ Subscriber | **`DENY INSERT/UPDATE/DELETE`** cho mọi role ứng dụng + **trigger chặn ghi** |

⚠️ **Bảo vệ Subscriber là bắt buộc, không phải tùy chọn.** Không có nó, admin tại HN sửa `MonHoc` sẽ *sửa được, chạy được*, rồi **mất trắng khi khởi tạo lại snapshot**. Đây là loại bug âm thầm, xuất hiện đúng lúc demo.

**Không dùng nhân bản hai chiều / merge:** nghiệp vụ chỉ có một nơi ghi danh mục, nên giải quyết xung đột là phức tạp thuần túy, không đổi lấy giá trị nào.

## D2. **Linked Server và tối ưu truy vấn phân tán**

### Bốn báo cáo — use case duy nhất của Linked Server

Vì tầng ứng dụng đã có sẵn DataSource của mọi site, việc "duyệt lớp mở ở site khác" chỉ là truy vấn thẳng — **không cần** Linked Server. Nên Linked Server chỉ phục vụ đúng một nhóm chức năng, và nhóm đó cần đủ dày:

| # | Báo cáo | Ghi chú |
|---|---|---|
| 1 | Sĩ số đăng ký môn X trên toàn hệ thống | Aggregate thuần — minh họa pushdown rõ nhất |
| 2 | Phân bố điểm theo cơ sở cho một môn | `GROUP BY` nhiều chiều |
| 3 | **Danh sách sinh viên học liên cơ sở** | ⭐ **Join chéo site thật sự** — SV ở site này, lớp ở site kia. Không mô phỏng bằng cách khác được → minh chứng thuyết phục nhất cho Linked Server |
| 4 | Tỉ lệ lớp đầy / còn chỗ theo cơ sở | So sánh giữa các site |

### Ba chiến lược, phải đo chứ không phỏng đoán

```
❌ Cách tệ                          ✅ Cách tốt
   HN DB                              HN DB
     │ hàng vạn dòng thô                │ WHERE + GROUP BY tại chỗ
     ▼ qua mạng                         ▼ vài dòng
   HCM: JOIN / GROUP BY               qua mạng → HCM
```

| Chiến lược | Cú pháp | Ưu | Nhược |
|---|---|---|---|
| **Four-part name** | `HN.UIS.dbo.DangKyHocPhan` | Viết gọn, tham số hóa tự nhiên | Optimizer *có thể* kéo cả bảng về rồi mới lọc |
| **`OPENQUERY`** | `OPENQUERY(HN, 'SELECT … GROUP BY …')` | Pass-through — remote server parse, lập plan, lọc dòng, chỉ trả kết quả cuối | **Không nhận biến** → phải dựng dynamic SQL (rủi ro injection, không tái dùng plan). Giới hạn chuỗi 8 KB |
| **`EXEC … AT`** | `EXEC('SELECT … WHERE k=?', @k) AT HN` | Pass-through **có tham số hóa** | Ít tài liệu hơn |

⚠️ **Không mặc định `OPENQUERY` luôn nhanh hơn.** Khi kết quả remote vốn đã lớn, hoặc khi optimizer *đã* đẩy được predicate tốt, four-part name có thể hòa hoặc thắng. **Phải đo** — xem G2.

**Khuyến nghị mặc định:** `OPENQUERY` cho báo cáo không tham số; `EXEC … AT` cho báo cáo có tham số.

Trong báo cáo, gọi đúng thuật ngữ: đẩy `WHERE`/`GROUP BY` xuống site chứa dữ liệu là **chiến lược semi-join / aggregate pushdown** trong tối ưu hóa truy vấn phân tán.

## D3. Saga đăng ký liên cơ sở ➕

### Mô hình Home / Host

| Khái niệm | Định nghĩa |
|---|---|
| **Home Campus** | Cơ sở nơi sinh viên là người học chính quy và sẽ nhận bằng |
| **Host Campus** | Cơ sở đang mở lớp mà sinh viên muốn học |

Mô hình này **có thật**. UM System (Missouri) định nghĩa nguyên văn *home institution* là "the campus in which you are enrolled as a degree-seeking student and from which you will earn your degree", và *host institution* là "the campus from which you would like to take a cross enrollment class".

Chi tiết đắt giá hơn là **quy trình**: sinh viên nộp yêu cầu qua hệ thống của **home campus** → **host campus** xét điều kiện → gửi hướng dẫn ghi danh trong **2–3 ngày làm việc**; học phí do host thu, hóa đơn riêng từng campus; điểm được chia sẻ **sau khi đã công bố**.

> ⭐ **Đây là lập luận mạnh nhất để đưa vào báo cáo:** quy trình thật của một đại học đa cơ sở vốn dĩ **bất đồng bộ, có bước duyệt, và nhất quán cuối**. Không trường nào chạy 2PC giữa hai campus để ghi danh trong 200 ms. Việc không dùng distributed transaction **không phải né tránh kỹ thuật, mà là mô hình hóa đúng nghiệp vụ.**

### Luồng saga

```
[HOME]  1. SV nộp yêu cầu.
           Home kiểm bằng DỮ LIỆU CỦA CHÍNH MÌNH:
             • đang hoạt động, không bị hold
             • tiên quyết (từ bảng điểm + BangDiemMirror)
             • trần tín chỉ, tính CẢ các yêu cầu đang CHO_DUYET
             • trùng lịch với các lớp đã đăng ký
           → 1 giao dịch cục bộ: INSERT YeuCauHocLienCoSo (CHO_DUYET)
                                 với MaYeuCau = idempotency key

        ── SiteContext.runAt(HOST, …) qua JDBC/VPN ──►

[HOST]  2. ★ TRA KetQuaXuLyYeuCau THEO MaYeuCau TRƯỚC TIÊN.
           Đã có bản ghi → TRẢ VỀ NGUYÊN KẾT QUẢ CŨ, không đánh giá lại.

           Chưa có → kiểm bằng DỮ LIỆU CỦA CHÍNH MÌNH:
             • còn chỗ · đợt đăng ký của Host đang mở
             • lớp đang mở và cho phép liên cơ sở
           → 1 giao dịch cục bộ:
             UPDATE sức chứa có điều kiện
             + INSERT DangKyHocPhan (nếu duyệt)
             + INSERT KetQuaXuLyYeuCau (LUÔN LUÔN — cả khi từ chối)

        ◄── trả kết quả ──

[HOME]  3. Cập nhật CHO_DUYET → DA_DUYET / TU_CHOI
           + ghi snapshot lớp vào chính dòng đó   ← read model (C10)
           + tăng SinhVien.SoMonLienCoSo          ← cờ fan-out

        4. Mất mạng ở bước 3 → worker retry cùng MaYeuCau
           → bước 2 trả lại đúng kết quả cũ → retry tất định

        5. Hủy đăng ký → compensating transaction, cũng qua
           KetQuaXuLyYeuCau để idempotent:
           Host DELETE enrollment + trả lại 1 chỗ, trong 1 giao dịch cục bộ
```

### ⚠️ Vì sao `UNIQUE(MaYeuCau)` một mình là **không đủ**

Ràng buộc `UNIQUE` chỉ ghi nhận được các yêu cầu **thành công**. Khi Host **từ chối** (lớp đầy) mà response bị mất trên đường về, Home retry → Host **đánh giá lại từ đầu** → lúc này có người vừa hủy lớp → **kết quả khác lần trước**. Retry trở thành không tất định: 5 lần retry là 5 lần đánh giá độc lập, và sinh viên có thể "được duyệt" ở lần thứ 3 cho một yêu cầu đã bị từ chối ở lần thứ nhất.

Bảng `KetQuaXuLyYeuCau` lưu **mọi kết quả, kể cả từ chối**, nên retry luôn trả về đúng quyết định đã ghi. Đây là mẫu **Idempotent Receiver có lưu phản hồi**, và cùng bảng đó phục vụ luôn đường hủy đăng ký.

**Chi phí thật của cả cơ chế: 2 bảng nhỏ, 1 cột trạng thái, 1 ràng buộc `UNIQUE`.** Đổi lại: không 2PC, không MSDTC (đỡ phải mở port 135 + dải RPC động qua VPN), và **xử lý sự cố gần như miễn phí** (D6).

### Quy tắc nghiệp vụ

| Mức | Quy tắc | Kiểm ở đâu |
|---|---|---|
| **MUST** | Sức chứa lớp | Host — atomic |
| **MUST** | Chống đăng ký trùng | Host — `UNIQUE(MaSinhVien, MaLopHP)` |
| **MUST** | Đợt đăng ký của Host đang mở | Host |
| **MUST** | SV đang hoạt động, không bị hold | Home |
| **MUST** | Trần tín chỉ mỗi kỳ (gồm cả yêu cầu `CHO_DUYET`) | Home |
| **MUST** | Trạng thái duyệt `CHO_DUYET → DA_DUYET/TU_CHOI` | Home |
| SHOULD | Môn tiên quyết | **Home** — bảng điểm nằm ở Home. Ví dụ sách giáo khoa của *push computation to data* |
| SHOULD | Trùng lịch | Home — dùng giờ học trong payload yêu cầu |
| SHOULD | Trần liên cơ sở (≤ 2 môn/kỳ) | Home — một dòng `CHECK` |
| **OUT** | Học phí (thật thì Host thu theo mức của Host, hóa đơn riêng) | Nêu "đã nhận diện, ngoài phạm vi" |
| **OUT** | Học bổng, duyệt bởi cố vấn học tập, giảng viên dạy liên cơ sở | |

### Các tình huống nghiệp vụ được hỗ trợ

Học lại môn trượt · Học vượt để ra trường sớm · Môn không được mở tại cơ sở nhà trong kỳ này · Tránh trễ tiến độ tốt nghiệp · Sinh viên tạm trú tại thành phố khác trong kỳ thực tập.

## D4. **Giao dịch và xử lý tương tranh**

### Cơ chế chính — atomic conditional update

```sql
BEGIN TRAN;

UPDATE LopHocPhan
   SET SoLuongDaDangKy = SoLuongDaDangKy + 1
 WHERE MaLopHP = @MaLopHP
   AND SoLuongDaDangKy < SoLuongToiDa;   -- điều kiện nằm TRONG câu UPDATE

IF @@ROWCOUNT = 0
BEGIN
    ROLLBACK;
    THROW 50001, N'Lớp đã đầy', 1;
END

INSERT INTO DangKyHocPhan (MaLopHP, MaSinhVien, MaCoSoNhaSV, HoTenSinhVien, MaYeuCau, ...)
VALUES (@MaLopHP, @MaSinhVien, @MaCoSoNhaSV, @HoTen, @MaYeuCau, ...);

COMMIT;
```

**Vì sao đúng:** câu `UPDATE` lấy exclusive lock trên dòng lớp, và việc kiểm tra điều kiện diễn ra **bên trong** thao tác ghi. 100 request đồng thời bị **tuần tự hóa trên đúng dòng đó**; đúng 30 câu trả về `@@ROWCOUNT = 1`, 70 câu còn lại trả về 0. Không có khe hở nào.

**Phản ví dụ — nên đưa vào báo cáo:**
```sql
SELECT @dem = COUNT(*) FROM DangKyHocPhan WHERE MaLopHP = @MaLopHP;  -- ❌
IF @dem < @SoLuongToiDa
    INSERT ...;    -- race condition kinh điển: 100 luồng cùng đọc 29
```

### Mức cô lập

**`READ COMMITTED` (mặc định) là đủ.** Tính đúng đắn đến từ exclusive lock của câu `UPDATE`, không đến từ mức cô lập. `SERIALIZABLE` chỉ thêm range lock, giảm thông lượng, không thêm bảo đảm nào ở đây.

**Bật `READ_COMMITTED_SNAPSHOT`** (D11): khi hàng trăm SV cùng làm mới trang "còn bao nhiêu chỗ" trong lúc người khác đang đăng ký, người đọc không bị chặn bởi người ghi. Vẫn đúng, vì `UPDATE` đọc lại dưới lock.

### Phòng thủ nhiều lớp

| Lớp | Cơ chế | Bắt gì |
|---|---|---|
| 1 | `UPDATE … WHERE SoLuongDaDangKy < SoLuongToiDa` | Vượt sức chứa — **cơ chế chính** |
| 2 | `UNIQUE (MaSinhVien, MaLopHP)` | Đăng ký trùng, kể cả khi bấm hai lần |
| 3 | `UNIQUE (MaYeuCau)` filtered index | Retry của saga → idempotent |
| 4 | `CHECK (SoLuongDaDangKy <= SoLuongToiDa)` | **Bất biến do DBMS bảo vệ.** Nếu constraint này từng nổ, nghĩa là có bug ở tầng trên |

> Lớp 4 đáng nhấn mạnh trong báo cáo: *ràng buộc toàn vẹn được đảm bảo bởi hệ quản trị, không phải bởi mã ứng dụng.*

⚠️ **Không dùng `rowversion`/optimistic concurrency cho bộ đếm sức chứa.** Với 100 luồng tranh một dòng nóng, optimistic sinh bão retry. Optimistic hợp cho sửa hồ sơ sinh viên (tranh chấp hiếm), không hợp cho counter.

⚠️ **Thứ tự khóa cố định để tránh deadlock:** luôn `LopHocPhan` trước, `DangKyHocPhan` sau — ở **mọi** luồng, kể cả luồng hủy đăng ký. Đảo thứ tự ở một luồng là đủ sinh deadlock ngẫu nhiên rất khó tái hiện.

### ⚠️ Trần tín chỉ cũng có race — và bản trước bỏ sót

Chống vượt sĩ số **không** đồng nghĩa chống vượt trần tín chỉ. Đó là hai bộ đếm ở **hai nơi khác nhau**:

```
Sinh viên đang có 17 tín chỉ, trần 20.
Bấm đăng ký HAI môn 3 tín chỉ gần như cùng lúc.

Luồng A: SELECT → thấy 17 → 17+3=20 ≤ 20  ✅ cho qua
Luồng B: SELECT → thấy 17 → 17+3=20 ≤ 20  ✅ cho qua
                                    → thực tế thành 23 tín chỉ  ❌
```

Đây **cùng một lớp lỗi** với overbooking, chỉ khác chỗ: sức chứa được bảo vệ tại **Host**, còn trần tín chỉ phải được bảo vệ tại **Home**. Cách chữa cũng giống hệt — một bộ đếm và một `UPDATE` có điều kiện:

```sql
-- Tại HOME, trong cùng giao dịch cục bộ với việc ghi nhận đăng ký
UPDATE SinhVien
   SET SoTinChiDangKyKy = SoTinChiDangKyKy + @SoTinChi
 WHERE MaSinhVien       = @MaSinhVien
   AND SoTinChiDangKyKy + @SoTinChi <= @TranTinChiMoiKy;

IF @@ROWCOUNT = 0
BEGIN
    ROLLBACK;
    THROW 50002, N'Vượt trần tín chỉ của học kỳ', 1;
END
```

Câu `UPDATE` này lấy exclusive lock trên **dòng sinh viên**, nên hai request của cùng một sinh viên bị tuần tự hoá — đúng chỗ cần tuần tự hoá, và không ảnh hưởng sinh viên khác.

**Ba quy tắc kèm theo:**

| | |
|---|---|
| Cộng tín chỉ **ngay khi tạo yêu cầu** liên cơ sở (`CHO_DUYET`), không đợi Host duyệt | Nếu không, sinh viên mở 5 yêu cầu cùng lúc sẽ vượt trần mà không bộ đếm nào biết |
| **Trả lại tín chỉ** khi Host từ chối, khi hủy đăng ký, hoặc khi yêu cầu quá hạn | Cùng một `UPDATE` với dấu trừ, trong giao dịch cục bộ tại Home |
| `CHECK (SoTinChiDangKyKy BETWEEN 0 AND @TranTinChiMoiKy)` | Bất biến do DBMS bảo vệ, giống lớp 4 của bộ đếm sức chứa |

> Bộ đếm này cũng cần **truy vấn đối soát** riêng, giống bộ đếm sức chứa — xem dưới.

### Truy vấn đối soát bất biến

Bộ đếm phi chuẩn hóa có thể lệch với số dòng thật nếu có bug. Chạy sau mỗi lần test tải:

```sql
SELECT l.MaLopHP, l.SoLuongDaDangKy, COUNT(d.MaSinhVien) AS SoDongThat
FROM LopHocPhan l
LEFT JOIN DangKyHocPhan d ON d.MaLopHP = l.MaLopHP
GROUP BY l.MaLopHP, l.SoLuongDaDangKy
HAVING l.SoLuongDaDangKy <> COUNT(d.MaSinhVien);   -- phải LUÔN trả 0 dòng
```

### Liên cơ sở vẫn chỉ là giao dịch cục bộ

```
SV cơ sở HCM  →  đăng ký lớp của HN  →  giao dịch cục bộ TẠI HN  →  Enrollment
```

Không có gì biến nó thành giao dịch phân tán, vì **toàn bộ dữ liệu cần cho ràng buộc (bộ đếm + đăng ký) đều nằm tại HN**. Home chỉ giữ bản ghi yêu cầu, và bản ghi đó không tham gia vào ràng buộc sức chứa. Đây là phần thưởng của lựa chọn ownership ở C2.

## D8. **Giao dịch phân tán (2PC) — chuyển cơ sở sinh viên**

> ⭐ **Đây là yêu cầu số 3 trong năm yêu cầu bắt buộc của giảng viên** (mục 0.1b).

### Vì sao đặt ở đây, không đặt ở đăng ký học phần

| Tiêu chí | **Chuyển cơ sở** ✅ | **Đăng ký học phần** ❌ |
|---|---|---|
| Tần suất | Vài lần mỗi kỳ | 32.000 lượt/ngày cao điểm |
| Tranh chấp | Không — mỗi sinh viên một dòng riêng | Cao — hàng trăm người tranh một dòng lớp |
| Trạng thái trung gian an toàn | **Không có.** Sinh viên không thể "nửa ở HCM nửa ở HN" | Có — `CHO_DUYET` |
| Chi phí giữ lock qua mạng | Không đáng kể | Sụp thông lượng |
| Số CSDL tham gia | **3** — cơ sở cũ, cơ sở mới, Master | 2 |

2PC là **blocking protocol**: nếu coordinator chết sau giai đoạn `prepare`, resource manager giữ lock tới khi có người can thiệp. Cái giá đó chấp nhận được cho một thao tác hiếm và không tranh chấp — **không** chấp nhận được cho đường nóng.

### Thủ tục

```sql
CREATE OR ALTER PROCEDURE sp_ChuyenCoSoSinhVien
    @MaSinhVien  VARCHAR(20),
    @CoSoMoi     VARCHAR(10)
AS
BEGIN
    SET XACT_ABORT ON;        -- ⚠️ BẮT BUỘC với giao dịch phân tán:
                              -- lỗi bất kỳ ở site nào cũng phải rollback toàn bộ
    SET NOCOUNT ON;

    ---- GIAI ĐOẠN 1 — TIỀN ĐIỀU KIỆN (ngoài giao dịch) --------------------
    IF EXISTS (SELECT 1 FROM YeuCauHocLienCoSo
                WHERE MaSinhVien = @MaSinhVien AND TrangThai = 'CHO_DUYET')
        THROW 51001, N'Còn yêu cầu liên cơ sở chưa xử lý xong', 1;

    IF EXISTS (SELECT 1 FROM OutboxSuKien
                WHERE KhoaThucThe = @MaSinhVien AND TrangThai = 'PENDING')
        THROW 51002, N'Còn sự kiện Outbox chưa gửi', 1;

    ---- GIAI ĐOẠN 2 — GIAO DỊCH PHÂN TÁN (nguyên tử trên 3 CSDL) ----------
    BEGIN DISTRIBUTED TRANSACTION;

        -- (a) chèn sang cơ sở MỚI trước  (qua Linked Server)
        SET @sql = N'
            INSERT INTO ' + @SrvMoi + N'.dbo.SinhVien
                   (MaSinhVien, HoTen, NgaySinh, MaCoSoNha, MaCTDT, TrangThai)
            SELECT MaSinhVien, HoTen, NgaySinh, @moi, MaCTDT, ''HOAT_DONG''
              FROM ' + @SrvCu + N'.dbo.SinhVien
             WHERE MaSinhVien = @sv;

            INSERT INTO ' + @SrvMoi + N'.dbo.TaiKhoan
                   (TenDangNhap, MatKhauHash, VaiTro, MaThucThe, MaCoSo)
            SELECT TenDangNhap, MatKhauHash, VaiTro, MaThucThe, @moi
              FROM ' + @SrvCu + N'.dbo.TaiKhoan
             WHERE MaThucThe = @sv;

        -- (b) xoá khỏi cơ sở CŨ
            DELETE FROM ' + @SrvCu + N'.dbo.TaiKhoan WHERE MaThucThe  = @sv;
            DELETE FROM ' + @SrvCu + N'.dbo.SinhVien WHERE MaSinhVien = @sv;';

        EXEC sp_executesql @sql,
             N'@sv VARCHAR(20), @moi VARCHAR(10)', @MaSinhVien, @CoSoMoi;

        -- (c) cập nhật danh bạ định vị tại Master
        --     ⚠️ đúng tên cột của DanhBaNguoiDung: MaCoSo và MaThucThe
        UPDATE PTITONE_MASTER.dbo.DanhBaNguoiDung
           SET MaCoSo      = @CoSoMoi,
               NgayCapNhat = SYSUTCDATETIME()
         WHERE MaThucThe   = @MaSinhVien
           AND LoaiNguoiDung = 'SINH_VIEN';

        IF @@ROWCOUNT <> 1
            THROW 51003, N'Không tìm thấy đúng một dòng danh bạ', 1;

    COMMIT TRANSACTION;
    ---- GIAI ĐOẠN 3 — HẬU XỬ LÝ chạy riêng, idempotent --------------------
    -- dựng lại BangDiemMirror · tính lại SoMonLienCoSo · chuyển lịch sử yêu cầu
END
```

Phần khai báo và kiểm tra tham số ở đầu thủ tục:

```sql
    @MaSinhVien VARCHAR(20),
    @CoSoCu     VARCHAR(10),      -- ⚠️ phải nhận CẢ hai đầu, không hardcode
    @CoSoMoi    VARCHAR(10)
...
    -- Chỉ chấp nhận mã cơ sở CÓ THẬT trong bảng CoSo → chặn SQL injection
    DECLARE @SrvCu SYSNAME, @SrvMoi SYSNAME, @sql NVARCHAR(MAX);

    SELECT @SrvCu  = TenLinkedServer + N'.' + TenDatabase
      FROM PTITONE_MASTER.dbo.CoSo WHERE MaCoSo = @CoSoCu;
    SELECT @SrvMoi = TenLinkedServer + N'.' + TenDatabase
      FROM PTITONE_MASTER.dbo.CoSo WHERE MaCoSo = @CoSoMoi;

    IF @SrvCu IS NULL OR @SrvMoi IS NULL
        THROW 51004, N'Mã cơ sở không hợp lệ', 1;
    IF @CoSoCu = @CoSoMoi
        THROW 51005, N'Cơ sở cũ và mới trùng nhau', 1;
```

> ⚠️ Bảng `CoSo` phải có thêm hai cột `TenLinkedServer` và `TenDatabase` để thủ tục dựng được tên bốn phần. Vì tên chỉ lấy từ bảng `CoSo` (danh sách trắng), dynamic SQL ở đây **không có đường injection**.

**Bốn điểm bắt buộc trong đoạn mã trên:**

| | |
|---|---|
| `SET XACT_ABORT ON` | ⚠️ Không có nó, một lỗi ở site xa có thể **không** làm rollback toàn bộ → dữ liệu hỏng âm thầm. Với `BEGIN DISTRIBUTED TRANSACTION` đây là bắt buộc |
| Chèn **trước**, xoá **sau** | Nếu site đích lỗi thì đã rollback trước khi đụng tới dữ liệu gốc |
| Cập nhật danh bạ **trong** giao dịch | Danh bạ chỉ được đổi khi hồ sơ đã sang tới nơi. Sau đó replication tự đẩy thay đổi ra mọi site |
| **Không hardcode cặp cơ sở** | Thủ tục phải chạy được cho **mọi** cặp (HCM→HN, HN→ĐN, ĐN→HCM…). Tên server/database lấy từ bảng `CoSo` |

### ⚠️ Cấu hình MS DTC — việc mới phải làm ở Phần F

Đây là hạ tầng mà bản thiết kế trước **cố tình né**, giờ bắt buộc phải dựng. Trên **mọi máy** tham gia:

| Việc | Chi tiết |
|---|---|
| Mở firewall | **TCP 135** (RPC Endpoint Mapper) + **dải RPC động 49152–65535** |
| Component Services → My Computer → Properties → MSDTC | Bật **Network DTC Access**, **Allow Inbound**, **Allow Outbound** |
| Chế độ xác thực | ⚠️ **`No Authentication Required`** — bắt buộc trong môi trường **workgroup** (không có domain). `Mutual Authentication` sẽ thất bại vì không có Kerberos |
| Dịch vụ | `Distributed Transaction Coordinator` đặt `Automatic`, khởi động lại sau khi đổi cấu hình |
| Linked Server | `rpc out = true` (đã có ở F5) |

**Kiểm tra nhanh** — chạy từ SRV-HCM, phải chạy được mới đi tiếp:

```sql
BEGIN DISTRIBUTED TRANSACTION;
SELECT TOP 1 1 FROM SRV_HN.PTITONE_HN.dbo.SinhVien;
COMMIT TRANSACTION;
```

> ⚠️ **MS DTC qua VPN là rủi ro cao** — dải port động rộng, và workgroup không có xác thực lẫn nhau. **Phải đưa vào spike tuần 1**, không để tới tuần 3.
>
> **Phương án dự phòng:** nếu MSDTC không chạy được qua VPN, thực hiện giao dịch phân tán giữa **hai named instance trên cùng một máy** — MSDTC nội máy luôn hoạt động. Vẫn là distributed transaction thật, vẫn có `BEGIN DISTRIBUTED TRANSACTION`, vẫn thoả yêu cầu 3. Chỉ mất phần "qua mạng thật", và phải nói rõ trong báo cáo.

### Kịch bản demo — rollback là cảnh đáng quay nhất

```
1. Chạy sp_ChuyenCoSoSinhVien cho một sinh viên  →  thành công
   SELECT tại HCM: không còn · tại HN: đã có · danh bạ: đã đổi

2. TẮT SQL Server ở HN, chạy lại cho sinh viên khác
   →  giao dịch THẤT BẠI, và:
      • sinh viên vẫn NGUYÊN VẸN ở HCM
      • danh bạ KHÔNG bị đổi
      • không có trạng thái nửa vời nào

3. Bật HN lên, chạy lại  →  thành công
```

Đây chính là minh chứng trực quan cho **tính nguyên tử của giao dịch phân tán**, và là thứ phân biệt 2PC với saga: saga sẽ để lại trạng thái trung gian và cần bù trừ; 2PC thì hoặc xong hết, hoặc như chưa từng xảy ra.

### Đo đạc

Benchmark **B5** so sánh 2PC (ở đây) với saga (D3): latency, thời gian giữ lock, và hành vi khi một site chết giữa chừng. Đây là bằng chứng bằng số cho việc **vì sao mỗi cơ chế được đặt ở chỗ của nó**.

---

## D5. **Trigger phân quyền và bảo vệ dữ liệu**

Theo D13, trigger lo **toàn vẹn theo site** — thứ không cần biết người dùng là ai.

> ⚠️⚠️ **`NOT FOR REPLICATION` là bắt buộc trên MỌI trigger đặt ở Subscriber.**
>
> Distribution Agent ghi dữ liệu nhân bản xuống Subscriber **như một thao tác ghi bình thường**. Trigger T1 sẽ chặn chính Agent đó, và replication chết — nhưng triệu chứng nhìn **không liên quan gì tới trigger**, nên rất tốn thời gian truy. Khai báo `CREATE TRIGGER … NOT FOR REPLICATION` để trigger không kích hoạt khi tác nhân replication thao tác.
>
> **Và `DENY` mới là lớp bảo vệ chính**, không phải trigger: login của Distribution Agent thường thuộc `db_owner` nên không bị `DENY` chặn, trong khi mọi role ứng dụng thì bị chặn. Trigger chỉ là lớp phụ, phòng trường hợp ai đó thao tác bằng tài khoản có đặc quyền.

| # | Trigger | Trên bảng | Chặn gì |
|---|---|---|---|
| **T1** | `trg_ChanGhiBangNhanBan` **`NOT FOR REPLICATION`** | `MonHoc`, `Khoa`, `ChuongTrinhDaoTao`, `HocKy`, `DanhBaNguoiDung`, `CoSo` | Mọi `INSERT/UPDATE/DELETE` **tại Subscriber**. Lớp phụ sau `DENY` |
| **T2** | `trg_ChanGhiCheoCoSo` **`NOT FOR REPLICATION`** | `SinhVien`, `GiangVien`, `LopHocPhan`, `DotDangKy` | Ghi dòng có `MaCoSo ≠` mã cơ sở của chính CSDL này. Bảo vệ tính đúng đắn của phân mảnh ở mức DBMS |
| **T3** | `trg_ChanSuaDiemDaKhoa` | `Diem` | Sửa điểm khi lớp đã ở trạng thái `DA_KHOA` |
| ~~T4~~ | ~~`trg_DongBoSoLuongDangKy`~~ | — | ❌ **ĐÃ BỎ.** Xem cảnh báo dưới |

> ⚠️ **Bỏ T4 — không được để ứng dụng và trigger cùng cập nhật một bộ đếm.**
>
> Ứng dụng đã tự tăng `SoLuongDaDangKy` trong câu `UPDATE` có điều kiện (D4). Nếu thêm trigger cũng tăng khi `INSERT DangKyHocPhan` thì **bộ đếm nhảy 2 mỗi lần đăng ký**. Chốt: **ứng dụng sở hữu bộ đếm**, không trigger nào được đụng vào; tính đúng đắn được canh bằng `CHECK` và bằng truy vấn đối soát ở D4.

**Database role — phân quyền theo vai trò:**

```sql
CREATE ROLE r_SinhVien;      -- SELECT trên bảng của mình; không quyền ghi Diem
CREATE ROLE r_GiangVien;     -- SELECT + UPDATE Diem trên lớp mình dạy
CREATE ROLE r_AdminCoSo;     -- CRUD trong phạm vi cơ sở
CREATE ROLE r_AdminMaster;   -- + CRUD bảng danh mục, CHỈ tại Master

-- Tại mọi Subscriber:
DENY INSERT, UPDATE, DELETE ON MonHoc TO r_AdminCoSo, r_AdminMaster;
-- ... tương tự cho các bảng nhân bản còn lại
```

**Cách demo cho mục 3.7 của đề bài:** mở SSMS, đăng nhập lần lượt bằng `sv_test` / `gv_test` / `admin_test`, thử thao tác bị cấm, **chụp màn hình thông báo từ chối**. Trực quan hơn nhiều so với giải thích bằng lời.

## D6. Xử lý sự cố ➕

| # | Kịch bản | Hành vi mong muốn | Điều kiện để đúng |
|---|---|---|---|
| **KB0** ⭐ | **Chỉ dừng DATABASE `PTITONE_MASTER`** (không tắt máy) | **Cả ba cơ sở vận hành đầy đủ:** đăng nhập được, xem lịch học, **đăng ký học phần bình thường**. Chỉ mất: sửa danh mục · nhân bản thay đổi mới · báo cáo toàn hệ thống | Mọi đường đọc danh mục phải trỏ vào **replica cục bộ**, không bao giờ trỏ vào `DS_MASTER`. Đây chính là kiểm chứng Replication Transparency |
| **KB0b** | ⚠️ **Tắt cả MÁY SRV-HCM** (phương án 3 máy) | ❌ **Toàn bộ website ngừng** — vì backend cũng nằm trên máy đó. Đây **không** phải phép thử của Replication Transparency mà là phép thử SPOF của tầng ứng dụng | Muốn diễn KB0 cho sạch: hoặc dùng phương án 4 máy (D15), hoặc chỉ dừng service của riêng database `PTITONE_MASTER` |
| **KB1** | HN chết, SV HCM làm việc bình thường | Hoạt động đầy đủ | `initializationFailTimeout = -1` để ứng dụng vẫn khởi động được khi một site chết; không đường code cục bộ nào chạm DataSource của HN |
| **KB2** | HN chết, SV HCM đăng ký lớp HN | Yêu cầu ở trạng thái `CHO_DUYET`, hiện "đang chờ cơ sở HN xác nhận", có nút thử lại. Retry idempotent | ⭐ Đây là chỗ saga trả cổ tức — nếu dùng 2PC thì kịch bản này là **lock treo** |
| **KB3** | HN chết, SV HCM xem lịch học có môn ở HN | **Vẫn hiện đủ**, kèm "Dữ liệu cơ sở HN tính đến 14:32" | Nhờ read model cục bộ (C10) |
| **KB4** | HN chết, Admin chạy thống kê toàn hệ thống | Trả **kết quả một phần**: số liệu HCM + ĐN, kèm chú thích "Chưa gồm cơ sở HN — không kết nối được lúc 14:32" | Query timeout ngắn + try/catch **theo từng site** |
| **KB5** | Replication đứt | HCM vẫn ghi bình thường, lệnh dồn trong distribution database; HN nối lại thì đuổi kịp | Demo: tắt HN → thêm 5 môn tại HCM → bật HN → xem chúng chảy về |
| **KB6** | Đọc bản sao cũ | SV tại HN **không nhìn thấy** môn mới tạo tại HCM trong lúc đứt | ⭐ Gọi đúng tên: **cái giá của nhất quán cuối**, kèm con số độ trễ đo được (G2) |

> ⭐ **KB0 là cảnh demo mạnh nhất của cả đồ án.** Tắt "trung tâm" mà sinh viên ở cả ba cơ sở vẫn đăng nhập được, vẫn xem lịch, vẫn đăng ký học phần — đó là câu trả lời trực quan nhất cho câu hỏi ở A2 *"vì sao không dùng một CSDL tập trung?"*. Nó cũng làm rõ đúng ranh giới mà team đã chỉ ra: **Master là SPOF của control plane, không phải SPOF của data plane.**
>
> Lưu ý khi diễn: nếu Master colocate trên SRV-HCM (phương án 3 máy), tắt máy đó là **tắt cùng lúc một cơ sở và Master** → kịch bản bị nhòe. Muốn diễn KB0 cho sạch thì hoặc dùng phương án 4 máy, hoặc chỉ dừng **service/database `PTITONE_MASTER`** thay vì tắt cả máy.

⚠️ **`connectionTimeout` + `socketTimeout` ngắn (2–5 s) trên DataSource của site ở xa.** Thiếu cái này, một site chết sẽ hút cạn thread pool và kéo sập cả những request cục bộ vốn đáng lẽ vẫn phải sống.

**Ngoài phạm vi:** tự động failover, quorum, giải quyết xung đột đa chủ, hàng đợi retry có exponential backoff. Một nút "Thử lại" là đủ.

## D7. **Các mức trong suốt đạt được**

> Môn CSDLPT chấm rất nặng phần này, và nhóm **đang làm cả ba mức** — nên phải **gọi đúng tên**, nếu không là làm không công.

| Mức trong suốt | Hiện thực ở đâu | Chứng minh thế nào | Giới hạn (nói thật) |
|---|---|---|---|
| **Phân mảnh** | `/api/lich-hoc/me` trả về lịch học hợp nhất; client không biết dữ liệu nằm ở mấy mảnh | Cùng một request, ba SV khác cơ sở, **cùng một shape response**. SV liên cơ sở thấy cả môn ở site khác trong cùng danh sách. ⭐ **X-Ray hiển thị trực tiếp site nào được chạm** | **Chỉ đạt một phần**: màn hình báo cáo của Admin có nêu tên site tường minh |
| **Vị trí** | `SiteContext` + `RoutingDataSource` + `DanhBaNguoiDung`; client không gửi tham số site nào. ⚠️ **Cơ sở luôn lấy từ danh bạ hoặc từ claim trong JWT đã ký — TUYỆT ĐỐI không tin tham số do client gửi lên**, nếu không là lỗ hổng leo thang đặc quyền | Đổi connection string của một site sang máy khác, **không sửa một dòng code**, hệ thống chạy nguyên. Thêm một cơ sở = thêm một dòng trong `CoSo` | Site đích của đăng ký liên cơ sở được suy ra từ lớp học, không do người dùng chỉ định |
| **Nhân bản** | Ứng dụng đọc `MonHoc` từ replica cục bộ, không biết đó là bản sao | Cùng một câu `SELECT` chạy ở mọi site cho kết quả giống nhau; **execution plan không có toán tử Remote Query nào**. ⭐ X-Ray hiện "Dòng qua mạng: 0" | Nhất quán cuối: sau khi Master ghi, replica trễ vài giây — đo bằng tracer token |

> Cột "Giới hạn" là cột ăn điểm. Nhận rõ hệ thống *chưa* có global query processor và nói ra, luôn được đánh giá cao hơn là tuyên bố đạt trong suốt hoàn toàn rồi bị hỏi vặn.

---

# PHẦN E — ⭐ X-RAY PHÂN TÁN ➕

## E1. Vấn đề nó giải

Toàn bộ Phần C và D nói về những thứ **không nhìn thấy được**. Khi bảo vệ, câu hỏi khó nhất luôn là *"làm sao em chứng minh truy vấn này chạy cục bộ?"* — và câu trả lời thông thường là mở execution plan, một thứ chỉ người trong nghề đọc được.

X-Ray trả lời câu đó bằng một bảng mà bất kỳ ai cũng đọc được, **ngay trong lúc thao tác**.

## E2. Thiết kế

```
Request  →  XRayFilter (mở một XRayTrace theo request scope)
              │
              ▼
         Controller → Service
              │
              ▼
      RoutingDataSource
              │
       ┌──────┴──────┬──────────────┐
       ▼             ▼              ▼
  DS_HCM proxy  DS_HN proxy    DS_DN proxy
       │             │              │
       └──── ghi vào XRayTrace ─────┘
             (site, sql, rows, ms, kiểu truy cập)
              │
              ▼
    Response body: { data: …, _xray: { … } }
```

**Cách thu thập:** bọc mỗi `DataSource` bằng một proxy ghi lại `(maCoSo, câu SQL rút gọn, số dòng, thời gian, có phải OPENQUERY/four-part/replica không)` vào một `XRayTrace` gắn theo request. Không dùng thư viện ngoài.

### ⚠️ Ranh giới đo được — phải phân biệt *measured* và *derived*

Proxy quanh `DataSource` **chỉ nhìn thấy lưu lượng giữa ứng dụng và từng SQL Server**. Nó **không thể** đo lưu lượng *giữa hai instance SQL Server với nhau* qua Linked Server — phần đó nằm hoàn toàn bên trong SQL Server.

| Chỉ số | Nguồn | Nhãn |
|---|---|---|
| Site nào được chạm | Proxy DataSource | **measured** |
| Số câu SQL, thời gian từng site | Proxy DataSource | **measured** |
| Số dòng trả về ứng dụng | Proxy DataSource | **measured** |
| Dùng replica hay Linked Server | Phân loại câu SQL | **measured** |
| **Số dòng đi giữa hai SQL Server** | Actual execution plan — `ActualRows` của toán tử `Remote Query`/`Remote Scan`, lấy bằng `SET STATISTICS XML ON` rồi phân tích XML | **derived (from plan)** |

Giao diện phải **ghi rõ nhãn**, không gộp hai loại làm một. Chỉ số cuối chỉ bật khi người dùng chủ động chạy ở "chế độ đo" (vì `SET STATISTICS XML ON` làm chậm truy vấn) — dùng cho Phần G, không bật mặc định.

Nói rõ ranh giới này làm X-Ray **đáng tin hơn**, không phải kém đi: đo được cái gì thì nói cái đó, phần còn lại chỉ rõ lấy từ đâu.

**Cấu trúc trace:**
```json
{
  "endpoint": "GET /api/thong-ke/mon-hoc",
  "sitesTouched": ["HCM", "HN", "DN"],
  "totalMs": 187,
  "strategy": "OPENQUERY",
  "measured": {
    "rowsToApp": 3,
    "steps": [
      { "site": "HCM", "kind": "LOCAL",     "rows": 1, "ms": 9  },
      { "site": "HN",  "kind": "OPENQUERY", "rows": 1, "ms": 87 },
      { "site": "DN",  "kind": "OPENQUERY", "rows": 1, "ms": 91 }
    ],
    "replicasUsed": ["MonHoc"],
    "degraded": false
  },
  "derivedFromPlan": {
    "available": true,
    "rowsBetweenInstances": 2,
    "source": "Remote Query operator ActualRows"
  }
}
```

## E3. Ba chế độ

### 1. Trace từng request
Bảng nhỏ ở góc màn hình, bật/tắt bằng một nút. Mỗi thao tác của người dùng đều hiện đường đi của nó.

### 2. Phòng điều khiển (Control Room)

```
┌─ PHÒNG ĐIỀU KHIỂN ─────────────────────────────────────┐
│  Database     Trạng thái  Độ trễ giả lập  Nhân bản     │
│  PTITONE_MASTER   ● LIVE      —               Publisher    │
│  PTITONE_HCM      ● LIVE      0 ms            ● 0,4s trễ   │
│  PTITONE_HN       ● LIVE      0 ms   [+200ms] ● 2,3s trễ   │
│  PTITONE_DN       ○ TẮT       —      [bật]    ⚠ dừng       │
│                                                         │
│  Outbox chờ gửi:  HCM 0 · HN 3 · ĐN 12 (site đang tắt) │
│  Danh mục đồng bộ lần cuối: HN 14:32:10 · ĐN 14:21:04  │
└─────────────────────────────────────────────────────────┘
```

Đây là nơi lấy dữ liệu từ port `CatalogHealth`, và là công cụ để diễn kịch bản D6 ngay trên sân khấu.

### 3. So sánh trực tiếp

```
┌─ SO SÁNH CHIẾN LƯỢC ──────────────────────────────────┐
│  Thống kê môn INT1339 · học kỳ 2025-1                  │
│                                                        │
│  four-part name    3.412 ms   84.213 dòng qua mạng    │
│  OPENQUERY           187 ms        2 dòng qua mạng    │
│  backend merge       241 ms      156 dòng qua mạng    │
│                                                        │
│                              [ chạy lại · 10 lần ]     │
└────────────────────────────────────────────────────────┘
```

Đây chính là bảng benchmark của Phần G — nhưng chạy trực tiếp thay vì chép vào báo cáo.

## E4. Giới hạn phạm vi

X-Ray là công cụ **quan sát**, không phải công cụ vận hành. Không xây: lưu trữ trace lâu dài, biểu đồ theo thời gian, cảnh báo, sampling, tích hợp OpenTelemetry. Trace sống trong vòng đời một request và hiện ngay tại chỗ. Vượt quá đó là over-engineering.

---

# PHẦN F — **CÀI ĐẶT VẬT LÝ**
*(mục 3 đề bài — ~40% điểm)*

> ⚠️ **Chụp màn hình TỪNG BƯỚC, ngay khi làm.** Đề bài nhắc "print Screen" ba lần. Nguyên nhân số một khiến nhóm chết ở tuần cuối là phải **dựng lại** replication chỉ để chụp ảnh minh họa.
> Lưu vào `docs/screenshots/<số thứ tự>-<tên bước>/`, đánh số theo đúng thứ tự dưới đây.

## F1. **Cài đặt VPN**

Theo tài liệu giảng viên: **Radmin VPN**. Tailscale là phương án dự phòng nếu Radmin không ổn định (IP `100.x` cố định, xuyên NAT tốt hơn).

Sau khi kết nối: thêm entry vào `hosts` trên **mọi máy**, ánh xạ IP VPN → tên máy. Replication lưu **tên máy**, không lưu IP.

```
26.10.x.x   SRV-HCM
26.10.x.y   SRV-HN
26.10.x.z   SRV-DN
```

## F2. **Tạo đường link kết nối giữa các server**

Kiểm tra hai chiều bằng `ping <tên máy>` và `telnet <tên máy> 1433` trước khi đi tiếp. Nếu bước này chưa thông thì mọi bước sau đều vô nghĩa.

## F3. **Cài đặt SQL Server**

| Hạng mục | Lựa chọn | Lý do |
|---|---|---|
| Edition | **Developer Edition** | ⚠️ **Bắt buộc.** Mọi edition **trừ Express và Compact** mới làm được Publisher. Express chỉ làm Subscriber và **không có SQL Server Agent** — không Agent thì không có Snapshot / Log Reader / Distribution Agent, tức là **không có replication ở bất kỳ dạng nào** |
| Instance | Default instance, TCP **1433** cố định (2 site) · named instance nếu ≥3 site trên ít máy | Default instance bỏ được SQL Browser khỏi phương trình |
| Xác thực | Mixed Mode | Cần SQL login cho Linked Server và cho demo role |
| Firewall | ⚠️ Mở TCP 1433 **CHỈ trên interface/subnet VPN**, KHÔNG mở trên interface public — nếu không là phơi CSDL ra internet · thêm UDP 1434 nếu dùng named instance · **không bao giờ forward 1433 trên modem** | |
| Collation | `Vietnamese_CI_AS` | Sắp xếp tiếng Việt đúng |

## F4. **Kiểm tra dịch vụ SQL Server Agent**

Agent phải ở trạng thái **Running** và **Startup Type = Automatic** trên mọi máy.

⚠️ **Tài khoản chạy Agent — đây là cái bẫy nặng nhất trong môi trường workgroup (không domain):** nếu Agent chạy bằng virtual account `NT Service\SQLSERVERAGENT`, nó **không xác thực được ra thư mục chia sẻ trên máy khác**.

**Cách xử lý:** tạo **cùng một tài khoản Windows cục bộ, trùng username và trùng password trên MỌI máy** (pass-through authentication của workgroup), rồi cho Agent chạy bằng tài khoản đó.

## F4b. **Cấu hình MS DTC** — bắt buộc cho giao dịch phân tán

> ⭐ Cần cho **yêu cầu 3** của giảng viên (D8). ⚠️ Đây là hạng mục **rủi ro cao nhất sau replication** — phải test trong spike tuần 1.

Trên **mọi máy** tham gia giao dịch phân tán:

| Việc | Chi tiết |
|---|---|
| Firewall | Mở **TCP 135** (RPC Endpoint Mapper) **+ dải RPC động 49152–65535** |
| `dcomcnfg` → Component Services → My Computer → Properties → tab **MSDTC** → Security Configuration | Bật **Network DTC Access** · **Allow Inbound** · **Allow Outbound** |
| Chế độ xác thực | ⚠️ **`No Authentication Required`** — bắt buộc trong **workgroup**. `Mutual Authentication` sẽ thất bại vì không có Kerberos |
| Dịch vụ | `Distributed Transaction Coordinator` → `Automatic`, **khởi động lại** sau khi đổi cấu hình |
| Linked Server | `rpc out = true` (đã đặt ở F5) |

**Kiểm tra — phải chạy được mới đi tiếp:**

```sql
BEGIN DISTRIBUTED TRANSACTION;
SELECT TOP 1 1 FROM SRV_HN.PTITONE_HN.dbo.SinhVien;
COMMIT TRANSACTION;
```

Lỗi hay gặp và ý nghĩa:

| Thông báo | Nguyên nhân |
|---|---|
| `Unable to begin a distributed transaction` | Dịch vụ DTC chưa chạy, hoặc chưa bật Network DTC Access |
| `The transaction manager has disabled its support for remote/network transactions` | Chưa bật Allow Inbound/Outbound ở máy còn lại |
| `No such host is known` / treo lâu rồi timeout | Phân giải tên máy sai, hoặc dải RPC động bị firewall chặn |
| `MSDTC ... authentication` | Đang để `Mutual Authentication` trong môi trường workgroup |

> **Dự phòng nếu MSDTC không qua được VPN:** thực hiện giao dịch phân tán giữa **hai named instance trên cùng một máy** — DTC nội máy luôn hoạt động. Vẫn là `BEGIN DISTRIBUTED TRANSACTION` thật, vẫn thoả yêu cầu 3, chỉ mất phần "qua mạng thật" và phải nói rõ trong báo cáo.

---

## F5. **Tạo Linked Server**

Linked Server là đối tượng **ở cấp instance**, nên định nghĩa một lần trên **SRV-HCM** là dùng được cho mọi database trên máy đó. Báo cáo tổng hợp chạy trong ngữ cảnh **`PTITONE_HCM`**, vì dữ liệu cần tổng hợp là dữ liệu vận hành chứ không phải dữ liệu tham chiếu.

Hình sao từ SRV-HCM — chỉ N−1 định nghĩa:

```sql
EXEC sp_addlinkedserver   @server = 'SRV_HN', @srvproduct = '',
                          @provider = 'MSOLEDBSQL', @datasrc = 'SRV-HN';

-- 1) CHẶN mặc định: mọi login KHÔNG được ánh xạ đều bị từ chối
EXEC sp_addlinkedsrvlogin @rmtsrvname = 'SRV_HN', @useself = 'false',
                          @locallogin = NULL,
                          @rmtuser = NULL, @rmtpassword = NULL;

-- 2) CHỈ ánh xạ đúng login chạy báo cáo
EXEC sp_addlinkedsrvlogin @rmtsrvname = 'SRV_HN', @useself = 'false',
                          @locallogin = 'uis_report',
                          @rmtuser = 'uis_link_ro', @rmtpassword = '***';

EXEC sp_serveroption 'SRV_HN', 'rpc out', 'true';   -- cần cho EXEC … AT
```

⚠️ **Không để credential dùng chung cho mọi local login.** Bước 1 đặt ánh xạ mặc định thành "không kết nối được", bước 2 chỉ mở cho đúng login chạy báo cáo. Tài khoản đầu xa `uis_link_ro` **chỉ cần quyền `SELECT`** — nguyên tắc đặc quyền tối thiểu.

### ⚠️ Đường thứ hai: quyền GHI cho chuyển cơ sở

Thủ tục ở **D8** phải `INSERT` và `DELETE` tại site xa, nên **`uis_link_ro` chỉ đọc là không đủ**. Cần một ánh xạ thứ hai, tách bạch:

```sql
-- 3) Đường GHI — CHỈ dành cho login chạy thủ tục chuyển cơ sở
EXEC sp_addlinkedsrvlogin @rmtsrvname = 'SRV_HN', @useself = 'false',
                          @locallogin = 'uis_chuyencoso',
                          @rmtuser = 'uis_link_rw', @rmtpassword = '***';
```

| Login cục bộ | Ánh xạ tới | Quyền ở site xa | Dùng cho |
|---|---|---|---|
| *(mọi login khác)* | — | **không kết nối được** | — |
| `uis_report` | `uis_link_ro` | `SELECT` trên bảng báo cáo | Thống kê toàn hệ thống |
| `uis_chuyencoso` | `uis_link_rw` | `INSERT`/`DELETE` **chỉ trên** `SinhVien`, `TaiKhoan` | **Chỉ** thủ tục D8 |

> ⚠️ **Đính chính một tuyên bố lặp lại nhiều lần trong tài liệu này.** Câu *"Linked Server chỉ dùng cho báo cáo"* **không còn đúng tuyệt đối** sau khi thêm yêu cầu bắt buộc số 3. Phát biểu chính xác là:
> **Linked Server có đúng hai công dụng — (1) báo cáo tổng hợp, chỉ đọc; (2) giao dịch phân tán chuyển cơ sở, có ghi nhưng chỉ trên hai bảng và chỉ bởi một login riêng.** Nó vẫn tuyệt đối không phục vụ đăng nhập, đăng ký hay xem điểm.

Kiểm tra: `SELECT TOP 1 * FROM OPENQUERY(SRV_HN, 'SELECT 1 AS ok');`

## F6. **Tạo Publication và Subscription**

Theo **tài liệu hướng dẫn của giảng viên**, dùng **wizard SSMS** (đề bài yêu cầu chụp từng màn hình → nghĩa là các bước wizard).

| Bước | Lựa chọn |
|---|---|
| Publisher | Database **`PTITONE_MASTER`** — **không phải** `PTITONE_HCM` |
| Distributor | **Local**, cùng instance với Publisher (SRV-HCM hoặc SRV-MASTER tùy D15) |
| ⚠️ **Retention — chỉnh CẢ HAI, và ĐẶT BẰNG NHAU** | **(a)** Distribution retention `@max_distretention` (mặc định 72 giờ) — thời gian lệnh được **giữ**. **(b)** Subscription expiration `@retention` (mặc định 336 giờ) — thời gian subscription **hết hạn**. **Đặt cả hai = 720 giờ (30 ngày).** ⚠️ Thời gian tắt máy tối đa = **min(a, b)** — đặt lệch nhau thì con số nhỏ hơn mới là giới hạn thật |
| ⚠️ **Snapshot folder** | **Bắt buộc là UNC share** — **KHÔNG** để mặc định `C:\Program Files\...\ReplData`, vì máy Subscriber không thể với tới. **Đây là lỗi số một giết các nhóm.** Share phải nằm trên **máy chạy Distributor**, không phải máy chạy Publisher (trùng nhau ở đây, nhưng nhớ nguyên tắc): 3 máy → `\\SRV-HCM\repldata` · 4 máy → `\\SRV-MASTER\repldata` |
| Quyền trên share | Tài khoản Windows chung ở F4 phải có quyền đọc/ghi |
| Loại publication | **Transactional** |
| Article | `CoSo`, `Khoa`, `ChuongTrinhDaoTao`, `MonHoc`, `MonHocTienQuyet`, `HocKy`, `DanhBaNguoiDung` |
| Subscription | **Push** — ba subscription: `PTITONE_HCM` (cục bộ, cùng instance — dễ nhất, làm trước để kiểm chứng), `PTITONE_HN`, `PTITONE_DN` |
| Kiểm tra | Replication Monitor phải xanh; đẩy một dòng test và xác nhận nó tới nơi |

## F7. **Thử các giao tác — mục 3.7**

### a. **Nhập dữ liệu**
Thêm sinh viên · mở lớp học phần · đăng ký học phần · nhập điểm. Mỗi thao tác chụp trước/sau.

### b. **Hiển thị dữ liệu** — kiểm chứng cơ chế phân tán

| Kiểm chứng | Cách làm | Kết quả mong đợi |
|---|---|---|
| **Phân mảnh ngang** | `SELECT COUNT(*) FROM SinhVien` tại từng site | Số khác nhau; tổng = tổng toàn trường; không dòng nào trùng |
| **Dẫn xuất** | `SELECT * FROM DangKyHocPhan` tại HN | Chỉ chứa đăng ký của các lớp mở tại HN |
| **Nhân bản** | Thêm một môn tại HCM, đợi vài giây, `SELECT` tại HN và ĐN | Xuất hiện ở cả hai |
| **Bảo vệ Subscriber** | `INSERT INTO MonHoc` tại HN | ❌ Bị từ chối — chụp thông báo lỗi |
| **Trigger chéo site** | `INSERT SinhVien` với `MaCoSoNha='HN'` vào CSDL HCM | ❌ Bị từ chối — chụp thông báo lỗi |
| **Database role** | Đăng nhập `sv_test`, thử `UPDATE Diem` | ❌ Bị từ chối — chụp thông báo lỗi |
| **Linked Server** | `SELECT * FROM OPENQUERY(SRV_HN, '…')` từ HCM | Trả về dữ liệu của HN |

### c. **Thống kê** — bốn báo cáo ở D2, chạy trong ngữ cảnh `PTITONE_HCM` (nơi có Linked Server tới các site còn lại).

### d. **Thử transaction**
Chạy đăng ký đồng thời (G3), đối soát bất biến (D4), và demo compensating transaction khi hủy đăng ký liên cơ sở.

### e. ⭐ **Giao dịch phân tán — yêu cầu 3 của giảng viên**

| Bước | Việc | Kết quả mong đợi | Chụp |
|---|---|---|---|
| 1 | Kiểm tra MSDTC bằng `BEGIN DISTRIBUTED TRANSACTION` đơn giản (F4b) | Chạy được, không lỗi | ☐ |
| 2 | `EXEC sp_ChuyenCoSoSinhVien` cho một sinh viên | Thành công. `SELECT` tại HCM: không còn · tại HN: đã có · danh bạ: đã đổi | ☐ |
| 3 | **TẮT SQL Server ở HN**, chạy lại cho sinh viên khác | ❌ **Giao dịch thất bại** — và sinh viên vẫn **nguyên vẹn** ở HCM, danh bạ **không** bị đổi, không có trạng thái nửa vời nào | ☐ |
| 4 | Bật HN lên, chạy lại | Thành công | ☐ |

> Bước 3 là cảnh đáng quay nhất của cả mục 3.7 — nó chứng minh **tính nguyên tử của giao dịch phân tán** một cách trực quan, và cho thấy khác biệt với saga: saga để lại trạng thái trung gian cần bù trừ, còn 2PC thì hoặc xong hết, hoặc như chưa từng xảy ra.

---

# PHẦN G — KIỂM THỬ VÀ ĐO ĐẠC

## G1. Sinh dữ liệu ⚠️ **điều kiện cần cho mọi benchmark**

| Bảng | Số lượng tối thiểu |
|---|---|
| `SinhVien` | 8.000 HCM · 5.000 HN · 3.000 ĐN |
| `MonHoc` | ~300 |
| `LopHocPhan` | ~600 mỗi cơ sở, trải 3 học kỳ |
| `DangKyHocPhan` | ~50.000 mỗi cơ sở |
| `Diem` | ~40.000 mỗi cơ sở |
| Liên cơ sở | ~2% sinh viên |

> ⚠️ **Đo trên 50 dòng thì four-part name và `OPENQUERY` ra kết quả giống hệt nhau**, và bạn không có gì để viết vào báo cáo. Đây là việc của **tuần 2**, không phải tuần cuối.

## G2. Bộ benchmark

| # | So sánh | Đo gì | Trả lời câu hỏi nào |
|---|---|---|---|
| **B1** | four-part vs `OPENQUERY` vs `EXEC … AT` | Elapsed · CPU · logical reads · **số dòng qua mạng** | Chiến lược truy vấn phân tán nào đúng? |
| **B2** | Join `MonHoc` replica cục bộ **vs** join qua Linked Server | Elapsed · số dòng qua mạng | ⭐ **Bằng chứng định lượng cho quyết định nhân bản** |
| **B3** | Linked Server vs backend merge cho cùng báo cáo | Elapsed · số dòng · hành vi khi 1 site chết | Ranh giới giữa hai cách ghép dữ liệu |
| **B4** | Đăng ký cục bộ vs đăng ký liên cơ sở | Latency p50/p95 | Cái giá của saga |
| **B5** | Saga vs 2PC (G5) | Latency · thời gian giữ lock · hành vi khi site chết | ⭐ Trả lời "sao không dùng distributed transaction?" |
| **B6** | Độ trễ nhân bản | Tracer token (`sp_posttracertoken`) | Nhất quán cuối trễ bao lâu? |

### Phương pháp đo — và thống kê nào hợp lệ với cỡ mẫu nào

| Loại phép đo | Cỡ mẫu | Được phép công bố | **Không** được công bố |
|---|---|---|---|
| Benchmark truy vấn (B1, B2, B3) | 30 lần chạy | **Trung vị + khoảng tứ phân vị (IQR)** + min/max | ❌ p95, p99 — 30 mẫu không đủ |
| Test tương tranh (G3) | 100 luồng → 100 mẫu | **Trung vị + p95** | ⚠️ p99 chỉ khi nâng lên ≥1.000 mẫu |
| Độ trễ nhân bản (B6) | ≥20 tracer token | Trung vị + max | ❌ phân vị cao |

> ⚠️ Công bố p95 từ 10 mẫu là **sai về thống kê** — p95 của 10 mẫu chỉ là giá trị lớn thứ hai. Nếu muốn có p95/p99 cho benchmark truy vấn thì phải nâng cỡ mẫu, còn không thì báo cáo trung vị và nói rõ đó là trung vị.

Giữa các lần đo nguội dùng `DBCC DROPCLEANBUFFERS` (chỉ trên máy lab). Ghi rõ trong báo cáo: đo nguội hay đo nóng, cỡ mẫu bao nhiêu, thống kê nào.

Số dòng đi giữa hai instance lấy từ **actual execution plan** — thuộc tính *Actual Number of Rows* của toán tử `Remote Query`/`Remote Scan` — và phải ghi nhãn là **derived**, không phải measured (xem E2).

## G3. Kiểm thử tương tranh

```
Lớp: SoLuongToiDa = 30
100 luồng (ExecutorService + CountDownLatch để bắn đồng thời thật)
   ↓
Khẳng định:  COUNT(*) DangKyHocPhan  = 30  (chính xác, không phải "khoảng 30")
             SoLuongDaDangKy         = 30
             số request bị từ chối    = 70
             số deadlock              = 0
             truy vấn đối soát D4     = 0 dòng
Báo cáo:     p50 / p95 latency · throughput
```

**Biến thể liên cơ sở:** 50 SV HCM + 50 SV HN cùng tranh 30 chỗ của một lớp tại HN. Kết quả vẫn phải là đúng 30. ⭐ Đây là cảnh demo mạnh nhất của cả đồ án.

## G4. Kiểm thử sự cố

Chạy lần lượt sáu kịch bản D6, mỗi kịch bản chụp màn hình trạng thái ứng dụng. Dùng Phòng điều khiển của X-Ray để bật/tắt site ngay trong lúc demo.

## G5. Thí nghiệm đối chứng: Saga vs 2PC ➕

Cài một stored procedure dùng MS DTC làm **cùng một việc** với saga, rồi đo:

Khác với bản trước, **cả hai cơ chế đều đã được cài đặt thật** trong hệ thống — saga cho đăng ký liên cơ sở (D3), 2PC cho chuyển cơ sở (D8). Thí nghiệm này cài thêm **một biến thể 2PC cho đăng ký liên cơ sở** để so sánh trên cùng một nghiệp vụ:

| Đo trên nghiệp vụ *đăng ký liên cơ sở* | Saga + idempotency (đang dùng) | 2PC (biến thể đối chứng) |
|---|---|---|
| Latency p50 / p95 | ? | ? |
| Thời gian giữ lock trên dòng lớp | ? | ? |
| Thông lượng khi 100 luồng cùng đăng ký | ? | ? |
| Khi Host chết giữa chừng | Yêu cầu ở `CHO_DUYET`, retry được | Lock treo tới khi DTC resolve |
| Cấu hình cần thêm | Không | Port 135 + dải RPC động |

**Câu hỏi thí nghiệm này trả lời:** *"Vì sao chuyển cơ sở dùng 2PC mà đăng ký liên cơ sở lại dùng saga?"* — trả lời **bằng số liệu** thay vì bằng lý lẽ. Chi phí: một proc + một buổi đo.

> ⭐ Đây là mục làm nên khác biệt trong báo cáo. Nhiều nhóm cài được một cơ chế; ít nhóm cài được **cả hai và giải thích được vì sao mỗi cái nằm ở chỗ của nó**.

## G6. Phân tích và bác bỏ phân mảnh dọc ➕

Đề bài chỉ liệt kê ba loại (ngang, dẫn xuất, nhân bản), nên phân mảnh dọc **không nằm trong phạm vi**. Nhưng viết một mục ngắn *đã cân nhắc và bác bỏ có căn cứ* mạnh hơn nhiều so với im lặng:

> Chúng tôi đã xét khả năng phân mảnh dọc bảng `SinhVien` thành phần học vụ (đọc thường xuyên) và phần hồ sơ (ảnh, địa chỉ, liên hệ khẩn cấp — đọc hiếm). Đo trên tập dữ liệu thật cho thấy phần hồ sơ chỉ chiếm X% kích thước bản ghi và mọi truy vấn nóng đều đã dùng chỉ mục phủ, nên việc tách sẽ thêm một phép nối cho nhóm truy cập nhiều nhất mà không giảm đáng kể I/O. Vì vậy chúng tôi giữ nguyên bảng và không áp dụng phân mảnh dọc.

## G7. Demo deadlock ➕

Cố tình đảo thứ tự khóa ở một luồng để tạo deadlock, bắt lại bằng **Extended Events**, hiện deadlock graph, rồi sửa bằng cách thống nhất thứ tự khóa và chạy lại cho thấy hết deadlock. Rẻ, và rất thuyết phục về mặt học thuật.

---

# PHẦN H — LỘ TRÌNH 8 TUẦN

```
TUẦN 1  ── Phân tích + Cổng chặn kỹ thuật
  □ Đặt vấn đề · Ma trận phân quyền · ERD
  □ ★ BẢNG TẦN SUẤT TRUY CẬP (số liệu thật, có giả định ghi rõ)
  □ ★ SPIKE ngày 1–3 — CỔNG CHẶN, gồm HAI phần:
       (a) Replication: INSERT ở A → ≤10s thấy ở B
       (b) MS DTC: BEGIN DISTRIBUTED TRANSACTION qua Linked Server chạy được
       → cả hai PASS mới đi tiếp. Không PASS → Plan B (I2)
  □ Chốt D12 (2 hay 3 site) + D10 cuối tuần, dựa trên kết quả spike
  □ ★ Bắt đầu chụp screenshot từ hôm nay

TUẦN 2  ── Thiết kế + Dữ liệu
  □ Thiết kế CSDL quan hệ · Ownership Matrix
  □ Lược đồ phân mảnh · Lược đồ ánh xạ · Sơ đồ định vị (3 ký hiệu)
  □ Tuyên bố Client/Server · Mô hình front-end/back-end · Hai chế độ ghi (C0)
  □ ★ Tạo 4 database: PTITONE_MASTER + PTITONE_HCM/HN/DN
  □ ★ Viết schema + câu UPDATE có điều kiện + 4 lớp ràng buộc
  □ ★ Sinh dữ liệu lớn (G1)

TUẦN 3  ── ★ CÀI ĐẶT VẬT LÝ — TUẦN NẶNG ĐIỂM NHẤT
  □ VPN · Link mạng · SQL Server · Agent
  □ ★ MS DTC (F4b) — port 135 + dải RPC động + No Authentication Required
  □ Linked Server (hình sao từ SRV-HCM)
  □ Publication trên PTITONE_MASTER
  □ Subscription: PTITONE_HCM (cục bộ — LÀM TRƯỚC để kiểm chứng)
                  → rồi mới tới PTITONE_HN, PTITONE_DN qua VPN
  □ ★ Chụp TỪNG MÀN HÌNH, đánh số ngay khi làm

TUẦN 4  ── ★ Mục 3.7 — hoàn tất phần BẮT BUỘC
  □ Trigger T1–T3 (NOT FOR REPLICATION) · Database role · demo phân quyền qua SSMS
  □ Nhập / Hiển thị / Thống kê (4 báo cáo)
  □ Thử transaction · test 100 luồng · đối soát bất biến
  □ ★ sp_ChuyenCoSoSinhVien (D8) + demo rollback khi tắt site đích
  ✅ ĐẾN ĐÂY CẢ NĂM YÊU CẦU BẮT BUỘC ĐÃ XONG — ~75% điểm đã an toàn

TUẦN 5  ── Ứng dụng nền tảng ➕
  □ apps/api: SiteContext · RoutingDataSource · 3 port · repository
  □ apps/web: đăng nhập · lịch học · đăng ký · bảng điểm · admin danh mục

TUẦN 6  ── Liên cơ sở ➕
  □ Saga + idempotency + compensating transaction
  □ Read model (snapshot lớp) · cờ fan-out
  □ Outbox + worker + write-back điểm + upsert có Version

TUẦN 7  ── ⭐ X-Ray + Đo đạc ➕
  □ Trace từng request · Phòng điều khiển · So sánh chiến lược
  □ Chạy toàn bộ B1–B6 · thí nghiệm 2PC (G5) · demo deadlock (G7)
  □ Sáu kịch bản sự cố (G4)

TUẦN 8  ── Hoàn thiện
  □ Báo cáo · slide · kịch bản demo · tổng duyệt 2 lần
  □ Tuần đệm cho việc phát sinh
```

## Phân vai 5 người

| Vai | Phụ trách | Gánh % điểm |
|---|---|---|
| **Hạ tầng CSDL 1** | VPN · SQL Server · Agent · Linked Server · Publication (F1–F6) | ~30% |
| **Hạ tầng CSDL 2** | Schema · trigger · role · transaction · sinh dữ liệu · benchmark (F7, G) | ~15% |
| **Tài liệu** | ⚠️ **Toàn thời gian Phần A–C.** Không kiêm code | ~30% |
| **Sơ đồ & thiết kế** | ERD · lược đồ phân mảnh/ánh xạ/định vị · kiến trúc | ~25% |
| **Ứng dụng & Demo** | `apps/api`, `apps/web`, X-Ray · kịch bản demo · quản lý kho screenshot | ~10% |

⚠️ **Phải có một người làm tài liệu toàn thời gian.** Với barem ~75% nằm ở tài liệu và screenshot, để tài liệu cho người rảnh viết vào tuần cuối là cách hỏng bài phổ biến nhất.

---

# PHẦN J — KHUNG MÃ NGUỒN ➕

> Đây là **khung sườn**, không phải mã cài đặt. Nó trả lời câu *"thứ này nằm ở đâu"* và *"chỗ này dùng mẫu thiết kế gì"*, để 5 người viết code không giẫm lên nhau.
> ➕ Không thuộc phần báo cáo bắt buộc (mục 2–3 đề bài).

**Nguyên tắc xuyên suốt:** chỉ đặt tên những mẫu thiết kế **thật sự được dùng**. Thêm mẫu cho oai chính là chỗ "professional" biến thành "over-engineering".

---

## J1. Backend — `apps/api`

### Kiến trúc: Ports & Adapters (Hexagonal), modular monolith

Bốn tầng, **phụ thuộc chỉ đi vào trong**:

```
interfaces  ──►  application  ──►  domain
                      ▲
infrastructure  ──────┘   (cài đặt các PORT do application định nghĩa)
```

`domain` **không import** Spring, JDBC, hay bất cứ thứ gì của hạ tầng. Đó là điều kiện để quy tắc nghiệp vụ kiểm thử được mà không cần CSDL.

### Cây thư mục

```
apps/api/src/main/java/vn/ptit/one/
│
├── PtitOneApplication.java
│
├── shared/                      ── hạ tầng dùng chung, không thuộc nghiệp vụ nào
│   ├── site/
│   │   ├── SiteContext              ThreadLocal + runAt(site, action)
│   │   ├── SiteRoutingDataSource    AbstractRoutingDataSource
│   │   ├── SiteRegistry             đọc bảng CoSo — KHÔNG hardcode danh sách site
│   │   └── MaCoSo                   value object
│   ├── xray/
│   │   ├── XRayTrace                gom bước theo request scope
│   │   ├── XRayFilter               mở/đóng trace, gắn vào response
│   │   └── TracingDataSourceProxy   bọc DataSource, ghi (site, sql, rows, ms)
│   ├── outbox/
│   │   ├── OutboxEvent · OutboxRepository
│   │   └── OutboxWorker             @Scheduled — upsert TRƯỚC, đánh SENT SAU
│   └── error/
│       ├── DomainException · MaLoi
│       └── GlobalExceptionHandler
│
├── domain/                      ── quy tắc thuần, không phụ thuộc hạ tầng
│   ├── sinhvien/                    SinhVien · TrangThaiSinhVien
│   ├── hocphan/                     LopHocPhan · SucChua · DangKyHocPhan
│   ├── diem/                        Diem · CongThucDiem · NguongDat
│   ├── liencoso/                    YeuCauLienCoSo · TrangThaiYeuCau
│   └── policy/                      TranTinChi · TienQuyet · TrungLich
│
├── application/                 ── use case + PORT
│   ├── port/
│   │   ├── CrossSiteQuery           đọc chéo site theo người dùng
│   │   ├── GlobalReport             tổng hợp toàn hệ thống
│   │   └── CatalogHealth            lastSyncedAt / isStale
│   ├── dangky/
│   │   ├── DangKyHocPhanService     luồng cùng cơ sở — 1 giao dịch cục bộ
│   │   └── DangKyLienCoSoSaga       ⭐ điều phối saga
│   ├── diem/                        NhapDiemService · DongBoDiemService
│   ├── chuyencoso/                  ChuyenCoSoService — CHỈ gọi stored proc 2PC
│   ├── danhmuc/                     QuanLyDanhMucService (ghi vào DS_MASTER)
│   └── baocao/                      BaoCaoTongHopService
│
├── infrastructure/              ── ADAPTER cài đặt các PORT
│   ├── persistence/                 *JdbcRepository theo từng aggregate
│   ├── crosssite/                   FanOutCrossSiteQuery
│   ├── report/
│   │   ├── LinkedServerGlobalReport     ← dùng cho môn học (OPENQUERY)
│   │   └── BackendMergeGlobalReport     ← đối chứng benchmark B3
│   ├── health/                      TracerTokenCatalogHealth
│   ├── security/                    JwtService · SiteContextFilter
│   └── config/                      DataSourceConfig · SecurityConfig · FlywayConfig
│
└── interfaces/rest/             ── Controller + DTO + mapper
```

### Mẫu thiết kế — và chỗ dùng

| Mẫu | Ở đâu | Giải quyết gì |
|---|---|---|
| **Ports & Adapters** | `application/port` ↔ `infrastructure` | Cô lập công nghệ bắt buộc của môn học |
| **Repository** | `infrastructure/persistence`, một lớp mỗi aggregate | Gom SQL về một chỗ, dễ trỏ vào lúc bảo vệ |
| **Saga (điều phối)** | `DangKyLienCoSoSaga` | Nhất quán nhiều site không cần 2PC |
| **Idempotent Receiver** | `KetQuaXuLyYeuCau` ở Host | Retry tất định, lưu **cả** kết quả từ chối |
| **Transactional Outbox** | `shared/outbox` | Chống mất sự kiện khi ghi hai nơi |
| **Read Model / Projection** | `BangDiemMirror`, snapshot lớp | Tách đường đọc khỏi nguồn sự thật |
| **Routing DataSource + Context Object** | `shared/site` | Location Transparency |
| **Strategy** | `GlobalReport` có **2** cài đặt | Đổi chiến lược truy vấn phân tán để đo B3 |
| **Proxy / Decorator** | `TracingDataSourceProxy` | Thu thập X-Ray mà không sửa repository |
| **Template Method** *(nhẹ)* | `OutboxWorker` | Cùng vòng lặp cho mọi loại sự kiện |

⚠️ **Không dùng:** Factory chồng Factory · Abstract Factory · Visitor · `DatabaseProvider` · `RepositoryFactory` · plugin system. Không có nhu cầu thật nào trong dự án gọi chúng ra.

### Ba ranh giới không được vượt

| # | Ranh giới | Vi phạm thì sao |
|---|---|---|
| 1 | `domain` không import Spring/JDBC | Mất khả năng test quy tắc nghiệp vụ không cần CSDL |
| 2 | Một `@Transactional` = **một site** | Ghi nhầm site hoặc mất nguyên tử, **không ném lỗi** |
| 3 | `interfaces` không gọi thẳng `infrastructure` | Controller lệ thuộc SQL, hết đường thay adapter |

---

## J2. Frontend — `apps/web`

### Kiến trúc: Feature-Sliced (rút gọn)

Ba tầng, phụ thuộc **một chiều**: `app → features → shared`.
**Feature không import lẫn nhau. `shared` không import feature.**

```
apps/web/src/
│
├── app/                     router · providers · layout · guard theo vai trò
│
├── shared/
│   ├── ui/                  design system: Button · Table · Field · Dialog · Badge
│   ├── api/                 client fetch · gắn JWT · map lỗi · BÓC _xray
│   ├── hooks/               useAuth · useSite · useAsync
│   └── lib/                 format ngày · tiết học · điểm
│
├── features/
│   ├── auth/                đăng nhập · giữ JWT
│   ├── lich-hoc/            thời khoá biểu hợp nhất
│   ├── dang-ky/             tìm lớp · đăng ký · trạng thái chỗ trống
│   ├── lien-co-so/          duyệt lớp site khác · gửi yêu cầu · theo dõi CHO_DUYET
│   ├── bang-diem/           điểm local + BangDiemMirror, có nhãn LastSyncedAt
│   ├── nhap-diem/           màn hình giảng viên
│   ├── danh-muc/            admin Master
│   ├── bao-cao/             thống kê toàn hệ thống
│   └── xray/                ⭐ panel + phòng điều khiển + so sánh chiến lược
│
└── styles/                  design token · theme sáng/tối
```

### Mẫu và quy ước

| Mẫu | Ở đâu |
|---|---|
| **Feature-Sliced Design** | toàn bộ `src/` |
| **Design tokens** | `styles/` — màu, khoảng cách, chữ khai báo một chỗ |
| **API client là adapter duy nhất** | `shared/api` — không component nào gọi `fetch` trực tiếp |
| **Overlay xuyên suốt** | `features/xray` đọc `_xray` từ **mọi** response, không component khác cần biết |
| **Error boundary theo feature** | một feature hỏng không làm trắng cả trang |

**Về UI/UX:** đây là phần được đầu tư, không phải làm cho có. Dùng skill đã cài (`ui-ux-pro-max`, `taste-skill`) thay vì tự chế lại design system. Thiết kế và dựng màn hình tĩnh **bắt đầu được từ tuần 1** — không phụ thuộc schema hay API.

---

## J3. CSDL — `db/`

### Nguyên tắc: script phải **chạy lại được**

Dựng lại toàn bộ môi trường từ số 0 chỉ bằng cách chạy lại thư mục này. Điều đó **sẽ cần đến** ở tuần 3.

- `CREATE OR ALTER PROCEDURE` · `IF NOT EXISTS` trước mỗi `CREATE TABLE`
- Không script nào phụ thuộc trạng thái do script khác để lại, ngoài thứ tự số
- Mọi thay đổi schema đi qua **Flyway**, không sửa tay trên CSDL

### Cây thư mục và nơi chạy

```
db/
├── 00-create-databases.sql       → chạy MỘT LẦN trên mỗi máy chủ
│
├── master/                       → CHỈ chạy trên PTITONE_MASTER
│   ├── 01-schema-thamchieu.sql       7 bảng + CoSo(TenLinkedServer, TenDatabase)
│   ├── 02-danhba-nguoidung.sql       danh bạ định vị
│   ├── 03-taikhoan-master.sql        tài khoản Admin Master
│   └── 04-seed-danhmuc.sql
│
├── site/                         → chạy trên MỌI PTITONE_<site>
│   ├── 10-schema-vanhanh.sql
│   ├── 11-rangbuoc.sql               UNIQUE · CHECK sức chứa · CHECK tín chỉ
│   ├── 12-chimuc.sql                 theo kế hoạch index, KHÔNG rải thêm
│   ├── 13-trigger.sql                ⚠️ NOT FOR REPLICATION
│   ├── 14-role-grant.sql             4 role + DENY trên bảng nhân bản
│   └── 15-thutuc-dangky.sql          UPDATE có điều kiện + @@ROWCOUNT
│
├── crosssite/                    → CHỈ chạy trên máy giữ Linked Server
│   ├── 20-linked-server.sql          2 ánh xạ login: ro (báo cáo) · rw (chuyển cơ sở)
│   ├── 21-sp-chuyen-co-so.sql        ⭐ BEGIN DISTRIBUTED TRANSACTION
│   └── 22-baocao-tonghop.sql         4 báo cáo OPENQUERY
│
├── replication/                  → chạy trên Publisher / Subscriber
│   ├── 30-distributor.sql            retention CẢ HAI = 720 giờ
│   ├── 31-publication.sql
│   ├── 32-subscription.sql           cục bộ TRƯỚC, rồi mới qua VPN
│   └── README.md                     các bước wizard + đối chiếu screenshot
│
└── seed/
    └── 90-sinh-du-lieu.sql           ~5.000 SV · ~50.000 đăng ký mỗi site
```

### Ma trận: script nào chạy ở đâu

| Thư mục | `PTITONE_MASTER` | `PTITONE_HCM` | `PTITONE_HN` | `PTITONE_DN` |
|---|:---:|:---:|:---:|:---:|
| `master/` | ✅ | — | — | — |
| `site/` | — | ✅ | ✅ | ✅ |
| `crosssite/` | — | ✅ *(nút báo cáo)* | — | — |
| `replication/` — publication | ✅ | — | — | — |
| `replication/` — subscription | — | ✅ | ✅ | ✅ |
| `seed/` | — | ✅ | ✅ | ✅ |

> ⚠️ Chạy nhầm `site/` lên `PTITONE_MASTER` sẽ tạo bảng vận hành trong database Master — phá vỡ ranh giới ở **C0**. Mỗi script mở đầu bằng một câu kiểm tra `DB_NAME()` và `THROW` nếu chạy sai chỗ.

### Quy ước đặt tên

| Đối tượng | Quy ước | Ví dụ |
|---|---|---|
| Bảng, cột | Tiếng Việt không dấu, PascalCase | `DangKyHocPhan`, `SoLuongDaDangKy` |
| Read model | Hậu tố `Mirror` | `BangDiemMirror` |
| Khoá chính | `PK_<Bang>` | `PK_DangKyHocPhan` |
| Khoá ngoại | `FK_<Bang>_<BangCha>` | `FK_Diem_DangKyHocPhan` |
| Duy nhất | `UQ_<Bang>_<Cot>` | `UQ_DangKyHocPhan_MaYeuCau` |
| Kiểm tra | `CK_<Bang>_<YNghia>` | `CK_LopHocPhan_SucChua` |
| Chỉ mục | `IX_<Bang>_<Cot>` | `IX_LopHocPhan_HocKy_MonHoc` |
| Thủ tục | `sp_<DongTu><DoiTuong>` | `sp_ChuyenCoSoSinhVien` |
| Trigger | `trg_<HanhVi>` | `trg_ChanGhiBangNhanBan` |

---

## J4. Thứ tự dựng — ai làm được gì song song

```
TUẦN 1–2   db/master + db/site + db/seed             ← hạ tầng CSDL 2
           apps/web: design system + màn hình tĩnh   ← ứng dụng (KHÔNG chờ API)

TUẦN 3     db/replication + db/crosssite             ← hạ tầng CSDL 1
TUẦN 4     13-trigger · 14-role · 15-thutuc · 21-sp-chuyen-co-so
           ✅ hết tuần 4: NĂM YÊU CẦU BẮT BUỘC XONG

TUẦN 5     apps/api: shared/site · domain · 3 port · persistence
TUẦN 6     application/dangky (saga) · shared/outbox
TUẦN 7     shared/xray · features/xray · bench/
```

⭐ Frontend **không nằm trên đường găng**. Design system và màn hình tĩnh dựng được ngay từ tuần 1, chỉ nối API ở tuần 5. Đây là chỗ song song hoá tốt nhất trong cả dự án — và là lý do UI/UX được đầu tư mà không ảnh hưởng lịch.

---

# PHẦN I — PHỤ LỤC

## I1. Cấu trúc repository

```
PTIT-One/
├── docs/
│   ├── PTIT-One-Thiet-Ke.md      ← tài liệu này
│   ├── bao-cao/                       ← bản Word nộp thầy
│   ├── diagrams/                      ← ERD, lược đồ phân mảnh/ánh xạ/định vị
│   └── screenshots/
│       ├── 01-vpn/          02-sqlserver/    03-agent/
│       ├── 04-linkedserver/ 05-publication/  06-trigger-role/
│       └── 07-demo-3.7/
├── db/
│   ├── 00-create-databases.sql     ← PTITONE_MASTER · PTITONE_HCM · PTITONE_HN · PTITONE_DN
│   ├── 01-schema-master.sql        ← 7 bảng tham chiếu, chỉ trong PTITONE_MASTER
│   ├── 02-schema-site.sql          ← mảnh vận hành, chạy trên MỌI CSDL site
│   ├── 03-roles-grants.sql         04-triggers.sql
│   ├── 05-linked-server.sql        06-replication/
│   └── 99-seed/
├── apps/
│   ├── api/    vn/ptit/one/{domain,application,infrastructure,interfaces}
│   └── web/    React + Vite
└── bench/      sinh tải + kịch bản benchmark
```

Nhánh: `main` (ổn định) · `dev` · `feat/*`. Mỗi tuần một tag `week-1` … `week-8` để lịch sử commit thể hiện được tiến độ.

## I2. Kế hoạch dự phòng cho replication

Nếu spike tuần 1 không PASS: **hai (hoặc ba) named instance trên cùng một máy Windows** — `PC\SITE_HCM`, `PC\SITE_HN`, `PC\SITE_DN`.

Phương án này **giữ nguyên 100% giá trị học thuật**: vẫn là các SQL Server độc lập thật sự, vẫn có Publisher/Subscriber thật, Linked Server thật, phân mảnh thật, và execution plan vẫn hiện toán tử `Remote Query`. Thứ duy nhất mất đi là **độ trễ mạng thật giữa các tỉnh** — nghĩa là benchmark cross-site sẽ cho số quá đẹp, và phải nói rõ điều đó trong báo cáo (có thể mô phỏng độ trễ bằng công cụ như `clumsy` để số liệu có ý nghĩa).

Tài liệu thiết kế **không phải sửa một chữ nào**, vì nó vốn viết cho N cơ sở.

## I2b. Vận hành máy chủ — hệ thống **không** cần chạy 24/7

> ⚠️ Bản trước của tài liệu này ghi "máy chạy ổn định 24/7", và đó là **yêu cầu sai**. Laptop sinh viên không thể chạy liên tục 8 tuần, mà hệ thống cũng không đòi hỏi điều đó.

### Yêu cầu thật chỉ có hai điều

```
1. Cả nhóm online CÙNG LÚC trong các buổi làm việc đã hẹn
   (dựng replication, test, đo benchmark, tổng duyệt demo)

2. Mỗi máy online ÍT NHẤT MỘT LẦN trong mỗi cửa sổ retention
   (để subscription không hết hạn)
```

Ngoài hai lúc đó, máy tắt hoàn toàn cũng không sao.

### Vì sao replication chịu được máy tắt

Transactional Replication được thiết kế cho **kết nối không liên tục**. Khi Subscriber offline, lệnh dồn lại trong distribution database và chảy về khi máy bật lại — không mất dữ liệu, không cần can thiệp. Ràng buộc duy nhất là retention, chính là thứ F6c đã chỉnh:

| | Mặc định | Đặt thành | Nghĩa là |
|---|---|---|---|
| Distribution retention (`@max_distretention`) | 72 giờ | **720 giờ = 30 ngày** | Lệnh được **giữ** trong distribution database 30 ngày |
| Subscription expiration (`@retention`) | 336 giờ | **720 giờ = 30 ngày** | Subscription **hết hạn** sau 30 ngày không đồng bộ |

> ⚠️ **Hai tham số này phải bằng nhau — nếu không, con số nhỏ hơn mới là giới hạn thật.**
>
> **Thời gian tắt máy tối đa = min(distribution retention, subscription expiration).**
>
> Bản trước của tài liệu đặt distribution retention 14 ngày nhưng lại tuyên bố "tắt 30 ngày vẫn bắt kịp" — **sai**. Nếu subscriber offline quá 14 ngày thì lệnh đã bị dọn khỏi distribution database, và dù subscription *chưa* hết hạn, nó vẫn **không còn gì để bắt kịp** → buộc phải khởi tạo lại snapshot.

Với 7 bảng tham chiếu nhỏ và ~15 lượt ghi/ngày, giữ 30 ngày tốn không đáng kể dung lượng.

### Bốn phương án, xếp theo khuyến nghị

**1. Lịch buổi làm việc cố định** ⭐

Hẹn **2–3 buổi/tuần**, ai ở đâu cũng được, chỉ cần bật máy và vào VPN. Ngoài giờ đó tắt máy thoải mái. Tuần 3 cần dày hơn (dựng replication), các tuần khác thưa hơn. Chi phí: 0đ, chỉ cần thống nhất lịch.

**2. Giảm số máy** — hiệu quả nhất về mặt xác suất

Nếu mỗi máy có ~70% khả năng bật được đúng hẹn:

| Số máy | Xác suất đủ mặt |
|---|---|
| 2 | ~49% |
| 3 | ~34% |
| 4 | ~24% |

**Phương án lai đáng cân nhắc: 2 máy thật + site thứ 3 là named instance trên một trong hai máy đó.**

```
Máy A  ──VPN──  Máy B
├─ PTITONE_MASTER          └─ PTITONE_HN
├─ PTITONE_HCM
└─ PTITONE_DN  (named instance trên chính máy A)
```

Được cả hai: **replication qua VPN thật giữa hai máy** (phần khó và được chấm điểm) **và** đủ 3 site cho thiết kế N-site. Chỉ cần 2 người bật máy thay vì 3–4.

**3. Ưu tiên máy để bàn.** Ai có PC để bàn ở nhà thì đó là ứng viên số một cho `SRV-MASTER` — không bị mang đi lại, cắm điện liên tục, không sập nguồn khi hết pin. Một máy bàn trong nhóm giải quyết gần hết vấn đề này.

**4. Plan B toàn phần** (I2) nếu vẫn không ổn.

### Cấu hình Windows để máy không tự ngủ hoặc tự khởi động lại

PowerShell **quyền admin**, chạy trên mỗi máy chủ:

```powershell
powercfg /change standby-timeout-ac 0      # không ngủ khi cắm điện
powercfg /change hibernate-timeout-ac 0    # không ngủ đông
powercfg /change monitor-timeout-ac 10     # màn hình tắt nhưng MÁY VẪN CHẠY
powercfg /hibernate off                     # tắt hibernate + fast startup

# Đóng nắp laptop mà máy vẫn chạy:
powercfg -setacvalueindex SCHEME_CURRENT SUB_BUTTONS LIDACTION 0
powercfg -setactive SCHEME_CURRENT
```

Hai việc nữa, quan trọng không kém:

- ⚠️ **Tạm dừng Windows Update 5 tuần** (Settings → Windows Update → Pause). Update tự khởi động lại giữa buổi test là chuyện rất hay xảy ra.
- **Đặt SQL Server và SQL Server Agent là `Automatic`** để máy bật lên là dịch vụ tự chạy, không ai phải nhớ bật tay.

### Yêu cầu phần cứng

| Hạng mục | Yêu cầu |
|---|---|
| RAM | **8GB tổng** là thoải mái (SQL Server Developer dùng thực ~2–4GB) |
| Đĩa trống | **20GB** — dư nhiều. Toàn bộ CSDL kể cả dữ liệu seed chỉ cỡ vài trăm MB |
| Hệ điều hành | Windows 10/11 bất kỳ |

Bất kỳ laptop nào 5–6 năm đổ lại đều chạy được. **Ràng buộc thật không phải cấu hình, mà là có người giữ máy và bật được đúng buổi hẹn.**

### Chi phí hạ tầng: 0đ

Không thuê gì, và cũng **không thuê được**: đề bài cần quyền `sysadmin` để cấu hình Distributor, cần **SQL Server Agent**, và cần **snapshot folder là UNC share Windows** — không dịch vụ CSDL quản lý (PaaS) nào cho đủ ba thứ đó. Thuê máy ảo đám mây thì được về kỹ thuật nhưng không có bậc miễn phí nào chạy nổi Windows + SQL Server (cần ~4GB RAM), lại thêm rủi ro vào đúng tuần 3 và lệch với tài liệu của giảng viên.

---

## I3. Bảng rủi ro

| Rủi ro | Xác suất | Tác động | Giảm thiểu |
|---|---|---|---|
| Replication không chạy qua VPN | Cao | Rất cao | Spike tuần 1 · Plan B I2 · checklist F |
| ⭐ **MS DTC không chạy qua VPN** → mất **yêu cầu 3** của giảng viên | **Cao** | **Rất cao** | Đưa vào spike tuần 1 (F4b) · mở dải RPC động 49152–65535 · `No Authentication Required` cho workgroup · Dự phòng: 2PC giữa hai named instance cùng máy |
| Snapshot folder để mặc định | Rất cao | Cao | F6 — dùng UNC share ngay từ đầu |
| Agent không xác thực ra share | Cao | Cao | F4 — tài khoản Windows trùng tên/mật khẩu mọi máy |
| Máy chủ site không bật được đúng buổi hẹn | Cao | Trung bình | Lịch buổi làm việc cố định (I2b) · giảm số máy · ưu tiên máy để bàn · backup `.bak` hằng tuần |
| ⚠️ **Subscription hết hạn** vì một site tắt quá retention (mặc định **72 giờ**) → phải khởi tạo lại snapshot | Trung bình | Cao | **Đặt CẢ HAI retention = 720 giờ** ngay lúc cấu hình Distributor (F6c) · giới hạn thật là **min** của hai tham số · giám sát bằng Replication Monitor hằng tuần |
| Thêm máy thứ tư (D15) → xác suất đủ mặt giảm | Trung bình | Trung bình | Chỉ chọn 4 máy khi có người sẵn sàng giữ máy thứ tư (lý tưởng: máy để bàn) |
| Screenshot thiếu, phải dựng lại | Cao | Cao | Chụp ngay khi làm, từ tuần 1 |
| Tài liệu dồn vào tuần cuối | Cao | Rất cao | Một người làm tài liệu toàn thời gian |
| Phần ➕ lấn vào thời gian phần bắt buộc | Trung bình | Rất cao | Cổng chặn cuối tuần 4 |
| Đề tài Excel khác giả định | Thấp | Trung bình | Khung thiết kế giữ nguyên, chỉ đổi tên thực thể |

## I4. Việc còn treo

- [ ] **File Excel phân công đề tài** — khóa tên thực thể trong ERD và 4 lược đồ
- [ ] **Tài liệu hướng dẫn Replication của giảng viên** — quyết cách làm F6 và xác nhận D9
- [ ] **Xác nhận trọng số bonus của phần mềm** với giảng viên
- [ ] **Số liệu quy mô thật** thay cho giả định 🔶 ở A4/B2
- [ ] **Chốt D12 và D10** — cuối tuần 1, sau spike
- [ ] **Chốt D15** — tuần 3, sau khi biết chắc **mấy máy có người giữ và bật được đúng buổi hẹn**
- [ ] Chốt người giữ máy chủ cho từng site (tiêu chí cho `SRV-MASTER`: **ít bị mang đi lại nhất, ưu tiên máy để bàn + đĩa khá**, không phải máy yếu nhất)
- [ ] **Chốt lịch buổi làm việc cố định** — 2–3 buổi/tuần, cả nhóm cùng online (I2b)
- [ ] ⭐ **Xác nhận năm yêu cầu bắt buộc với giảng viên** (mục 0.1b) — nhất là yêu cầu 3, để chắc `sp_ChuyenCoSoSinhVien` là hiện thực được chấp nhận

## I5. Checklist thi công — bản tick nhanh

> Bản rút gọn của Phần F để in ra hoặc tick trực tiếp. Chi tiết đầy đủ và giải thích xem Phần F.
> ⚠️ **Chụp màn hình từng bước, ngay khi làm.** Lưu vào `docs/screenshots/<số>-<tên bước>/`.

| Mục | Bước | Cái bẫy cần nhớ | Xong | Đã chụp |
|---|---|---|---|---|
| F1 | Cài đặt VPN (Radmin) | Thêm entry vào `hosts` trên **mọi máy**: IP VPN → tên máy. Replication lưu **tên máy**, không lưu IP | ☐ | ☐ |
| F2 | Tạo đường link giữa các server | `ping <tên máy>` và `telnet <tên máy> 1433` hai chiều **trước** khi đi tiếp | ☐ | ☐ |
| F3 | Cài SQL Server Developer Edition | Bắt buộc Developer. Mở TCP 1433 inbound. Collation `Vietnamese_CI_AS` | ☐ | ☐ |
| F4 | Kiểm tra SQL Server Agent | ⚠️ **Cái bẫy nặng nhất:** `NT Service\SQLSERVERAGENT` không xác thực được ra share máy khác trong workgroup. Tạo tài khoản Windows **trùng username + trùng password trên mọi máy** | ☐ | ☐ |
| **F4b** | ⭐ **Cấu hình MS DTC** | **Bắt buộc cho yêu cầu 3.** Mở TCP 135 + dải RPC động 49152–65535 · bật Network DTC Access / Allow Inbound / Allow Outbound · ⚠️ **`No Authentication Required`** vì là workgroup · khởi động lại dịch vụ DTC | ☐ | ☐ |
| F5 | Tạo Linked Server | Chặn ánh xạ mặc định **trước**, rồi chỉ mở cho login chạy báo cáo. Tài khoản đầu xa chỉ cần `SELECT` | ☐ | ☐ |
| F6a | Publication trên `PTITONE_MASTER` | Publisher là `PTITONE_MASTER`, **không phải** `PTITONE_HCM`. Distributor local | ☐ | ☐ |
| F6b | Snapshot folder | ⚠️ **Lỗi số một giết các nhóm:** bắt buộc **UNC share**, không để mặc định `C:\Program Files\…\ReplData`. Share nằm trên máy chạy **Distributor** | ☐ | ☐ |
| F6c | Retention (**cả hai, BẰNG NHAU**) | `@max_distretention` 72h → **720h**. `@retention` 336h → **720h**. ⚠️ Giới hạn tắt máy = **min** của hai số | ☐ | ☐ |
| F6d | Subscription cục bộ (`PTITONE_HCM`) | **Làm trước** — cùng instance, không qua VPN. Tách *"publication có đúng không"* khỏi *"mạng có thông không"* | ☐ | ☐ |
| F6e | Subscription qua VPN (`PTITONE_HN`, `PTITONE_DN`) | Push subscription. Kiểm tra bằng Replication Monitor | ☐ | ☐ |
| F7a | Nhập dữ liệu | Thêm SV · Mở lớp · Đăng ký · Nhập điểm. Chụp trước/sau | ☐ | ☐ |
| F7b-1 | Kiểm chứng **phân mảnh ngang** | `SELECT COUNT(*) FROM SinhVien` tại từng site — số khác nhau, tổng = toàn trường, không trùng | ☐ | ☐ |
| F7b-2 | Kiểm chứng **dẫn xuất** | `SELECT * FROM DangKyHocPhan` tại HN — chỉ chứa đăng ký của lớp mở tại HN | ☐ | ☐ |
| F7b-3 | Kiểm chứng **nhân bản** | Thêm môn tại Master, đợi vài giây, `SELECT` tại HN và ĐN | ☐ | ☐ |
| F7b-4 | Kiểm chứng **bảo vệ Subscriber** | `INSERT INTO MonHoc` tại HN → **bị từ chối**. Chụp lỗi | ☐ | ☐ |
| F7b-5 | Kiểm chứng **trigger chéo site** | `INSERT SinhVien` với `MaCoSoNha='HN'` vào CSDL HCM → **bị từ chối**. Chụp lỗi | ☐ | ☐ |
| F7b-6 | Kiểm chứng **database role** | Đăng nhập `sv_test`, thử `UPDATE Diem` → **bị từ chối**. Chụp lỗi | ☐ | ☐ |
| F7b-7 | Kiểm chứng **Linked Server** | `SELECT * FROM OPENQUERY(SRV_HN, '…')` từ HCM | ☐ | ☐ |
| F7c | Thống kê | Bốn báo cáo (D2), chạy trong ngữ cảnh `PTITONE_HCM` | ☐ | ☐ |
| F7d | Thử transaction | Test 100 luồng tranh 30 chỗ · Truy vấn đối soát bất biến (phải trả **0 dòng**) · Demo compensating transaction | ☐ | ☐ |
| **F7e-1** | ⭐ **Giao dịch phân tán — chạy thành công** | `EXEC sp_ChuyenCoSoSinhVien` · kiểm tra 3 CSDL: HCM không còn · HN đã có · danh bạ đã đổi | ☐ | ☐ |
| **F7e-2** | ⭐ **Giao dịch phân tán — demo ROLLBACK** | **Tắt SQL Server ở site đích** rồi chạy lại → giao dịch thất bại, sinh viên vẫn **nguyên vẹn** ở site cũ, danh bạ **không** đổi | ☐ | ☐ |

---

## I6. Danh sách bảng — tra nhanh

`[R]` nhân bản · `[F]` phân mảnh · `[P]` read model (projection — **không phải** mảnh)

| Nhóm | Bảng | Loại | Chủ sở hữu | Vị từ phân mảnh / Ghi chú | Chiến lược khóa |
|---|---|---|---|---|---|
| Tham chiếu | `CoSo` | `[R]` | `PTITONE_MASTER` | Nhân bản toàn phần | Mã nghiệp vụ toàn cục |
| Tham chiếu | `Khoa` | `[R]` | `PTITONE_MASTER` | Nhân bản toàn phần | Mã nghiệp vụ toàn cục |
| Tham chiếu | `ChuongTrinhDaoTao` | `[R]` | `PTITONE_MASTER` | Nhân bản toàn phần | Mã nghiệp vụ toàn cục |
| Tham chiếu | `MonHoc` | `[R]` | `PTITONE_MASTER` | Nhân bản toàn phần — bảng bị đọc nhiều nhất | Mã nghiệp vụ toàn cục |
| Tham chiếu | `MonHocTienQuyet` | `[R]` | `PTITONE_MASTER` | Nhân bản toàn phần | Khóa kép |
| Tham chiếu | `HocKy` | `[R]` | `PTITONE_MASTER` | Chỉ lịch chung toàn trường | Mã nghiệp vụ toàn cục |
| Tham chiếu | `DanhBaNguoiDung` | `[R]` | `PTITONE_MASTER` | **Danh bạ định vị** — nền tảng của Location Transparency | `MaSinhVien` |
| Phân mảnh | `SinhVien` | `[F]` | Cơ sở nhà | `MaCoSoNha = <site>` | Mã toàn trường — **không** tiền tố cơ sở |
| Phân mảnh | `GiangVien` | `[F]` | Cơ sở | `MaCoSo = <site>` | Mã nghiệp vụ toàn trường |
| Phân mảnh | `TaiKhoan` | `[F]` | Cơ sở | `MaCoSo = <site>` | `TenDangNhap` |
| Phân mảnh | `DotDangKy` | `[F]` | Cơ sở | `MaCoSo = <site>` · ⚠️ **không nhân bản** | `<MaCoSo>-<MaHocKy>-<STT>` |
| Phân mảnh | `LopHocPhan` | `[F]` | **Host** | `MaCoSoHost = <site>` | `<MaMonHoc>-<MaHocKy>-<MaCoSo><STT>` |
| Dẫn xuất | `DangKyHocPhan` | `[F]` | **Host** | **Bậc 1:** `⋉ LopHocPhan` — *không phải* `⋉ SinhVien` | Khóa kép (`MaLopHP`, `MaSinhVien`) |
| Dẫn xuất | `Diem` | `[F]` | **Host** | **Bậc 2:** `Diem ⋉ DangKyHocPhan ⋉ LopHocPhan` | Khóa kép (`MaLopHP`, `MaSinhVien`) |
| Liên cơ sở ➕ | `YeuCauHocLienCoSo` | `[F]` | Cơ sở nhà | Trạng thái saga + snapshot lớp | `MaYeuCau` (`uniqueidentifier`) |
| Liên cơ sở ➕ | `KetQuaXuLyYeuCau` | `[F]` | **Host** | Inbox/outcome — lưu **cả kết quả từ chối** | `MaYeuCau` |
| Liên cơ sở ➕ | `BangDiemMirror` | `[P]` | Cơ sở nhà (worker ghi) | ⚠️ **Không phải nguồn sự thật** — nguồn ở Host | Khóa kép + `Version` |
| Liên cơ sở ➕ | `OutboxSuKien` | `[F]` | Site phát sinh | Hàng đợi phát sự kiện | `EventId` (`uniqueidentifier`) |

> **Quy tắc khóa:** nhúng mã cơ sở vào khóa **chỉ khi cơ sở là một phần ngữ nghĩa** của thực thể. Lớp học phần thuộc về một cơ sở → đúng. Sinh viên thì không, vì sinh viên có thể chuyển cơ sở. **Không dùng `IDENTITY` ở bất kỳ đâu.**

---

## I7. Nguồn tham khảo

- [Configure Publishing and Distribution — SQL Server | Microsoft Learn](https://learn.microsoft.com/en-us/sql/relational-databases/replication/configure-publishing-and-distribution?view=sql-server-ver16)
- [Tutorial: Prepare for replication — SQL Server | Microsoft Learn](https://learn.microsoft.com/en-us/sql/relational-databases/replication/tutorial-preparing-the-server-for-replication?view=sql-server-ver16)
- [Tutorial: Configure Transactional Replication | Microsoft Learn](https://learn.microsoft.com/en-us/sql/relational-databases/replication/tutorial-replicating-data-between-continuously-connected-servers?view=sql-server-ver16)
- [OPENQUERY (Transact-SQL) | Microsoft Learn](https://learn.microsoft.com/en-us/sql/t-sql/functions/openquery-transact-sql?view=sql-server-ver17)
- [Query remote servers — OPENQUERY, OPENROWSET, EXEC AT | Microsoft Learn](https://learn.microsoft.com/en-us/sql/relational-databases/linked-servers/linked-servers-openquery-openrowset-exec-at?view=sql-server-ver17)
- [Cross Campus Enrollment | UMSL](https://www.umsl.edu/registration/policies-and-procedures/cross-campus-enrollment.html)
- [Cross Enrollment | Office of the Registrar, University of Missouri](https://registrar.missouri.edu/registration-classes/registration/cross-enrollment-mizzou/)
- [Cross Campus Enrollment | UMKC Office of the Registrar](https://www.umkc.edu/registrar/registration/cross-campus-enrollment.html)
