import { readMockCatalog, resetMockCatalog, saveMockCatalog } from '@/features/dang-ky/api/mockCatalogStore'

export { resetMockCatalog, saveMockCatalog }

/** Đọc cùng danh mục môn và tiên quyết mà các màn đăng ký sử dụng. */
export async function loadMockCatalog() {
  return readMockCatalog()
}
