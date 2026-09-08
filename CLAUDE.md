# UISPTITv2 — Quản lý đăng ký tín chỉ đa cơ sở

Đồ án **Cơ sở dữ liệu phân tán**. SQL Server, nhiều cơ sở, phân mảnh ngang +
nhân bản một chiều + giao dịch phân tán + truy vấn phân tán.

**Thiết kế đã CHỐT.** Nguồn sự thật duy nhất: `docs/UISPTITv2-Thiet-Ke-v2.md`.
Trước khi sinh code, đọc mục **0.1b** (năm yêu cầu bắt buộc) và **0.1**
(bảng quyết định D1–D16). Đề xuất khác thiết kế thì nêu ra để nhóm quyết,
đừng tự đổi.

## Ràng buộc tầng CSDL — sai là hỏng bài

| Quy tắc | Chi tiết |
|---|---|
| **Vị từ phân mảnh dẫn xuất** | `DangKyHocPhan ⋉ LopHocPhan` — **KHÔNG** phải `⋉ SinhVien`. `Diem` dẫn xuất bậc 2 qua `DangKyHocPhan`. Đây là lỗi đã từng mắc, đừng lặp lại |
| **Trigger ở Subscriber** | **BẮT BUỘC** `CREATE TRIGGER … NOT FOR REPLICATION`. Thiếu nó thì trigger chặn chính Distribution Agent và replication chết với triệu chứng không liên quan |
| **Chống vượt sức chứa** | `UPDATE LopHocPhan SET SoLuongDaDangKy = SoLuongDaDangKy + 1 WHERE MaLopHP = … AND SoLuongDaDangKy < SoLuongToiDa;` rồi kiểm `@@ROWCOUNT`. **Cấm** `SELECT COUNT` rồi `IF` — đó là race condition |
| **Chống vượt trần tín chỉ** | Cùng kỹ thuật, nhưng ở **Home**, trên `SinhVien.SoTinChiDangKyKy`. Cộng ngay khi tạo yêu cầu `CHO_DUYET`, trả lại khi bị từ chối |
| **Bộ đếm do ứng dụng sở hữu** | Không trigger nào được cập nhật `SoLuongDaDangKy` — nếu không sẽ nhảy 2 mỗi lần đăng ký |
| **Thứ tự khóa** | Luôn `LopHocPhan` trước, `DangKyHocPhan` sau — ở **mọi** luồng, kể cả hủy đăng ký. Đảo ở một chỗ là sinh deadlock ngẫu nhiên |
| **Không dùng `MERGE`** | Dùng `UPDATE` trước, `INSERT` sau, có `UPDLOCK, HOLDLOCK`. `MERGE` của SQL Server không tự lấy khóa phù hợp |
| **Không dùng `IDENTITY`** | Chọn khóa **theo từng aggregate** (mục C9). Nhúng mã cơ sở vào khóa **chỉ khi** cơ sở là một phần ngữ nghĩa của thực thể — `LopHocPhan` thì đúng, `SinhVien` thì không |
| **Giao dịch phân tán** | `SET XACT_ABORT ON` là bắt buộc. Chỉ dùng cho **chuyển cơ sở sinh viên**, tuyệt đối không cho đăng ký học phần |
| **Subscriber chỉ đọc** | `DENY INSERT/UPDATE/DELETE` trên bảng nhân bản. `DENY` là lớp chính, trigger là lớp phụ |

## Ràng buộc tầng ứng dụng

| Quy tắc | Chi tiết |
|---|---|
| **Một giao dịch = một site** | `@Transactional` **không bao giờ** trải hai DataSource. `AbstractRoutingDataSource` phân giải khóa một lần; đổi site giữa chừng sẽ ghi nhầm site hoặc mất tính nguyên tử **mà không ném lỗi**. Ghép nhiều site bằng saga, không bằng transaction |
| **Cơ sở lấy từ JWT đã ký** | **Tuyệt đối không** tin tham số client gửi lên (`?campus=HN`). Đó là lỗ hổng leo thang đặc quyền |
| **JdbcTemplate cho đường nóng** | Đăng ký, truy vấn chéo site, benchmark — cần thấy chính xác SQL và đọc `@@ROWCOUNT`. JPA chỉ dùng cho CRUD danh mục nếu thật sự cần |
| **Đúng 3 port, không hơn** | `CrossSiteQuery`, `GlobalReport`, `CatalogHealth`. `SiteContext`, `RoutingDataSource`, `OutboxWorker` là class cụ thể, không phải interface |
| **`initialization-fail-timeout: -1`** | Bắt buộc, để ứng dụng vẫn khởi động khi một site đang tắt. Thiếu nó là hỏng kịch bản demo tắt site |
| **Thứ tự Outbox** | Upsert vào mirror **TRƯỚC**, đánh dấu `SENT` **SAU**. Đảo thứ tự là mất sự kiện vĩnh viễn |
| **Outbox chỉ cho sinh viên khách** | Sinh viên có cơ sở nhà trùng site thì điểm đã nằm đúng chỗ, không phát sự kiện |

## Quy ước đặt tên

- **Bảng và cột: tiếng Việt không dấu** (`SinhVien`, `MaCoSoNha`) — giảng viên đọc lược đồ
- **Code, interface, biến: tiếng Anh** (`CrossSiteQuery`, `SiteContext`)
- **Read model mang hậu tố `Mirror`** (`BangDiemMirror`) — nhìn tên là biết không phải nguồn sự thật
- Commit theo Conventional Commits rút gọn, có thêm loại `db:` — xem README

## Không được làm

Microservices · Kafka/RabbitMQ · Redis · Kubernetes · nhân bản hai chiều hoặc
merge replication · 2PC cho đăng ký học phần · phân mảnh dọc · port thứ tư ·
frontend nặng (shadcn, state library, router phức tạp) · thêm bảng CRUD không
phục vụ một khái niệm phân tán nào.

