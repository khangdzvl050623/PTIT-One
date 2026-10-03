import { Panel } from '@/shared/ui'

import styles from './RightsPanel.module.scss'

export interface RightsPanelProps {
  rights: readonly string[]
}

/** Phạm vi quyền của tài khoản quản trị — server vẫn kiểm ở mỗi request. */
export function RightsPanel({ rights }: RightsPanelProps) {
  return (
    <Panel title="Phạm vi quản trị" icon="gear">
      <ul className={styles.list}>
        {rights.map((r) => (
          <li key={r}>{r}</li>
        ))}
      </ul>
    </Panel>
  )
}
