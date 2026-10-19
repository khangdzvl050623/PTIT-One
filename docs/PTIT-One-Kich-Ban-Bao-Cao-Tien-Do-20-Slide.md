# PTIT One — Lời thuyết trình báo cáo tiến độ (20 slide)

**Dùng cho:** buổi báo cáo tiến độ, ~13–15 phút, hai người nói, chưa tính hỏi đáp.
**Số liệu tính đến:** 10/10/2026.

> **Ba chỗ phải sửa trên slide trước khi nói**
>
> 1. Slide ghi **"68 ENDPOINT · SNAPSHOT 03/10/2026"**. Số thật hôm nay:
>    **67 đường dẫn, 85 thao tác** (`apps/api/openapi.json`). Sửa slide, hoặc
>    nói "gần 70 đường dẫn API" cho khớp.
> 2. Slide 18 cũ ghi *"một số màn hình vẫn đang hoàn thiện"* — không còn đúng.
> 3. Slide 19 cũ gọi Phần 2 là *"phương án thiết kế để nghiên cứu cho tương
>    lai"*. Phần 2 **đã có kế hoạch ba tuần và bắt đầu tuần này** — nói như cũ
>    là tự hạ thấp phần nặng điểm nhất.
> 4. Slide 17: bản cũ lấy **"mở đợt đăng ký"** làm ví dụ thông báo. Tin đó do
>    quản trị **soạn tay**, không tự sinh — `EnrollmentPeriodService` không
>    phát thông báo nào. Sáu sự kiện tự sinh là: đăng ký · huỷ đăng ký · công
>    bố điểm · sửa điểm · lớp bị huỷ · bị gỡ khỏi lớp. Đừng gộp hai loại làm
>    một, thầy hỏi lại là lộ.

**Cách nói:** mỗi slide 3–5 câu, câu ngắn, một ý một câu. Không đọc slide.
Chỗ in đậm là chỗ nhấn giọng rồi dừng nửa nhịp.

**Sợi chỉ xuyên bài:** đi theo một sinh viên đăng ký lớp, và **dừng lại ở
những chỗ hệ thống từ chối** — vì chỗ từ chối mới là chỗ có kỹ thuật.

**Phân vai:** Người 1 nói slide 1–11 và demo. Người 2 nói slide 12–20.

---

## SLIDE 01 | PTIT One

Em chào thầy và các bạn. Nhóm em làm **PTIT One** — hệ thống đăng ký tín chỉ
cho trường có ba cơ sở.

Thay vì liệt kê công nghệ, em xin kể theo đúng một hành trình: một sinh viên
đăng nhập, tìm lớp, bấm đăng ký, rồi xem thời khoá biểu và điểm.

Dọc hành trình đó em sẽ dừng ở những chỗ hệ thống **từ chối** — vì đó mới là
chỗ đáng nói.

Cuối bài em báo cáo Phần 1 đã xong tới đâu, và ba tuần tới Phần 2 làm gì.

---

## SLIDE 02 | Một hệ thống, hai giai đoạn

Đồ án chia hai giai đoạn. Em xin nói rõ ranh giới ngay từ đầu để thầy khỏi
phải đoán.

**Phần 1** là toàn bộ nghiệp vụ học vụ, chạy trên một database tập trung.
Phần này **đã xong** — code đã merge, test đã chạy.

**Phần 2** là tách database đó ra nhiều cơ sở, thêm nhân bản và giao dịch phân
tán. Phần này **bắt đầu tuần này**.

Hôm nay em demo Phần 1, chạy thật. Phần 2 em chỉ trình bày hướng làm.

---

## SLIDE 03 | Bài toán đa cơ sở

Vì sao phải phân tán? Ba cơ sở Hà Nội, Đà Nẵng và TP.HCM cách nhau hàng trăm
cây số.

Dồn hết về một máy chủ thì đường truyền xa, và máy đó chết là cả ba cơ sở
ngừng đăng ký.

Nhưng để mỗi cơ sở làm một hệ thống riêng thì danh mục môn học và chương trình
đào tạo lệch nhau, sinh viên cũng không học liên cơ sở được.

Bài toán nằm đúng ở giữa: **dữ liệu chung thì dùng chung, dữ liệu của cơ sở nào
thì nằm ở cơ sở đó.** Phần 2 giải bài này.

---

## SLIDE 04 | Ai sử dụng PTIT One?

Hệ thống có bốn vai. Sinh viên đăng ký và xem điểm. Giảng viên nhập điểm cho
lớp mình dạy. Quản trị cơ sở mở lớp, xếp lịch, mở đợt đăng ký. Admin Master
giữ danh mục dùng chung.

