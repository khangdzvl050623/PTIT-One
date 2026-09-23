import { LoginForm } from '@/features/auth'
import {
  NOTICES,
  NoticeList,
  NoticeSpotlight,
  SPOTLIGHT_NOTICE,
  TUITION_NOTICES,
} from '@/features/thong-bao'
import { AccessStats } from '@/features/thong-ke'
import { LABELS } from '@/shared/constants'
import { Panel } from '@/shared/ui'

import styles from './HomePage.module.scss'

export function HomePage() {
  return (
    <div className={styles.page}>
      <div className={styles.columns}>
        <div className={styles.mainColumn}>
          <Panel title={LABELS.notices}>
            <div className={styles.notices}>
              <NoticeSpotlight notice={SPOTLIGHT_NOTICE} />
              <NoticeList notices={NOTICES} />
            </div>
          </Panel>

          <Panel title={LABELS.tuition}>
            <NoticeList notices={TUITION_NOTICES} />
          </Panel>
        </div>

        <aside className={styles.asideColumn}>
          <Panel title={LABELS.login} icon="user">
            <LoginForm />
          </Panel>
        </aside>
      </div>

      <div className={styles.stats}>
        <AccessStats />
      </div>
    </div>
  )
}
