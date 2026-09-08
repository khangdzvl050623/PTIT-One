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
)

$args = @('-S', $server, '-E', '-b', '-I', '-i', $sqlPath)
foreach ($v in $vars) { $args += @('-v', $v) }

Write-Host ""
Write-Host "  Script : $Script"        -ForegroundColor Cyan
Write-Host "  Server : $server"        -ForegroundColor Cyan
Write-Host "  Target : $($cfg.Databases[$On])  ($On)" -ForegroundColor Cyan
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