Điều đáng nói không phải là có bốn vai, mà là **vai được kiểm ở backend, không
phải ở giao diện.**

Giao diện ẩn nút chỉ để cho gọn. Ai gõ thẳng URL thì API vẫn trả về từ chối.

---

## SLIDE 05 | Nền tảng Phần 1

Kiến trúc Phần 1 gọn: trình duyệt chạy React, gọi API Spring Boot, API đọc ghi
SQL Server.

Trình duyệt **không bao giờ** nối thẳng vào database. Mọi quy tắc nghiệp vụ
nằm một chỗ, trong API.

Hiện có **67 đường dẫn API, 85 thao tác**. Có hợp đồng OpenAPI và một test tự
động chặn việc tài liệu lệch khỏi code.

---

## SLIDE 06 | Đăng nhập và vai trò

Đăng nhập xong, hệ thống không chỉ nhớ *ai*, mà nhớ cả *vai gì, cơ sở nào*.

Thông tin đó nằm trong token đã ký. Hệ thống **không lấy từ tham số client gửi
lên** — vì tin client thì chỉ cần sửa URL là leo được quyền.

Mỗi request sau đó đều kiểm lại. Sinh viên chỉ thấy hồ sơ của mình, giảng viên
chỉ thấy lớp mình được phân công.

---

## SLIDE 07 | Thiết lập danh mục học vụ

Trước khi mở lớp phải có danh mục: môn học, chương trình đào tạo, học kỳ, và
môn tiên quyết.

Môn tiên quyết chính là thứ sau này dùng để chặn đăng ký — muốn học Cơ sở dữ
liệu phân tán thì phải qua Cơ sở dữ liệu trước.

Một chi tiết nhỏ nhưng dễ sập: hệ thống chặn **vòng tiên quyết**. A cần B, B
cần A thì không ai học được môn nào — mà lỗi đó chỉ lộ ra đúng lúc sinh viên
đăng ký.

---

## SLIDE 08 | Mở lớp và xếp lịch

Quản trị tạo lớp học phần: đặt sức chứa, phân công giảng viên, chọn phòng và
khung giờ.

Hệ thống đối chiếu ngay lúc xếp — một giảng viên không dạy hai lớp cùng giờ,
một phòng không chứa hai lớp cùng giờ.

Bắt lỗi ở khâu chuẩn bị rẻ hơn nhiều so với để sinh viên phát hiện sau khi đã
đăng ký xong.

---

## SLIDE 09 | Mở đợt đăng ký

Có lớp rồi vẫn chưa đăng ký được. Phải có **đợt đăng ký**: chọn học kỳ, khung
thời gian, cơ sở áp dụng, rồi công bố.

Đợt chưa mở hoặc đã đóng thì API từ chối, kể cả khi người dùng vào được đường
dẫn trên giao diện.

Đây là mẫu lặp lại suốt hệ thống: **giao diện không phải cổng chặn, API mới
là.**

---

## SLIDE 10 | Sinh viên tìm lớp

Đến lượt sinh viên. Màn hình tìm lớp cho xem môn, giảng viên, lịch và số chỗ
còn lại.

Nhưng số chỗ đó là số **tại thời điểm tải trang**. Trong lúc sinh viên còn
đang cân nhắc, lớp có thể đã đầy.

Nên con số trên màn hình chỉ để tham khảo. Quyết định thật nằm ở bước sau.

---

## SLIDE 11 | Kiểm tra điều kiện đăng ký

Khi bấm đăng ký, API chạy qua **năm cửa**, theo đúng thứ tự: đợt còn mở không ·
lớp còn nhận không · đã qua môn tiên quyết chưa · có trùng lịch không · còn chỗ
không.

Rớt cửa nào thì trả về đúng lý do của cửa đó, không trả lỗi chung chung — để
sinh viên biết phải xử lý gì.

Qua hết năm cửa mới sang bước ghi danh.

---

## SLIDE 12 | Ghi danh an toàn

*(Người 2 nhận từ đây. Đây là đỉnh của bài — nói chậm lại.)*

Đây là chỗ khó nhất của Phần 1.

Lớp còn **một** chỗ. Ba mươi sinh viên bấm đăng ký trong cùng một giây. Nếu
lập trình theo kiểu "đếm số chỗ, nếu còn thì ghi" thì cả ba mươi người đều
thấy còn chỗ, và cả ba mươi cùng vào. Lớp vỡ sức chứa.

