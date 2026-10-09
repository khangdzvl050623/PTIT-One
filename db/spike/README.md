# Spike Phần 2 — cổng chặn trước khi làm bất cứ thứ gì khác

> Hai thứ dưới đây có thể **giết hai trong năm yêu cầu bắt buộc**. Phải biết
> chúng chạy được hay không trong vài ngày đầu, không phải tuần cuối.

| Spike | Chứng minh được | Hỏng thì mất |
|---|---|---|
| A — Replication | Yêu cầu **#2** (nhân bản một chiều) | Phải chuyển sang phương án I2 |
| B — MS DTC | Yêu cầu **#3** (giao dịch phân tán) | Không hiện thực được `sp_ChuyenCoSoSinhVien` |

Yêu cầu **#1** (phân mảnh ngang), **#4** (tương tranh) và **#5** (truy vấn
phân tán) không cần spike: #1 và #5 chỉ phụ thuộc Linked Server, còn #4 **đã
xong từ Phần 1** — `EnrollmentConcurrencyIntegrationTest` chạy đúng kịch bản
100 luồng tranh 30 chỗ.

---

## Làm trên một máy trước, rồi mới qua mạng

Chạy cả hai spike trên **hai named instance cùng một máy** trước:

```
.\SPIKE_A   →  đóng vai PTITONE_MASTER
.\SPIKE_B   →  đóng vai một cơ sở
```

Lý do không phải là lười. Nó **tách được hai nguyên nhân hỏng** mà nếu chạy
thẳng qua VPN thì sẽ lẫn vào nhau:

- Cùng máy **FAIL** → sai cấu hình SQL Server (Agent, DTC, quyền, share).
- Cùng máy PASS, qua mạng **FAIL** → mạng: tường lửa, VPN, port 135, dải RPC.

Chẩn đoán sai chỗ ở bước này là cách tốn vài ngày mà không hiểu vì sao.

⚠️ **Đừng chạy spike trên instance đang giữ `PTITONE_CENTRAL`** nếu còn phải
demo Phần 1. Cấu hình Distributor là thay đổi ở mức server (tạo database
`distribution`, thêm job cho Agent) — không đụng dữ liệu, nhưng không nên
động vào máy sắp dùng để demo.

---

## Spike A — Replication

Dùng lại bộ script có sẵn, không viết mới:

```powershell
# 1. Distributor + publication trên instance đóng vai MASTER
.\db\run.ps1 -Action Replication -Step Distributor
.\db\run.ps1 -Action Replication -Step Publication

# 2. Subscription về instance đóng vai site
.\db\run.ps1 -Action Replication -Step Subscription
```

Chi tiết và mọi cái bẫy đã biết: [`db/replication/README.md`](../replication/README.md).

**PASS khi:** `INSERT` một dòng vào bảng tham chiếu ở MASTER, **≤10 giây** sau
thấy dòng đó ở site. Đo bằng [`db/tests/90-demo-nhan-ban.sql`](../tests/90-demo-nhan-ban.sql).

**Bẫy số một:** `SnapshotFolder` trong `db/config.ps1` **phải là UNC share**,
và tài khoản chạy SQL Server Agent phải đọc/ghi được nó. Đường dẫn local là
lỗi giết nhiều nhóm nhất.

---

## Spike B — MS DTC

```powershell
sqlcmd -S <SRV-HCM> -d PTITONE_HCM -E -N -C -b -I -f 65001 `
       -v LinkedServer="SRV_HN" RemoteDb="PTITONE_HN" `
       -i db/spike/01-kiem-dtc.sql
```

Script tự dựng bảng rác hai đầu, chạy hai ca, rồi dọn. Không đụng bảng nghiệp
vụ nào.

| Ca | Chứng minh |
|---|---|
| 1 — COMMIT | gọi được qua mạng, hai đầu cùng có dòng |
| 2 — ROLLBACK | **tính nguyên tử** — hai đầu cùng huỷ, không đầu nào sót |

Ca 2 mới là ca quan trọng. COMMIT thành công chỉ nói "mạng thông"; chỉ
ROLLBACK mới chứng minh hai đầu thật sự nằm trong **một** giao dịch.

**PASS khi:** in ra `DTC: PASS`.

**FAIL điển hình:** lỗi `7391 — unable to begin a distributed transaction`.
Đây gần như luôn là DTC chưa thông, không phải lỗi SQL. Sửa ở [F4b] chứ đừng
sửa script: Network DTC Access · Allow Inbound + Outbound · **No Authentication
Required** · mở port **135** và dải RPC động trên tường lửa cả hai máy.

---

## Sau khi có kết quả

| Kết quả | Việc tiếp |
|---|---|
| A PASS · B PASS | Đi tiếp theo kế hoạch. Chốt D12 (số cơ sở) và D10 (kiểu instance). |
| A FAIL | Phương án **I2**: named instance trên một máy. Giữ nguyên 100% giá trị học thuật; chỉ mất độ trễ mạng thật, phải ghi rõ trong báo cáo và mô phỏng bằng `clumsy`. |
| B FAIL qua mạng, PASS cùng máy | Vấn đề mạng. Chạy yêu cầu #3 trên hai instance cùng máy; vẫn là 2PC thật, vẫn đủ điểm. |
| B FAIL cả cùng máy | **Hỏi giảng viên ngay.** Yêu cầu #3 là mục duy nhất trong năm mục chưa có đường lui nào khác. |
