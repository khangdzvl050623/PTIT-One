import type { ReactNode } from 'react'

import { Panel } from '@/shared/ui'
import type { IconName } from '@/shared/ui'

import { IdPhoto } from './IdPhoto'
import { InfoTable } from './InfoTable'
import type { InfoRow } from './InfoTable'
import styles from './ProfilePanel.module.scss'

export interface ProfilePanelProps {
  title: string
  icon?: IconName
  rows: readonly InfoRow[]
  /** Hiện ảnh thẻ mặc định bên phải bảng — cho hồ sơ người (SV, GV). */
  photo?: boolean
  /** Nội dung thêm dưới bảng. */
  children?: ReactNode
}

/** Khung hồ sơ: bảng nhãn – giá trị, tuỳ chọn kèm ảnh thẻ. */
export function ProfilePanel({ title, icon = 'user', rows, photo = false, children }: ProfilePanelProps) {
  return (
    <Panel title={title} icon={icon}>
      <div className={photo ? styles.withPhoto : undefined}>
        <InfoTable rows={rows} />
        {photo ? <IdPhoto /> : null}
      </div>
      {children}
    </Panel>
  )
}