Nhóm em **không đếm trước**. Nhóm em cập nhật có điều kiện: tăng bộ đếm *chỉ
khi* số đã đăng ký còn nhỏ hơn sức chứa, rồi đọc xem có đúng một dòng bị ảnh
hưởng không. Ai lấy được dòng đó thì đậu, còn lại trượt.

Nhóm em có test tự động bắn **100 luồng song song** vào một lớp 30 chỗ. Kết
quả: đúng 30 người vào, 70 người bị từ chối, chạy bao nhiêu lần cũng vậy.

---

## SLIDE 13 | Khi sinh viên hủy lớp

Huỷ cũng không đơn giản như xoá một dòng.

Hệ thống kiểm còn trong hạn huỷ không, rồi đổi trạng thái đăng ký **và** trả
chỗ lại cho lớp — hai việc trong **cùng một giao dịch**.

Làm một nửa thì danh sách lớp của sinh viên và số chỗ của lớp lệch nhau vĩnh
viễn.

Và nếu người dùng bấm huỷ hai lần, lần thứ hai không được trả chỗ thêm lần
nữa.

---

## SLIDE 14 | Xem lớp và thời khóa biểu

Đăng ký xong, sinh viên xem lại lớp của mình và thời khoá biểu tuần, dựng từ
các lớp còn hiệu lực trong học kỳ.

Một chi tiết nhỏ của giao diện nhưng quan trọng: **"chưa đăng ký lớp nào" và
"không tải được dữ liệu" là hai màn hình khác nhau.**

Gộp hai cái đó lại thì sinh viên tưởng mình vừa mất hết đăng ký.

---

## SLIDE 15 | Giảng viên theo dõi lớp

Giảng viên thấy các lớp được phân công, sĩ số và danh sách sinh viên.

Phạm vi này lấy từ bảng phân công trong database, **không** lấy từ tham số
trên URL.

Nên nếu giảng viên sửa mã lớp trên thanh địa chỉ sang lớp của đồng nghiệp, API
từ chối. Nhóm em có test riêng cho đúng tình huống này.

---

## SLIDE 16 | Nhập và công bố điểm

Điểm đi qua **ba trạng thái**, không phải một.

Giảng viên nhập và lưu nháp — sinh viên chưa thấy gì. Giảng viên công bố — lúc
đó sinh viên mới thấy. Quản trị khoá điểm — không ai sửa được nữa.

Tách ba trạng thái để nhà trường biết điểm đang ở khâu nào, và để một sai sót
lúc nhập không lập tức hiện ra trước mặt sinh viên.

---

## SLIDE 17 | Sinh viên xem điểm và thông báo

Sinh viên chọn học kỳ và xem bảng điểm.

Môn chưa công bố hiển thị là **"chưa có điểm"**, không hiển thị số 0 — vì 0 và
"chưa chấm" là hai chuyện hoàn toàn khác nhau.

Bên cạnh là hộp thư thông báo, và thông báo có **hai loại**. Loại quản trị
soạn tay — có bản nháp, gửi theo phạm vi toàn trường, cơ sở hoặc một lớp. Và
loại **tự sinh từ sự kiện**: đăng ký thành công, huỷ đăng ký, điểm được công
bố, điểm bị sửa, lớp bị huỷ, bị gỡ khỏi lớp. Sáu sự kiện đó không ai gõ tay.

Chi tiết em muốn nhấn: thông báo tự sinh nằm **trong cùng giao dịch** với
nghiệp vụ. Đăng ký mà rollback thì thông báo "đăng ký thành công" biến mất
theo — hệ thống không bao giờ báo thành công cho một việc chưa xảy ra. Nhóm em
có test riêng cho đúng tình huống này.

---

## SLIDE 18 | Trạng thái và ranh giới demo

Em xin chốt trạng thái, tính đến hôm nay.

**Phần 1 đã xong:** 85 thao tác API, 10 phiên bản lược đồ database, **189 test
tự động** trong 27 lớp test, và giao diện web đã nối đủ các luồng vừa trình
bày.

**Phần 2 bắt đầu tuần này**, chưa có gì chạy thật.

Hôm nay em chỉ demo những gì đã test. Chỗ nào còn đang làm em sẽ nói rõ là
đang làm.

---

## SLIDE 19 | Phần 2

Phần 2 mới là phần đồ án này hướng tới. Em xin nói ngắn, **bốn ý**.

