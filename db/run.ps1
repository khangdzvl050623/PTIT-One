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

.PARAMETER Subscribers
    Chỉ dùng cho script subscription (32 và 39-go-subscription). Giới hạn
    danh sách Subscriber được đăng ký/gỡ trong lần chạy này. Mặc định là
    toàn bộ Sites trong config.ps1.

    Cần thiết khi máy bạn học chưa lên: đăng ký subscription tới một server
    không tồn tại vẫn "thành công" ở mức metadata, nhưng để lại Distribution
    Agent job chạy lỗi liên tục.

.PARAMETER WhatIf
    Chỉ in ra lệnh sqlcmd sẽ chạy, không thực thi.

.EXAMPLE
    .\run.ps1 -Script 00-create-databases.sql -On MASTER
    .\run.ps1 -Script replication\30-distributor.sql -On MASTER
    .\run.ps1 -Script replication\32-subscription.sql -On MASTER -Subscribers HCM
    .\run.ps1 -Script site\10-schema-vanhanh.sql -On HN
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Script,
    [Parameter(Mandatory)] [ValidateSet('MASTER','HCM','HN','DN')] [string] $On,
    [string[]] $Subscribers,
    [switch] $WhatIf
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$cfg  = Import-PowerShellDataFile (Join-Path $here 'config.ps1')

$sqlPath = Join-Path $here $Script
if (-not (Test-Path $sqlPath)) { throw "Khong tim thay script: $sqlPath" }

$server = $cfg.Servers[$On]
if (-not $server) { throw "Khong co cau hinh server cho '$On' trong config.ps1" }

# --- Subscriber được chọn cho lần chạy này ------------------------------
# Mặc định: toàn bộ Sites. Danh sách này KHÔNG gồm MASTER — Master là vai
# trò Publisher, không bao giờ là Subscriber của chính nó.
if ($Subscribers) {
    $subList = @()
    foreach ($s in ($Subscribers -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })) {
        $ma = $s.ToUpperInvariant()
        if ($cfg.Sites -notcontains $ma) {
            throw "Subscriber '$s' khong co trong Sites cua config.ps1 ($($cfg.Sites -join ', '))"
        }
        if ($subList -notcontains $ma) { $subList += $ma }
    }
    if (-not $subList) { throw "Tham so -Subscribers rong." }
} else {
    $subList = @($cfg.Sites)
}

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

    # Subscriber được chọn cho lần chạy này (mặc định: mọi site).
    # Script 32 và 39-go-subscription lọc theo danh sách này.
    "SubList=$($subList -join ',')"

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

# Script SQL duoc luu UTF-8; chi dinh ma hoa de seed tieng Viet khong bi loi
# khi Windows dang dung mot code page khac.
$args = @('-S', $server, '-E', '-b', '-I', '-f', '65001', '-i', $sqlPath)
# Script tao database chay trong master; cac script con lai can dung DB
# ngay luc ket noi (master/ va site/ deu kiem DB_NAME truoc khi tao bang).
$initialDatabase = if ([IO.Path]::GetFileName($sqlPath) -eq '00-create-databases.sql') {
    'master'
} else {
    $cfg.Databases[$On]
}
$args += @('-d', $initialDatabase)
foreach ($v in $vars) {
    $pair = $v -split '=', 2
    # ODBC sqlcmd can tach dau phay thanh argument moi neu gia tri khong
    # co dau nhay. DbList cua may Master + HCM luon co dau phay.
    $args += @('-v', ('{0}="{1}"' -f $pair[0], $pair[1].Replace('"', '""')))
}

Write-Host ""
Write-Host "  Script : $Script"        -ForegroundColor Cyan
Write-Host "  Server : $server"        -ForegroundColor Cyan
Write-Host "  Target : $($cfg.Databases[$On])  ($On)" -ForegroundColor Cyan
Write-Host "  DB tren instance nay : $dbList" -ForegroundColor DarkGray
if ([IO.Path]::GetFileName($sqlPath) -match 'subscription') {
    Write-Host "  Subscriber lan nay   : $($subList -join ', ')" -ForegroundColor DarkGray
}
Write-Host ""

if ($WhatIf) {
    Write-Host "sqlcmd $($args -join ' ')" -ForegroundColor DarkGray
    return
}

# ODBC sqlcmd tren Windows tu phan tich chuoi lenh, can nhan nguyen
# Name="value". Goi truc tiep qua ProcessStartInfo de PowerShell 5.1/7
# khong loai bo hoac escape lai dau nhay. Khong di qua shell khac.
$commandParts = for ($i = 0; $i -lt $args.Count; $i++) {
    if ($i -gt 0 -and $args[$i - 1] -eq '-v') { $args[$i] }
    elseif ($args[$i] -match '\s') { '"' + $args[$i] + '"' }
    else { $args[$i] }
}
$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = (Get-Command sqlcmd -CommandType Application -ErrorAction Stop).Source
$startInfo.Arguments = $commandParts -join ' '
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.StandardOutputEncoding = [Text.Encoding]::UTF8
$startInfo.StandardErrorEncoding = [Text.Encoding]::UTF8
$process = [Diagnostics.Process]::Start($startInfo)
try {
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    if ($stdout.Result) { Write-Host $stdout.Result }
    if ($stderr.Result) { Write-Host $stderr.Result -ForegroundColor Red }
    if ($process.ExitCode -ne 0) {
        throw "sqlcmd that bai voi ma loi $($process.ExitCode)"
    }
}
finally { $process.Dispose() }
Write-Host "  OK" -ForegroundColor Green