## Thứ tự ưu tiên

Phần **in đậm** trong tài liệu thiết kế là bắt buộc theo đề bài (~75% điểm).
Phần ➕ chỉ làm sau khi phần bắt buộc đã xong và đã chụp đủ screenshot.
**Cổng chặn cuối tuần 4.** Không viết code ứng dụng trước khi cài đặt vật lý
đã PASS.

## Git

`feature/* → dev → main`, không push thẳng. `dev` cần PR nhưng 0 approval;
`main` cần 1 approval của CODEOWNERS. Chi tiết ở README.

---

<!-- rtk-instructions v2 -->
# RTK (Rust Token Killer) - Token-Optimized Commands

## Golden Rule

**Always prefix commands with `rtk`**. If RTK has a dedicated filter, it uses it. If not, it passes through unchanged. This means RTK is always safe to use.

**Important**: Even in command chains with `&&`, use `rtk`:
```bash
# ❌ Wrong
git add . && git commit -m "msg" && git push

# ✅ Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## RTK Commands by Workflow

### Build & Compile (80-90% savings)
```bash
rtk cargo build         # Cargo build output
rtk cargo check         # Cargo check output
rtk cargo clippy        # Clippy warnings grouped by file (80%)
rtk tsc                 # TypeScript errors grouped by file/code (83%)
rtk lint                # ESLint/Biome violations grouped (84%)
rtk prettier --check    # Files needing format only (70%)
rtk next build          # Next.js build with route metrics (87%)
```

### Test (60-99% savings)
```bash
rtk cargo test          # Cargo test failures only (90%)
rtk go test             # Go test failures only (90%)
rtk jest                # Jest failures only (99.5%)
rtk vitest              # Vitest failures only (99.5%)
rtk playwright test     # Playwright failures only (94%)
rtk pytest              # Python test failures only (90%)
rtk rake test           # Ruby test failures only (90%)
rtk rspec               # RSpec test failures only (60%)
rtk test <cmd>          # Generic test wrapper - failures only
```

### Git (59-80% savings)
```bash
rtk git status          # Compact status
rtk git log             # Compact log (works with all git flags)
rtk git diff            # Compact diff (80%)
rtk git show            # Compact show (80%)
rtk git add             # Ultra-compact confirmations (59%)
rtk git commit          # Ultra-compact confirmations (59%)
rtk git push            # Ultra-compact confirmations
rtk git pull            # Ultra-compact confirmations
rtk git branch          # Compact branch list
rtk git fetch           # Compact fetch
rtk git stash           # Compact stash
rtk git worktree        # Compact worktree
```

Note: Git passthrough works for ALL subcommands, even those not explicitly listed.

### GitHub (26-87% savings)
```bash
rtk gh pr view <num>    # Compact PR view (87%)
rtk gh pr checks        # Compact PR checks (79%)
rtk gh run list         # Compact workflow runs (82%)
rtk gh issue list       # Compact issue list (80%)
rtk gh api              # Compact API responses (26%)
```

### JavaScript/TypeScript Tooling (70-90% savings)
```bash
rtk pnpm list           # Compact dependency tree (70%)
rtk pnpm outdated       # Compact outdated packages (80%)
rtk pnpm install        # Compact install output (90%)
rtk npm run <script>    # Compact npm script output
rtk npx <cmd>           # Compact npx command output
rtk prisma              # Prisma without ASCII art (88%)
```

### Files & Search (60-75% savings)
```bash
rtk ls <path>           # Tree format, compact (65%)
rtk read <file>         # Code reading with filtering (60%)
rtk grep <pattern>      # Search grouped by file (75%). Format flags (-c, -l, -L, -o, -Z) run raw.
rtk find <pattern>      # Find grouped by directory (70%)
```

### Analysis & Debug (70-90% savings)
```bash
rtk err <cmd>           # Filter errors only from any command
rtk log <file>          # Deduplicated logs with counts
rtk json <file>         # JSON structure without values
rtk deps                # Dependency overview
rtk env                 # Environment variables compact
rtk summary <cmd>       # Smart summary of command output
rtk diff                # Ultra-compact diffs
```

### Infrastructure (85% savings)
```bash
rtk docker ps           # Compact container list
rtk docker images       # Compact image list
rtk docker logs <c>     # Deduplicated logs
rtk kubectl get         # Compact resource list
rtk kubectl logs        # Deduplicated pod logs
```

### Network (65-70% savings)
```bash
rtk curl <url>          # Compact HTTP responses (70%)
rtk wget <url>          # Compact download output (65%)
```

### Meta Commands
```bash
rtk gain                # View token savings statistics
rtk gain --history      # View command history with savings
rtk discover            # Analyze Claude Code sessions for missed RTK usage
rtk proxy <cmd>         # Run command without filtering (for debugging)
rtk init                # Add RTK instructions to CLAUDE.md
rtk init --global       # Add RTK to ~/.claude/CLAUDE.md
```

## Token Savings Overview

| Category | Commands | Typical Savings |
|----------|----------|-----------------|
| Tests | vitest, playwright, cargo test | 90-99% |
| Build | next, tsc, lint, prettier | 70-87% |
| Git | status, log, diff, add, commit | 59-80% |
| GitHub | gh pr, gh run, gh issue | 26-87% |
| Package Managers | pnpm, npm, npx | 70-90% |
| Files | ls, read, grep, find | 60-75% |
| Infrastructure | docker, kubectl | 85% |
| Network | curl, wget | 65-70% |

Overall average: **60-90% token reduction** on common development operations.
<!-- /rtk-instructions -->
