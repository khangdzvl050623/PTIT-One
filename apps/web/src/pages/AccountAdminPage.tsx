import { useState } from 'react'

import { AccountDirectory, ProvisionPanel } from '@/features/danh-muc'
import { Panel } from '@/shared/ui'

import styles from './AccountAdminPage.module.scss'

/**
 * Hồ sơ và tài khoản (F02) — chỉ `ADMIN_MASTER`. Đang chạy bản giả
 * `features/danh-muc/api/mockDirectoryApi`: cùng thứ tự kiểm và cùng mã lỗi
 * với `POST /api/students`, `POST /api/teachers`, `GET /api/accounts`,
 * `POST /api/accounts/{tenDangNhap}/activation-code` và
 * `PUT /api/accounts/{tenDangNhap}/status`.
 *
 * Cấp tài khoản **chỉ ở Master** (chốt 02/10/2026): danh bạ là bảng Master sở
 * hữu, Admin cơ sở chỉ đọc — nên không có màn tương đương cho vai đó.
 *
 * Hai cột: danh bạ bên trái, khung cấp hồ sơ bên phải. Hai khung không gọi
 * thẳng vào nhau được, nên trang giữ `version` và tăng lên sau mỗi lần cấp để
 * danh bạ tải lại.
 */
export function AccountAdminPage() {
  const [version, setVersion] = useState(0)

  return (
    <div className={styles.columns}>
      <div className={styles.main}>
        <Panel title="DANH BẠ TÀI KHOẢN" icon="users">
          <AccountDirectory demo reloadKey={version} />
        </Panel>
      </div>

      <aside className={styles.aside}>
        <Panel title="CẤP HỒ SƠ VÀ TÀI KHOẢN" icon="user">
          <ProvisionPanel onCreated={() => setVersion((v) => v + 1)} />
        </Panel>
      </aside>
    </div>
  )
}
