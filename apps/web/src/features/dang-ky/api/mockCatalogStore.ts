import { CATALOG, COURSE_NAMES, PREREQUISITES } from '../data/demo'
import type { CourseRelation, CourseSummary } from '../types'

const STORAGE_KEY = 'ptitone:mock:danh-muc-mon-hoc'

export interface MockCatalogData {
  courses: CourseSummary[]
  relations: CourseRelation[]
}

function initialCatalog(): MockCatalogData {
  const name = (code: string) => CATALOG.find((course) => course.maMonHoc === code)?.tenMonHoc ?? COURSE_NAMES[code] ?? code
  const relations = Object.entries(PREREQUISITES).flatMap(([maMonHoc, required]) =>
    required.map((maMonYeuCau) => ({
      loai: 'TIEN_QUYET' as const,
      maMonHoc,
      tenMonHoc: name(maMonHoc),
      maMonYeuCau,
      tenMonYeuCau: name(maMonYeuCau),
    })),
  )
  return { courses: mergeCatalog(CATALOG, relations), relations }
}

function mergeCatalog(courses: readonly CourseSummary[], relations: readonly CourseRelation[]): CourseSummary[] {
  const byCode = new Map(courses.map((course) => [course.maMonHoc, { ...course }]))
  for (const relation of relations) {
    for (const [maMonHoc, tenMonHoc] of [
      [relation.maMonHoc, relation.tenMonHoc],
      [relation.maMonYeuCau, relation.tenMonYeuCau],
    ]) {
      if (!byCode.has(maMonHoc)) {
        byCode.set(maMonHoc, { maMonHoc, tenMonHoc, soTinChi: 0, maKhoa: '', tenKhoa: 'Chưa cập nhật' })
      }
    }
  }
  return [...byCode.values()].sort((a, b) => a.maMonHoc.localeCompare(b.maMonHoc))
}

function isCourse(value: unknown): value is CourseSummary {
  if (!value || typeof value !== 'object') return false
  const course = value as Record<string, unknown>
  return typeof course.maMonHoc === 'string' && typeof course.tenMonHoc === 'string' &&
    typeof course.soTinChi === 'number' && typeof course.maKhoa === 'string' && typeof course.tenKhoa === 'string'
}

function isRelation(value: unknown): value is CourseRelation {
  if (!value || typeof value !== 'object') return false
  const relation = value as Record<string, unknown>
  return relation.loai === 'TIEN_QUYET' && typeof relation.maMonHoc === 'string' &&
    typeof relation.tenMonHoc === 'string' && typeof relation.maMonYeuCau === 'string' &&
    typeof relation.tenMonYeuCau === 'string'
}

function readSavedCatalog(): MockCatalogData | null {
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (raw === null) return null
  let value: unknown
  try {
    value = JSON.parse(raw)
  } catch {
    throw new Error('Dữ liệu demo danh mục bị lỗi. Hãy khôi phục dữ liệu demo.')
  }
  if (!value || typeof value !== 'object') throw new Error('Dữ liệu demo danh mục không hợp lệ. Hãy khôi phục dữ liệu demo.')
  const saved = value as Record<string, unknown>
  if (!Array.isArray(saved.courses) || !Array.isArray(saved.relations) ||
      !saved.courses.every(isCourse) || !saved.relations.every(isRelation)) {
    throw new Error('Dữ liệu demo danh mục không hợp lệ. Hãy khôi phục dữ liệu demo.')
  }
  return { courses: mergeCatalog(saved.courses, saved.relations), relations: saved.relations }
}

/** Nguồn mock dùng chung cho quản trị danh mục, xem tiên quyết và đăng ký. */
export function readMockCatalog(): MockCatalogData {
  return readSavedCatalog() ?? initialCatalog()
}

export function saveMockCatalog(data: MockCatalogData): void {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export function resetMockCatalog(): void {
  window.localStorage.removeItem(STORAGE_KEY)
}
