# PTIT One — Trả lời nghiệm thu Phần 2

**Dùng cho:** buổi báo cáo với giảng viên. Số liệu đo được tính đến **24/10/2026**.

Ba mục đầu trả lời ba câu hỏi thầy đã nêu. Mục 4 là mười câu hỏi dễ bị hỏi
thêm, trả lời ngắn — đọc trước để không phải nghĩ tại chỗ.

---

## 1. Phần 2 làm gì

Tách database tập trung của Phần 1 thành **4 nút**, rồi chạy lại toàn bộ
nghiệp vụ trên đó.

```
SRV-MASTER          SRV-HCM        SRV-HN         SRV-DN
9 bảng danh mục  →  mảnh HCM       mảnh HN        mảnh ĐN
(Publisher)         (Subscriber)   (Subscriber)   (Subscriber)
```

**Năm yêu cầu bắt buộc — tất cả đã chạy và đo được:**

| # | Yêu cầu | Làm gì cụ thể | Số đo |
|---|---|---|---|
| 1 | Phân mảnh ngang | Cắt theo cơ sở. `DangKyHocPhan` đi theo **lớp**, `Diem` dẫn xuất bậc 2 | ✅ đối soát **10/10 bảng** khớp số dòng |
| 2 | Nhân bản một chiều | Master → site, site chỉ đọc | ✅ tracer token, trễ **7 giây** |
| 3 | Giao dịch phân tán | `sp_ChuyenCoSoSinhVien` qua MS DTC | ✅ demo **COMMIT + ROLLBACK** cưỡng bức |
| 4 | Tương tranh | 100 luồng tranh 30 chỗ | ✅ đúng **30** vào, 70 trượt vì `CLASS_FULL` |
| 5 | Truy vấn phân tán | Báo cáo toàn trường qua Linked Server | ✅ pushdown **1 dòng/33 ms** so với kéo thô **50.000 dòng/118 ms** |

Chạy lại toàn bộ bằng **một lệnh**:

```
powershell -NoProfile -File db\tests\Chay-Bang-Chung.ps1
```

