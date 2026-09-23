<#
.SYNOPSIS
    Chạy một script SQL của PTIT One với biến topology lấy từ config.ps1.

.DESCRIPTION
    Mọi script trong db/ dùng biến SQLCMD ($(DbMaster), $(SrvHN)...) thay vì
    hardcode tên máy và tên database. Runner này bơm giá trị từ config.ps1 vào.

    Nhờ vậy đổi topology chỉ cần sửa config.ps1, không đụng tới file SQL nào.

.PARAMETER Script
    Đường dẫn script SQL, tương đối với thư mục db/.

.PARAMETER On
    Chạy trên đâu: MASTER | HCM | HN | DN

.PARAMETER WhatIf
    Chỉ in ra lệnh sqlcmd sẽ chạy, không thực thi.

.EXAMPLE
    .\run.ps1 -Script 00-create-databases.sql -On MASTER
    .\run.ps1 -Script replication\30-distributor.sql -On MASTER
    .\run.ps1 -Script site\10-schema-vanhanh.sql -On HN
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Script,
    [Parameter(Mandatory)] [ValidateSet('MASTER','HCM','HN','DN')] [string] $On,
    [switch] $WhatIf
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$cfg  = Import-PowerShellDataFile (Join-Path $here 'config.ps1')

$sqlPath = Join-Path $here $Script
if (-not (Test-Path $sqlPath)) { throw "Khong tim thay script: $sqlPath" }

$server = $cfg.Servers[$On]
if (-not $server) { throw "Khong co cau hinh server cho '$On' trong config.ps1" }

# --- Mọi database được cấu hình nằm trên CHÍNH instance này -------------
# Nhờ vậy một lần chạy 00-create-databases.sql trên mỗi máy là tạo đủ,
# không phải nhớ chạy thêm -On HCM cho máy vừa giữ Master vừa giữ HCM.
$dbList = (
    $cfg.Servers.GetEnumerator() |
    Where-Object { $_.Value -eq $server } |
    ForEach-Object { $cfg.Databases[$_.Key] } |
    Sort-Object -Unique
) -join ','

# --- Bơm toàn bộ topology vào biến SQLCMD -------------------------------
$vars = @(
    "SrvMaster=$($cfg.Servers.MASTER)"
    "SrvHCM=$($cfg.Servers.HCM)"
    "SrvHN=$($cfg.Servers.HN)"
    "SrvDN=$($cfg.Servers.DN)"

    "DbMaster=$($cfg.Databases.MASTER)"
    "DbHCM=$($cfg.Databases.HCM)"
    "DbHN=$($cfg.Databases.HN)"
    "DbDN=$($cfg.Databases.DN)"

    "Collation=$($cfg.Collation)"

    "SnapshotFolder=$($cfg.Replication.SnapshotFolder)"
    "PublicationName=$($cfg.Replication.PublicationName)"
    "DistRetention=$($cfg.Replication.DistributionRetentionHours)"
    "SubRetention=$($cfg.Replication.SubscriptionRetentionHours)"

    "LnkHN=$($cfg.LinkedServers.HN)"
    "LnkDN=$($cfg.LinkedServers.DN)"

    # Database đích của lần chạy này — script dùng để tự kiểm tra chạy đúng chỗ
    "DbTarget=$($cfg.Databases[$On])"
    "SiteTarget=$On"

    # ---------------------------------------------------------------
    # Danh sách MỌI database thuộc instance đang kết nối.
    #
    # Việc phân giải topology làm ở PowerShell, KHÔNG làm trong T-SQL:
    # so khớp tên máy trong SQL rất dễ sai với named instance ('.\SITE_HN'),
    # với tên gần giống nhau, và với alias. PowerShell so khớp đúng chuỗi
    # cấu hình nên không có chuyện đoán nhầm.
    # ---------------------------------------------------------------
    "DbList=$dbList"
)

$args = @('-S', $server, '-E', '-b', '-I', '-i', $sqlPath)
foreach ($v in $vars) { $args += @('-v', $v) }

Write-Host ""
Write-Host "  Script : $Script"        -ForegroundColor Cyan
Write-Host "  Server : $server"        -ForegroundColor Cyan
Write-Host "  Target : $($cfg.Databases[$On])  ($On)" -ForegroundColor Cyan
Write-Host "  DB tren instance nay : $dbList" -ForegroundColor DarkGray
Write-Host ""

if ($WhatIf) {
    Write-Host "sqlcmd $($args -join ' ')" -ForegroundColor DarkGray
    return
}

& sqlcmd @args
if ($LASTEXITCODE -ne 0) {
    throw "sqlcmd that bai voi ma loi $LASTEXITCODE"
}
Write-Host "  OK" -ForegroundColor Green
