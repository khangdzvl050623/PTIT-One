# PTIT One — Trả lời ba câu hỏi nghiệm thu

**Dùng cho:** buổi báo cáo với giảng viên. Số liệu tính đến **19/10/2026**.

---

## 1. Phần 2 làm gì

Tách database tập trung của Phần 1 thành **4 nút**, rồi chạy lại toàn bộ
nghiệp vụ trên đó.

```
SRV-MASTER          SRV-HCM        SRV-HN         SRV-DN
9 bảng danh mục  →  mảnh HCM       mảnh HN        mảnh ĐN
(Publisher)         (Subscriber)   (Subscriber)   (Subscriber)
```

**Năm yêu cầu bắt buộc:**

| # | Yêu cầu | Làm gì cụ thể | Trạng thái |
|---|---|---|---|
| 1 | Phân mảnh ngang | Cắt dữ liệu theo cơ sở. `DangKyHocPhan` đi theo **lớp**, `Diem` dẫn xuất bậc 2 | Schema xong, chưa triển khai |
| 2 | Nhân bản một chiều | Master → 3 site, site chỉ đọc | ✅ **Đã chạy**, trễ 4 giây |
| 3 | Giao dịch phân tán | `sp_ChuyenCoSoSinhVien` dùng 2PC qua MS DTC | Chưa bắt đầu |
| 4 | Tương tranh | 100 luồng tranh 30 chỗ | ✅ **Xong từ Phần 1**, cần chạy lại trên bản phân tán |
| 5 | Truy vấn phân tán | 4 báo cáo toàn trường qua `OPENQUERY` | Chưa bắt đầu |

**Hai cơ chế thêm, không nằm trong 5 yêu cầu nhưng bắt buộc để hệ chạy được:**

- **Saga** — đăng ký lớp ở cơ sở khác. Ghi hai site, có người đang chờ.
- **Outbox** — đồng bộ điểm về cơ sở nhà. Ghi hai site, không ai chờ.

---

## 2. Tại sao thiết kế như vậy

### Vì sao phải phân tán — và không, không phải để chạy nhanh hơn

Phân tán **làm chậm đi** là chính. 2PC giữ khoá suốt thời gian đi mạng. Saga
cần ba giao dịch thay vì một. Truy vấn toàn trường phải gom từ ba máy.

Không ai trả cái giá đó để đổi lấy tốc độ. Ba lý do thật:

**Một — tự chủ.** Mỗi cơ sở sở hữu và tự quản dữ liệu của mình. HN quyết lớp
của HN còn chỗ hay không, bằng dữ liệu của HN. Không ai quyết hộ.

**Hai — cô lập sự cố.** Máy HN chết thì HCM và ĐN vẫn đăng ký bình thường.
Gom một chỗ thì một máy chết là cả ba cơ sở dừng.

**Ba — ranh giới dữ liệu trùng ranh giới tổ chức.** Sinh viên thuộc một cơ sở,
lớp mở tại một cơ sở. Ước lượng **98% thao tác chỉ chạm một mảnh**. Đó đúng là
điều kiện sách giáo khoa nói phân mảnh có lãi.

> Nếu 50% nghiệp vụ phải nối hai cơ sở thì phân tán sẽ **lỗ**, và câu trả lời
> đúng là giữ một database tập trung.

Tốc độ chỉ là hiệu ứng phụ của việc đặt dữ liệu đúng chỗ: 98% truy vấn đọc cục
bộ, không qua mạng. Đó là **locality**, không phải "truyền nhanh hơn".

### Ba quyết định chính và lý do

**Phân mảnh ngang, không dọc.** Cắt dọc là cắt cột — không giải bài toán địa
lý. Tách `SinhVien` thành hai nửa đặt hai máy thì mỗi lần đọc hồ sơ thành một
phép nối qua mạng, cho mọi sinh viên. Cắt ngang theo cơ sở thì hồ sơ nằm trọn
ở máy của cơ sở đó.

**Danh mục dùng single-master.** Môn học sửa ~15 lần/ngày, đọc gấp ~5.300 lần.
Một nơi ghi, nhân bản ra mọi nơi. Cho ghi hai chiều chỉ mua thêm bài toán giải
quyết xung đột mà không đổi lấy gì. Đây là hình dạng chuẩn: PostgreSQL,
MongoDB replica set, Kafka partition, etcd đều vậy.

**Saga cho đăng ký, 2PC chỉ cho chuyển cơ sở.** Đăng ký là đường nóng, nhiều
người tranh một dòng lớp — 2PC ở đó giữ khoá xuyên mạng, gây nghẽn. Chuyển cơ
sở thì hiếm, và không có trạng thái "nửa chuyển" nào an toàn, nên chịu được
chi phí 2PC. Nhóm **cài cả hai** rồi đo đối chứng, không phỏng đoán.

---

## 3. Nghiệm thu và chi phí

### Nghiệm thu — mỗi yêu cầu một bằng chứng kiểm được

| # | PASS khi |
|---|---|
| 1 | Tổng số dòng từng bảng ở 3 site **cộng lại bằng** số dòng ở `CENTRAL` |
| 2 | Tracer token từ Master tới cả 3 site **≤ 10 giây** |
| 3 | Demo rollback: tắt máy đích giữa giao dịch → **cả hai đầu cùng huỷ** |
| 4 | 100 luồng / 30 chỗ → đúng 30 người vào, 70 bị từ chối |
| 5 | `OPENQUERY` và truy vấn four-part cho **cùng kết quả**, khác số dòng qua mạng |

Yêu cầu #2 **đã đạt** ở mức một site: đo được 4 giây (distributor 3 + subscriber 1).

### Chi phí — còn 12 ngày

| Việc | Công sức | Phụ thuộc |
|---|---|---|
| Spike MS DTC | 0,5 ngày | Cần instance hoặc máy thứ hai |
| Triển khai schema 3 site + tách dữ liệu | 2 ngày | 3 máy lên VPN |
| Nhân bản ra 3 site thật qua VPN | 2 ngày | VPN thông |
| #3 `sp_ChuyenCoSoSinhVien` + demo rollback | 2 ngày | DTC thông |
| #5 bốn báo cáo `OPENQUERY` | 1,5 ngày | Linked Server |
| #4 chạy lại trên bản phân tán | 0,5 ngày | Ứng dụng nối 3 site |
| Định tuyến DataSource ở `apps/api` | 3 ngày | Schema xong |
| Báo cáo + ảnh chụp | 3 ngày | Chạy xuyên suốt |
| **Tổng** | **~14,5 người-ngày** | |

Nhóm 5 người × 12 ngày = **60 người-ngày khả dụng**. Khối lượng vừa sức, với
điều kiện làm song song và VPN không vỡ.

**Đường găng:** VPN → nhân bản 3 site → 2PC → báo cáo. Bốn việc này nối tiếp,
không rút ngắn bằng cách thêm người được.

**Mốc đặt ra:** năm yêu cầu bắt buộc xong **26/10**, báo cáo đầy đủ **30/10**.

### Rủi ro lớn nhất

**Yêu cầu #3** phụ thuộc MS DTC qua VPN và hiện **chưa có một dòng code nào**.
Đây là mục duy nhất trong năm mục chưa có đường lui. Nếu DTC không thông qua
mạng, phương án dự phòng là chạy 2PC giữa **hai instance trên cùng một máy** —
vẫn là giao dịch phân tán thật, và nhóm sẽ ghi rõ giới hạn này trong báo cáo.