**Một — phân mảnh.** Cắt database hiện tại theo cơ sở. Sinh viên và lớp về
đúng cơ sở của mình. Riêng bảng đăng ký đi theo **lớp**, không đi theo sinh
viên — vì lớp mới là thứ thuộc về một cơ sở.

**Hai — nhân bản.** Danh mục dùng chung nằm ở một nút Master, tự động đẩy
xuống ba cơ sở, **một chiều**. Cơ sở chỉ đọc, không sửa.

**Ba — giao dịch phân tán.** Khi sinh viên chuyển cơ sở, hai database phải
cùng đổi hoặc cùng không đổi. Chỗ này dùng 2PC.

**Bốn — truy vấn phân tán.** Thống kê toàn trường phải gom dữ liệu từ ba cơ sở
trong một câu truy vấn.

Còn tình huống tranh chỗ thì nhóm em **đã làm xong ở Phần 1**, sang Phần 2 chỉ
chạy lại trên hệ phân tán.

---

## SLIDE 20 | Demo và kết luận

**Người 1 — demo:**

Em xin demo đúng luồng vừa kể. Đăng nhập bằng tài khoản sinh viên, vào danh
sách lớp, bấm đăng ký.

Em sẽ **cố tình chọn một lớp trùng lịch trước**, để thầy thấy hệ thống từ chối
và báo đúng lý do, rồi mới đăng ký lớp hợp lệ.

Sau đó em mở thời khoá biểu và bảng điểm.

**Người 2 — kết luận và hướng đi:**

Tóm lại: Phần 1 đã xong và chạy được. Nghiệp vụ học vụ đầy đủ, quyền kiểm ở
backend, và tình huống tranh chỗ đã có test chứng minh chứ không phải nói
suông.

**Ba tuần tới nhóm em làm Phần 2 theo thứ tự này:**

- **Tuần này** — dựng bốn máy: một máy Master giữ danh mục, ba máy cho ba cơ
  sở. Chạy thử nhân bản và giao dịch phân tán trước tiên, để biết sớm có vướng
  gì không, thay vì phát hiện ở tuần cuối.
- **Tuần sau** — tách dữ liệu hiện có ra ba cơ sở theo đúng vị từ phân mảnh,
  rồi hoàn thành: nhân bản chạy thật, thủ tục chuyển cơ sở bằng 2PC **có demo
  rollback khi tắt máy đích**, truy vấn phân tán gom ba cơ sở, và chạy lại bài
  test tranh chỗ trên hệ phân tán.
- **Tuần cuối** — đo đạc, so sánh các phương án, và viết báo cáo.

Mốc nhóm em tự đặt cho mình: đến **24/10**, năm yêu cầu bắt buộc phải có bằng
chứng chạy thật, không còn gì ở mức thiết kế.

Em xin hết. Nhóm em mong nhận câu hỏi của thầy.

---

## Câu hỏi dự kiến

Trả lời ngắn. Không biết thì nói chưa làm tới, đừng đoán.

**Vì sao không làm phân tán ngay từ đầu?**
Nhóm em chia lại phạm vi: làm đúng nghiệp vụ trên một database trước, rồi mới
tách. Tách một hệ thống đã chạy đúng thì biết ngay chỗ nào vỡ; làm phân tán
trước thì không phân biệt được lỗi nghiệp vụ với lỗi phân tán.

**Vì sao chuyển cơ sở dùng 2PC mà đăng ký học phần thì không?**
Chuyển cơ sở hiếm khi xảy ra và không có trạng thái "nửa chuyển" nào an toàn,
nên chịu được chi phí 2PC. Đăng ký là đường nóng, tranh chỗ cao — giữ khoá
xuyên mạng ở đó là tự tạo nghẽn. Luồng liên cơ sở nhóm em dùng saga.

**Chống vượt sức chứa bằng cách nào?**
Cập nhật có điều kiện rồi kiểm số dòng bị ảnh hưởng, không đếm trước rồi mới
ghi. Thêm ràng buộc chống đăng ký trùng môn và khoá tuần tự hoá đặt **trước**
mọi phép kiểm.

**Dữ liệu demo có phải dữ liệu thật không?**
Không. Dữ liệu seed cố định, dựng lại được bằng script trong dự án. Nhóm em
không chiếu mật khẩu, mã kích hoạt hay thông tin cá nhân nào.

**Nhân bản và truy vấn phân tán chạy chưa?**
Chưa. Script đã viết xong và đã kiểm cú pháp, nhưng chưa chạy trên SQL Server
thật. Tuần này nhóm em chạy thử, và em sẽ báo cáo kết quả ở buổi sau.