Mỗi mục tự in `PASS`/`FAIL` của chính nó; script chỉ gom output vào thư mục
`bang-chung\<ngày-giờ>\`. Không mục nào "đạt" vì script tự nhận xét.

**Hai cơ chế thêm**, không nằm trong 5 yêu cầu (tài liệu thiết kế đánh dấu ➕):

- **Saga** — đăng ký lớp ở cơ sở khác. Ghi hai site, **có người đang chờ**.
  Hiện mới có nền móng: model, hai repository, phép so trùng giờ. Chưa nối.
- **Outbox** — đồng bộ điểm về cơ sở nhà. Ghi hai site, **không ai chờ**.
  Chưa bắt đầu.

---

## 2. Tại sao thiết kế như vậy

### Vì sao phải phân tán — và không, không phải để chạy nhanh hơn

Phân tán **làm chậm đi** là chính. 2PC giữ khoá suốt thời gian đi mạng. Saga
cần ba giao dịch thay vì một. Truy vấn toàn trường phải gom từ ba máy.

Không ai trả cái giá đó để đổi lấy tốc độ. Ba lý do thật:

**Một — tự chủ.** Mỗi cơ sở sở hữu và tự quản dữ liệu của mình. HN quyết lớp
của HN còn chỗ hay không, bằng dữ liệu của HN. Không ai quyết hộ.

**Hai — cô lập sự cố ở tầng dữ liệu.** Máy HN chết thì dữ liệu HCM và ĐN vẫn
nguyên vẹn và vẫn đăng ký được. Bốn báo cáo toàn trường đã cài sẵn `TRY/CATCH`
cho từng site: một cơ sở tắt máy thì báo cáo ghi lại "cơ sở X không phản hồi"
rồi vẫn trả số của hai cơ sở còn lại.

**Ba — ranh giới dữ liệu trùng ranh giới tổ chức.** Sinh viên thuộc một cơ sở,
lớp mở tại một cơ sở. Ước lượng **98% thao tác chỉ chạm một mảnh**. Đó đúng là
điều kiện sách giáo khoa nói phân mảnh có lãi.

> Nếu 50% nghiệp vụ phải nối hai cơ sở thì phân tán sẽ **lỗ**, và câu trả lời
> đúng là giữ một database tập trung.

Tốc độ chỉ là hiệu ứng phụ của việc đặt dữ liệu đúng chỗ: 98% truy vấn đọc cục
bộ, không qua mạng. Đó là **locality**, không phải "truyền nhanh hơn".

> ⚠️ **Không nói "hệ thống không có điểm lỗi duy nhất".** Bản đồ án dùng
> **một API trung tâm** (mục C7b của tài liệu thiết kế), nên tầng ứng dụng
> vẫn là một điểm lỗi. Cô lập sự cố ở đây là **cô lập dữ liệu**, nói đúng
> phạm vi đó thì không ai bắt bẻ được.

### Ba quyết định chính và lý do

**Phân mảnh ngang, không dọc.** Cắt dọc là cắt cột — không giải bài toán địa
lý. Tách `SinhVien` thành hai nửa đặt hai máy thì mỗi lần đọc hồ sơ thành một
phép nối qua mạng, cho mọi sinh viên. Nghiệp vụ ở đây đọc ghi **nguyên dòng**
(một lớp, một sinh viên, một bảng điểm), không có nhóm cột nào tách ra dùng
riêng, nên cắt dọc chỉ thêm join mà không giảm được gì.

**Danh mục dùng single-master.** Môn học sửa ~15 lần/ngày, đọc gấp ~5.300 lần.
Một nơi ghi, nhân bản ra mọi nơi. Cho ghi hai chiều chỉ mua thêm bài toán giải
quyết xung đột mà không đổi lấy gì. Đây là hình dạng chuẩn: PostgreSQL,
MongoDB replica set, Kafka partition, etcd đều vậy.

**Saga cho đăng ký, 2PC chỉ cho chuyển cơ sở.** Xem chi tiết ở câu 9 bên dưới.
Nhóm **cài cả hai** rồi đối chứng, không phỏng đoán.

---

## 3. Mười câu hỏi dễ bị hỏi thêm

### 1. Vì sao hệ thống này cần phân tán?

Vì **quyền sở hữu dữ liệu** trùng với ranh giới tổ chức, không phải vì tốc độ.
Mỗi cơ sở tự mở lớp, tự xếp lịch, tự nhập điểm và chịu trách nhiệm về dữ liệu
đó. Bảng tần suất truy cập (mục B2 tài liệu thiết kế) cho thấy khoảng **98%
thao tác chỉ chạm dữ liệu của chính cơ sở mình** — gom về một chỗ là bắt ba cơ
sở tranh nhau cùng một bảng trong khi chúng không liên quan tới nhau. Phân tán
cũng giữ cho một cơ sở mất kết nối không kéo theo dữ liệu của hai cơ sở kia.

### 2. Mỗi cơ sở một server riêng là đủ rồi, sao phải thiết kế thêm?

Server riêng mới là cách triển khai vật lý, chưa phải thiết kế phân tán. Ba
database rời nhau thì chưa trả lời được ba câu: **bảng nào thuộc site nào**,
**bảng nào dùng chung và đồng bộ ra sao**, và **thống kê toàn trường lấy ở đâu**.
Đồ án này trả lời cả ba: lược đồ phân mảnh ở mục C3, nhân bản một chiều ở D1,
và truy vấn phân tán qua Linked Server ở D2. Thiếu ba thứ đó thì đúng là chỉ
có ba database độc lập.

### 3. Dữ liệu nào dùng chung, dữ liệu nào ở local?

Chia theo **ai được phép ghi**:

- **Dùng chung, chỉ Master ghi** — `MonHoc`, `Khoa`, `HocKy`,
  `ChuongTrinhDaoTao`, `CTDT_MonHoc`, `MonHocTienQuyet`, `KhungGioTiet`,
  `CoSo`. Nhân bản xuống mọi site, site **chỉ đọc**.
- **Cục bộ, site tự ghi** — `SinhVien`, `GiangVien`, `TaiKhoan`, `DotDangKy`,
  `LopHocPhan`, `LichHoc`, `DangKyHocPhan`, `DangKyMonHoc`, `SinhVienHocKy`,
  `Diem`.

Quy tắc xuyên suốt: **mỗi bảng có đúng MỘT nơi sở hữu**. Không bảng nào được
ghi từ hai phía.

### 4. Vì sao phân mảnh ngang, không dọc hay hỗn hợp?

Vì nghiệp vụ đọc ghi **nguyên dòng**: xem một lớp, một hồ sơ, một bảng điểm.
Không có nhóm cột nào chỉ một nơi dùng, nên cắt dọc chỉ thêm join qua mạng mà
không giảm được gì.

Phần đáng nói hơn là **phân mảnh ngang dẫn xuất**. `LopHocPhan` cắt theo
`MaCoSoHost`. Nhưng `DangKyHocPhan` **dẫn xuất theo `LopHocPhan`, không theo
`SinhVien`** — ghi danh thuộc về *nơi dạy*, không thuộc *nơi sinh viên học
chính quy*. `Diem` là dẫn xuất **bậc hai** qua `DangKyHocPhan`.

Chọn vậy có lý do đo được: nếu cắt `DangKyHocPhan` theo `SinhVien` thì chuyển
cơ sở phải di trú **cả lịch sử đăng ký và điểm** — khối lượng lớn nhất hệ
thống — và phá vỡ ngữ nghĩa "điểm thuộc về nơi dạy". Với vị từ theo
`LopHocPhan`, phạm vi giao dịch 2PC **thu về ba bảng nhỏ**. Đó chính là thứ
làm 2PC ở câu 9 trở nên khả thi.

### 5. "Same schema, different data" nghĩa là gì?

Ba site chạy **cùng một bộ script schema** (`db/site/10-schema-vanhanh.sql`),
cùng một ứng dụng, cùng một logic nghiệp vụ — chỉ khác dữ liệu bên trong. Nhờ
vậy thêm một cơ sở thứ tư chỉ là chạy lại bộ script đó, không phải viết code
mới. Đây cũng là điều kiện để đối soát ở yêu cầu 1 làm được: cùng tên bảng,
cùng tên cột thì mới cộng số dòng ba site rồi so với bản gốc.

### 6. Replication dùng để làm gì, và theo cơ chế nào?

**Transactional Replication một chiều**, Master → Subscribers, chỉ cho **dữ
liệu danh mục**. Mục đích là để site đọc danh mục tại chỗ thay vì hỏi Master
qua mạng ở mỗi lần duyệt lớp.

Ba chi tiết kỹ thuật đáng nêu:

- Subscriber **chỉ đọc**, chặn bằng `DENY INSERT/UPDATE/DELETE` trên bảng nhân
  bản. `DENY` là lớp chính, trigger là lớp phụ.
- Trigger ở Subscriber bắt buộc `NOT FOR REPLICATION`. Thiếu nó thì trigger
  chặn chính Distribution Agent, và replication chết với triệu chứng không
  liên quan gì tới trigger.
- Đo bằng **tracer token** — nó đi đúng đường dữ liệu thật đi, nên con số là
  trễ thật. Hiện đo được **7 giây**.

### 7. Vì sao cần invariant trong hệ phân tán?

Invariant là ràng buộc luôn đúng ở mọi trạng thái. Trong hệ phân tán chúng
quan trọng hơn vì không có một nơi nào nhìn thấy toàn bộ hệ thống, nên phải
**ép ở tầng CSDL** chứ không trông vào tầng ứng dụng. Ba cái cụ thể của đồ án
này:

- **Trần tín chỉ** trên `SinhVienHocKy`, với **hai** bộ đếm:
  `CHECK (SoTinChiDaDangKy + SoTinChiDangGiuCho <= TranTinChi)`. Hai bộ đếm vì
  một yêu cầu liên cơ sở đang chờ Host trả lời vẫn phải **giữ chỗ** trong hạn
  mức — trả sớm thì Home tưởng đã huỷ trong khi Host vẫn giữ.
- **Chống trùng môn** bằng unique **filtered** index trên
  `DangKyMonHoc(MaSinhVien, MaHocKy, MaMonHoc)`, lọc theo đúng ba trạng thái
  còn giữ chỗ. `UNIQUE(MaSinhVien, MaLopHP)` **không** chặn được hai lớp khác
  nhau của cùng một môn.
- **Chống vượt sức chứa** bằng `UPDATE … WHERE SoLuongDaDangKy < SoLuongToiDa`
  rồi kiểm `@@ROWCOUNT`. Không `SELECT COUNT` rồi `IF` — đó là race condition.

Nhóm không chỉ nêu mà đã **đo**: 100 luồng tranh 30 chỗ, đúng 30 người vào, và
70 người trượt đều trượt vì `CLASS_FULL` chứ không vì deadlock hay timeout.

### 8. Atomic là gì và vì sao quan trọng?

"Toàn bộ hoặc không có gì": thành công thì mọi thay đổi được ghi, thất bại thì
không còn dấu vết nào ở trạng thái nửa chừng. Trong hệ phân tán điều này khó
hơn vì các thay đổi nằm ở nhiều database, và mạng có thể đứt giữa chừng.

Chi tiết kỹ thuật: giao dịch phân tán bắt buộc `SET XACT_ABORT ON` — thiếu nó
thì một lỗi ở đầu xa không tự huỷ giao dịch và hệ thống kẹt ở trạng thái nửa
chừng. Trong cả hệ thống **chỉ đúng một** nghiệp vụ dùng tới nó.

### 9. Vì sao 2PC chỉ dùng cho chuyển cơ sở, không dùng cho mọi nghiệp vụ?

Vì hai nghiệp vụ có đặc tính tải ngược nhau:

| | Chuyển cơ sở sinh viên | Đăng ký liên cơ sở |
|---|---|---|
| Tần suất | vài lần mỗi kỳ | ~32.000 lượt/ngày cao điểm |
| Tranh chấp | không — mỗi SV một dòng riêng | cao — hàng trăm người tranh **một dòng lớp** |
| Trạng thái trung gian an toàn | **không có** | **có** — `DANG_XU_LY` |
| Giữ lock qua mạng | không đáng kể | thảm hoạ thông lượng |

Chuyển cơ sở động tới **ba** database: xoá ở cơ sở cũ, chèn ở cơ sở mới, cập
nhật danh bạ ở Master. Sinh viên không thể "nửa ở HCM nửa ở HN", nên không tồn
tại trạng thái trung gian an toàn nào — đây đúng là chỗ cần nguyên tử tuyệt
đối, và vài lần mỗi kỳ thì giữ khoá ba site trong một giây là chấp nhận được.

Đăng ký học phần dùng **saga + idempotent receiver** thay thế. `DANG_XU_LY`
*là* một trạng thái trung gian an toàn, nên không cần 2PC. Điểm mấu chốt của
saga ở đây là bảng `KetQuaXuLyYeuCau` lưu **mọi** kết quả, **kể cả từ chối**:
nếu chỉ lưu yêu cầu thành công thì khi Host từ chối vì lớp đầy mà phản hồi mất
trên đường về, lần retry sẽ đánh giá lại từ đầu — lúc đó có người vừa huỷ lớp
— và cho **kết quả khác lần trước**. Retry thành không tất định.

### 10. Kết luận

Đồ án không chọn phân tán cho có, mà vì dữ liệu và trách nhiệm của từng cơ sở
vốn đã tách bạch. Hệ thống không chia sẻ mọi thứ: danh mục nhân bản một chiều,
dữ liệu vận hành phân mảnh theo nơi sở hữu, và chỉ những nghiệp vụ thật sự cần
nguyên tử tuyệt đối mới trả giá 2PC.

Năm yêu cầu bắt buộc đều có bằng chứng chạy lại được:

| # | Yêu cầu | Bằng chứng |
|---|---|---|
| 1 | Phân mảnh ngang | đối soát 10/10 bảng khớp số dòng với bản gốc |
| 2 | Nhân bản một chiều | tracer token, trễ 7 giây |
| 3 | Giao dịch phân tán | 2PC, demo COMMIT + ROLLBACK cưỡng bức |
| 4 | Tương tranh | 100 luồng / 30 chỗ → đúng 30 |
| 5 | Truy vấn phân tán | pushdown 1 dòng/33 ms vs kéo thô 50.000 dòng/118 ms |

Một lệnh chạy lại cả năm: `db\tests\Chay-Bang-Chung.ps1`.

---

## 4. Nghiệm thu và chi phí

### Nghiệm thu — mỗi yêu cầu một bằng chứng kiểm được

| # | PASS khi | Hiện tại |
|---|---|---|
| 1 | Tổng số dòng từng bảng ở các site **cộng lại bằng** số dòng ở `CENTRAL` | ✅ 10/10 bảng |
| 2 | Tracer token từ Master tới mọi Subscriber **≤ 10 giây** | ✅ 7 giây |
| 3 | Demo rollback: ép lỗi giữa giao dịch → **cả hai đầu cùng huỷ** | ✅ hai màn, tự khẳng định |
| 4 | 100 luồng / 30 chỗ → đúng 30 người vào, 70 bị từ chối | ✅ và trượt đúng lý do |
| 5 | Các chiến lược cho **cùng kết quả**, khác số dòng qua mạng | ✅ 4 chiến lược khớp nhau |

Bộ test ứng dụng: **194 test, 0 đỏ**.

### Giới hạn phải nói rõ

**Yêu cầu #2 hiện chỉ có một Subscriber** (`PTITONE_HCM`, cùng máy với
Publisher). HN và ĐN hiện là giàn thử dựng tay, chưa phải Subscriber thật —
nên 7 giây là trễ nội bộ máy, **chưa phải trễ qua mạng**. Khi ba máy site vào
VPN và chạy `replication\32-subscription.sql`, script đo tự báo đủ 3 Subscriber
mà không phải sửa gì.

### Còn lại — 6 ngày tới 30/10

| Việc | Công sức | Phụ thuộc |
|---|---|---|
| Ba máy site lên VPN, nhân bản thật | 2 ngày | VPN thông |
| F02 cấp tài khoản | 1 ngày | — |
| Saga + Outbox đăng ký liên cơ sở (➕) | 3 ngày | Nền móng đã có |
| Báo cáo + ảnh chụp | 2 ngày | Chạy xuyên suốt |

**Đường găng:** VPN → nhân bản ba site thật → chụp lại bằng chứng #2. Phần ➕
cắt được nếu thiếu giờ; phần bắt buộc thì **đã xong và đã có số**.
