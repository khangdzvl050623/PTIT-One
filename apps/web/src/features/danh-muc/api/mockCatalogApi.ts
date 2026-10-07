import { ApiError } from '@/shared/api'

import {
  readMockCatalog,
  resetMockCatalog,
  saveMockCatalog,
} from '@/features/dang-ky/api/mockCatalogStore'

import type { CourseRelation } from '@/features/dang-ky/types'
import type { Catalog, CourseInput } from './httpCatalogApi'

/**
 * Danh mục giả — cùng chữ ký **theo từng thao tác** với `httpCatalogApi`, và
 * ném cùng mã lỗi như server.
 *
 * Dùng chung store với các màn đăng ký (`mockCatalogStore`) để sửa danh mục ở
 * đây thì màn đăng ký thấy ngay — giống hệt khi chạy API thật.
 */

const LATENCY_MS = 250

export async function loadCatalog(): Promise<Catalog> {
  await delay()
  const data = readMockCatalog()
  return { courses: [...data.courses], relations: [...data.relations] }
}

export async function saveCourse(input: CourseInput, moi: boolean): Promise<unknown> {
  await delay()
  const data = readMockCatalog()
  const existing = data.courses.find((c) => c.maMonHoc === input.maMonHoc)

  if (moi && existing) {
    throw new ApiError(409, {
      code: 'COURSE_DUPLICATE',
      message: `Mã môn học ${input.maMonHoc} đã tồn tại.`,
    })
  }
  if (!moi && !existing) {
    throw notFound(input.maMonHoc)
  }

  const row = { ...input, tenKhoa: existing?.tenKhoa ?? input.maKhoa }
  const courses = moi
    ? [...data.courses, row]
    : data.courses.map((c) => (c.maMonHoc === input.maMonHoc ? { ...c, ...row } : c))
  saveMockCatalog({ courses: sorted(courses), relations: data.relations })
  return row
}

export async function setPrerequisites(maMonHoc: string, tienQuyet: string[]): Promise<unknown> {
  await delay()
  const data = readMockCatalog()
  const name = (code: string) =>
    data.courses.find((c) => c.maMonHoc === code)?.tenMonHoc ?? code

  if (!data.courses.some((c) => c.maMonHoc === maMonHoc)) {
    throw notFound(maMonHoc)
  }
  for (const code of tienQuyet) {
    if (code === maMonHoc) {
      throw new ApiError(400, {
        code: 'PREREQUISITE_SELF',
        message: 'Môn học không thể là tiên quyết của chính nó.',
      })
    }
    if (!data.courses.some((c) => c.maMonHoc === code)) {
      throw new ApiError(400, {
        code: 'PREREQUISITE_UNKNOWN',
        message: `Không có môn học ${code}.`,
      })
    }
  }

  /* Chu trình: thêm A → B tạo chu trình khi B đã đi tới được A. Xét trên đồ thị
     ĐÃ BỎ cạnh cũ của chính môn này, đúng như server làm. */
  const others = data.relations.filter((r) => r.maMonHoc !== maMonHoc)
  for (const code of tienQuyet) {
    if (reaches(others, code, maMonHoc)) {
      throw new ApiError(409, {
        code: 'PREREQUISITE_CYCLE',
        message: `Thêm ${code} làm tiên quyết của ${maMonHoc} sẽ tạo chu trình.`,
      })
    }
  }

  const relations: CourseRelation[] = [
    ...others,
    ...tienQuyet.map((code) => ({
      loai: 'TIEN_QUYET' as const,
      maMonHoc,
      tenMonHoc: name(maMonHoc),
      maMonYeuCau: code,
      tenMonYeuCau: name(code),
    })),
  ]
  saveMockCatalog({ courses: data.courses, relations })
  return relations
}

export async function deleteCourse(maMonHoc: string): Promise<void> {
  await delay()
  const data = readMockCatalog()
  if (!data.courses.some((c) => c.maMonHoc === maMonHoc)) {
    throw notFound(maMonHoc)
  }

  /* Cùng cổng chặn đầu tiên của server. Hai cổng kia (đã có lớp, nằm trong
     CTĐT) bản giả không kiểm được vì store này chỉ giữ môn và tiên quyết —
     chạy API thật mới thấy đủ ba. */
  if (data.relations.some((r) => r.maMonYeuCau === maMonHoc)) {
    throw new ApiError(409, {
      code: 'COURSE_IS_PREREQUISITE',
      message: `Môn ${maMonHoc} đang là môn tiên quyết của môn khác. `
        + 'Gỡ nó khỏi các môn đó trước khi xoá.',
    })
  }

  saveMockCatalog({
    courses: data.courses.filter((c) => c.maMonHoc !== maMonHoc),
    relations: data.relations.filter((r) => r.maMonHoc !== maMonHoc),
  })
}

export function resetCatalog(): void {
  resetMockCatalog()
}

/** `from` có đi tới được `target` qua các cạnh tiên quyết hay không. */
function reaches(relations: readonly CourseRelation[], from: string, target: string): boolean {
  const seen = new Set<string>()
  const stack = [from]
  while (stack.length > 0) {
    const current = stack.pop()!
    if (current === target) return true
    if (seen.has(current)) continue
    seen.add(current)
    for (const r of relations) {
      if (r.maMonHoc === current) stack.push(r.maMonYeuCau)
    }
  }
  return false
}

function sorted<T extends { maMonHoc: string }>(rows: T[]): T[] {
  return [...rows].sort((a, b) => a.maMonHoc.localeCompare(b.maMonHoc))
}

function notFound(maMonHoc: string): ApiError {
  return new ApiError(404, {
    code: 'COURSE_NOT_FOUND',
    message: `Không tìm thấy môn học ${maMonHoc}.`,
  })
}

function delay(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, LATENCY_MS))
}
