import { CatalogManager } from '@/features/danh-muc'
import { Panel } from '@/shared/ui'

/** Danh mục môn học và tiên quyết — chỉ dành cho ADMIN_MASTER (F03). */
export function CatalogAdminPage() {
  return (
    <Panel title="DANH MỤC MÔN HỌC VÀ TIÊN QUYẾT" icon="book">
      <CatalogManager />
    </Panel>
  )
}
