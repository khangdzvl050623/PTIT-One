import { useEffect, useMemo, useState } from 'react'

import type { CourseRelation, CourseSummary } from '@/features/dang-ky/types'
import { Dialog, Icon, Select } from '@/shared/ui'

import * as api from '../api/catalogApi'
import styles from './CatalogManager.module.scss'

type Tab = 'courses' | 'prerequisites'
type DialogKind = 'course' | 'prerequisite' | 'delete' | 'reset'
type CourseForm = Pick<CourseSummary, 'maMonHoc' | 'tenMonHoc' | 'soTinChi' | 'maKhoa' | 'tenKhoa'>

const EMPTY_FORM: CourseForm = {
  maMonHoc: '',
  tenMonHoc: '',
  soTinChi: 3,
  maKhoa: '',
  tenKhoa: '',
}

function fold(value: string): string {
  return value.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
}

function createsCycle(
  courseCode: string,
  prerequisiteCode: string,
  relations: readonly CourseRelation[],
): boolean {
  const pending = [prerequisiteCode]
  const visited = new Set<string>()
  while (pending.length) {
    const current = pending.pop()!
    if (current === courseCode) return true
    if (visited.has(current)) continue
    visited.add(current)
    for (const relation of relations) {
      if (relation.maMonHoc === current) pending.push(relation.maMonYeuCau)
    }
  }
  return false
}

/** Màn quản lý danh mục và tiên quyết dùng mock API có lưu localStorage. */
export function CatalogManager() {
  const [catalog, setCatalog] = useState<CourseSummary[] | null>(null)
  const [relations, setRelations] = useState<CourseRelation[]>([])
  const [failed, setFailed] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [tab, setTab] = useState<Tab>('courses')
  const [query, setQuery] = useState('')
  const [selectedCode, setSelectedCode] = useState('')
  const [dialog, setDialog] = useState<DialogKind | null>(null)
  const [editingCode, setEditingCode] = useState<string | null>(null)
  const [deletingCourse, setDeletingCourse] = useState<CourseSummary | null>(null)
  const [courseForm, setCourseForm] = useState<CourseForm>(EMPTY_FORM)
  const [formError, setFormError] = useState<string | null>(null)
  const [prerequisiteError, setPrerequisiteError] = useState<string | null>(null)
  const [prerequisiteCode, setPrerequisiteCode] = useState('')
  const [notice, setNotice] = useState<{ text: string; tone: 'success' | 'error' } | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    api.loadCatalog().then(
      (data) => {
        if (cancelled) return
        setCatalog(data.courses)
        setRelations(data.relations)
        setSelectedCode(data.courses[0]?.maMonHoc ?? '')
      },
      (cause: unknown) => {
        if (!cancelled) {
          setFailed(true)
          setLoadError(cause instanceof Error ? cause.message : 'Không tải được danh mục môn học.')
        }
      },
    )
    return () => {
      cancelled = true
    }
  }, [])

  const courses = catalog ?? []
  const visibleCourses = useMemo(() => {
    const q = fold(query.trim())
    return courses.filter((course) => !q || fold(`${course.maMonHoc} ${course.tenMonHoc} ${course.tenKhoa}`).includes(q))
  }, [courses, query])
  const currentCourse = courses.find((course) => course.maMonHoc === selectedCode)
  const courseMap = useMemo(() => new Map(courses.map((course) => [course.maMonHoc, course])), [courses])
  const currentRelations = relations.filter((relation) => relation.maMonHoc === selectedCode)
  const choices = courses
    .filter(
      (course) =>
        course.maMonHoc !== selectedCode &&
        !currentRelations.some((relation) => relation.maMonYeuCau === course.maMonHoc),
    )
    .map((course) => ({ value: course.maMonHoc, label: `${course.maMonHoc} · ${course.tenMonHoc}` }))

  function openCourseForm(course?: CourseSummary) {
    setEditingCode(course?.maMonHoc ?? null)
    setCourseForm(course ? { ...course } : EMPTY_FORM)
    setFormError(null)
    setDialog('course')
  }

  /**
   * Chạy một thao tác ghi rồi TẢI LẠI danh mục từ server.
   *
   * Không tự suy đoán trạng thái mới ở client: API thật làm việc theo từng
   * thực thể và còn có ràng buộc mà client không thấy (môn đã có lớp, đang
   * trong CTĐT). Tải lại thì màn hình luôn khớp server.
   */
  async function commit(run: () => Promise<unknown>, message: string): Promise<boolean> {
    setSaving(true)
    setNotice(null)
    try {
      await run()
      const data = await api.loadCatalog()
      setCatalog(data.courses)
      setRelations(data.relations)
      setNotice({ text: message, tone: 'success' })
      return true
    } catch (cause) {
      setNotice({
        text: cause instanceof Error ? cause.message : 'Không lưu được thay đổi.',
        tone: 'error',
      })
      return false
    } finally {
      setSaving(false)
    }
  }

  async function saveCourse() {
    const next = {
      ...courseForm,
      maMonHoc: courseForm.maMonHoc.trim().toUpperCase(),
      tenMonHoc: courseForm.tenMonHoc.trim(),
      maKhoa: courseForm.maKhoa.trim().toUpperCase(),
      tenKhoa: courseForm.tenKhoa.trim(),
    }
    if (!next.maMonHoc || !next.tenMonHoc || !next.maKhoa || !next.tenKhoa) {
      setFormError('Vui lòng nhập đầy đủ thông tin môn học và khoa quản lý.')
      return
    }
    if (!Number.isInteger(next.soTinChi) || next.soTinChi < 1 || next.soTinChi > 10) {
      setFormError('Số tín chỉ phải là số nguyên từ 1 đến 10.')
      return
    }
    if (
      next.maMonHoc !== editingCode &&
      courses.some((course) => course.maMonHoc === next.maMonHoc)
    ) {
      setFormError(`Mã môn ${next.maMonHoc} đã tồn tại.`)
      return
    }
    const saved = await commit(
      () =>
        api.saveCourse(
          {
            maMonHoc: next.maMonHoc,
            tenMonHoc: next.tenMonHoc,
            soTinChi: next.soTinChi,
            maKhoa: next.maKhoa,
          },
          editingCode === null,
        ),
      editingCode ? `Đã cập nhật môn ${next.maMonHoc}.` : `Đã thêm môn ${next.maMonHoc}.`,
    )
    if (!saved) return
    setSelectedCode(next.maMonHoc)
    setFormError(null)
    setDialog(null)
  }

  async function addPrerequisite() {
    if (!currentCourse || !prerequisiteCode) return
    const prerequisite = courseMap.get(prerequisiteCode)
    if (!prerequisite || prerequisiteCode === currentCourse.maMonHoc) return
    if (createsCycle(currentCourse.maMonHoc, prerequisite.maMonHoc, relations)) {
      setPrerequisiteError('Điều kiện này tạo thành vòng lặp tiên quyết.')
      return
    }
    /* API thay TOÀN BỘ tập tiên quyết của một môn, không thêm từng cạnh — nên
       gửi tập hiện có cộng môn mới. */
    const saved = await commit(
      () => api.setPrerequisites(currentCourse.maMonHoc, [...prerequisitesOf(currentCourse.maMonHoc), prerequisite.maMonHoc]),
      `Đã thêm ${prerequisite.maMonHoc} làm môn tiên quyết cho ${currentCourse.maMonHoc}.`,
    )
    if (!saved) return
    setDialog(null)
    setPrerequisiteCode('')
    setPrerequisiteError(null)
  }

  async function removePrerequisite(code: string) {
    await commit(
      () =>
        api.setPrerequisites(
          selectedCode,
          prerequisitesOf(selectedCode).filter((ma) => ma !== code),
        ),
      `Đã gỡ ${code} khỏi tiên quyết của ${selectedCode}.`,
    )
  }

  /** Tập tiên quyết hiện tại của một môn, dạng mã. */
  function prerequisitesOf(maMonHoc: string): string[] {
    return relations.filter((r) => r.maMonHoc === maMonHoc).map((r) => r.maMonYeuCau)
  }

  async function confirmDeleteCourse() {
    if (!deletingCourse) return
    const ma = deletingCourse.maMonHoc
    /* Server quyết được xoá hay không (môn là tiên quyết của môn khác, đã có
       lớp, hay đang trong CTĐT) — client không đoán trước, chỉ hiện lý do. */
    const saved = await commit(() => api.deleteCourse(ma), `Đã xoá môn ${ma} khỏi danh mục.`)
    if (!saved) return
    if (selectedCode === ma) setSelectedCode(courses.find((c) => c.maMonHoc !== ma)?.maMonHoc ?? '')
    setDeletingCourse(null)
    setDialog(null)
  }

  async function confirmReset() {
    setSaving(true)
    setNotice(null)
    try {
      api.resetCatalog()
      const data = await api.loadCatalog()
      setCatalog(data.courses)
      setRelations(data.relations)
      setSelectedCode(data.courses[0]?.maMonHoc ?? '')
      setFailed(false)
      setLoadError(null)
      setNotice({ text: 'Đã khôi phục danh mục ban đầu.', tone: 'success' })
      setDialog(null)
    } catch (cause) {
      setNotice({
        text: cause instanceof Error ? cause.message : 'Không khôi phục được danh mục.',
        tone: 'error',
      })
      setLoadError(cause instanceof Error ? cause.message : 'Không khôi phục được danh mục.')
    } finally {
      setSaving(false)
    }
  }

  if (failed) {
    return (
      <div className={styles.loadFailure}>
        <p className={styles.muted} role="alert">{loadError ?? 'Không tải được danh mục môn học.'}</p>
        <button type="button" className={styles.primary} onClick={() => void confirmReset()} disabled={saving}>
          Khôi phục danh mục ban đầu
        </button>
      </div>
    )
  }
  if (!catalog) return <p className={styles.muted}>Đang tải danh mục môn học…</p>

  return (
    <div className={styles.block}>
      <div className={styles.tabHeader}>
        <div className={styles.tabs} role="tablist" aria-label="Danh mục môn học và tiên quyết">
          <button type="button" role="tab" aria-selected={tab === 'courses'} onClick={() => setTab('courses')}>
            Danh mục môn học <span>{courses.length}</span>
          </button>
          <button type="button" role="tab" aria-selected={tab === 'prerequisites'} onClick={() => setTab('prerequisites')}>
            Môn tiên quyết <span>{relations.length}</span>
          </button>
        </div>
        <button
          type="button"
          className={`${styles.primary} ${styles.resetLink}`}
          onClick={() => setDialog('reset')}
          disabled={saving}
        >
          Khôi phục ban đầu
        </button>
      </div>

      {notice ? (
        <p className={`${styles.notice} ${notice.tone === 'error' ? styles.errorNotice : ''}`} role={notice.tone === 'error' ? 'alert' : 'status'}>
          {notice.text}
          <button type="button" onClick={() => setNotice(null)} aria-label="Đóng thông báo">×</button>
        </p>
      ) : null}

      {tab === 'courses' ? (
        <>
          <div className={styles.toolbar}>
            <label className={styles.search}>
              <Icon name="search" size="14px" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm theo mã, tên môn hoặc khoa…"
                aria-label="Tìm môn học"
              />
            </label>
            <button type="button" className={styles.primary} onClick={() => openCourseForm()} disabled={saving}>
              + Thêm môn học
            </button>
          </div>
          <p className={styles.summary}>{visibleCourses.length} / {courses.length} môn học</p>
          <div className={styles.scroll}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Mã môn</th>
                  <th className={styles.left}>Tên môn học</th>
                  <th>Khoa quản lý</th>
                  <th>Số tín chỉ</th>
                  <th>Tiên quyết</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {visibleCourses.length === 0 ? (
                  <tr><td colSpan={6} className={styles.empty}>Không tìm thấy môn học</td></tr>
                ) : visibleCourses.map((course) => (
                  <tr key={course.maMonHoc}>
                    <td className={styles.code}>{course.maMonHoc}</td>
                    <td>{course.tenMonHoc}</td>
                    <td>{course.tenKhoa}</td>
                    <td className={styles.center}>{course.soTinChi || '—'}</td>
                    <td className={styles.center}>{relations.filter((relation) => relation.maMonHoc === course.maMonHoc).length}</td>
                    <td className={styles.actions}>
                      <button type="button" className={styles.linkButton} onClick={() => {
                        setSelectedCode(course.maMonHoc)
                        setTab('prerequisites')
                      }} disabled={saving}>Tiên quyết</button>
                      <button type="button" className={styles.linkButton} onClick={() => openCourseForm(course)} disabled={saving}>Sửa</button>
                      <button type="button" className={styles.remove} onClick={() => {
                        setDeletingCourse(course)
                        setDialog('delete')
                      }} disabled={saving || courses.length < 2}>Xoá</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <>
          <div className={styles.toolbar}>
            <Select
              ariaLabel="Chọn môn cần thiết lập tiên quyết"
              className={styles.courseSelect}
              value={selectedCode}
              options={courses.map((course) => ({ value: course.maMonHoc, label: `${course.maMonHoc} · ${course.tenMonHoc}` }))}
              onChange={setSelectedCode}
            />
            <button type="button" className={styles.primary} onClick={() => {
              setPrerequisiteCode(choices[0]?.value ?? '')
              setPrerequisiteError(null)
              setDialog('prerequisite')
            }} disabled={choices.length === 0 || saving}>
              + Thêm tiên quyết
            </button>
          </div>
          {currentCourse ? (
            <div className={styles.courseSummary}>
              <span className={styles.courseCode}>{currentCourse.maMonHoc}</span>
              <span><b>{currentCourse.tenMonHoc}</b><small>{currentCourse.tenKhoa} · {currentCourse.soTinChi || '—'} tín chỉ</small></span>
            </div>
          ) : null}
          <p className={styles.summary}>{currentRelations.length} môn tiên quyết</p>
          <div className={styles.scroll}>
            <table className={styles.table}>
              <thead><tr><th>Mã môn tiên quyết</th><th className={styles.left}>Tên môn học</th><th>Thao tác</th></tr></thead>
              <tbody>
                {currentRelations.length === 0 ? (
                  <tr><td colSpan={3} className={styles.empty}>Môn học chưa có điều kiện tiên quyết</td></tr>
                ) : currentRelations.map((relation) => (
                  <tr key={relation.maMonYeuCau}>
                    <td className={styles.code}>{relation.maMonYeuCau}</td>
                    <td>{relation.tenMonYeuCau}</td>
                    <td className={styles.actions}>
                      <button type="button" className={styles.remove} onClick={() => void removePrerequisite(relation.maMonYeuCau)} disabled={saving}>Gỡ</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className={styles.hint}>Sinh viên cần đạt các môn trong danh sách này trước khi đăng ký môn học.</p>
        </>
      )}

      <Dialog
        open={dialog === 'course'}
        onClose={() => setDialog(null)}
        ariaLabel={editingCode ? 'Sửa thông tin môn học' : 'Thêm môn học'}
        header={<b>{editingCode ? 'SỬA THÔNG TIN MÔN HỌC' : 'THÊM MÔN HỌC'}</b>}
        footer={
          <>
            <button type="button" className={styles.ghost} onClick={() => setDialog(null)}>Huỷ</button>
            <button type="button" className={styles.primary} onClick={() => void saveCourse()} disabled={saving}>
              {saving ? 'Đang lưu…' : 'Lưu'}
            </button>
          </>
        }
      >
        <div className={styles.form}>
          <label>Mã môn học
            <input required value={courseForm.maMonHoc} disabled={Boolean(editingCode)} onChange={(event) => setCourseForm({ ...courseForm, maMonHoc: event.target.value })} placeholder="Ví dụ: INT1401" />
          </label>
          <label>Tên môn học
            <input required value={courseForm.tenMonHoc} onChange={(event) => setCourseForm({ ...courseForm, tenMonHoc: event.target.value })} placeholder="Nhập tên môn học" />
          </label>
          <label>Khoa quản lý
            <input required value={courseForm.tenKhoa} onChange={(event) => setCourseForm({ ...courseForm, tenKhoa: event.target.value })} placeholder="Nhập khoa quản lý" />
          </label>
          <label>Mã khoa
            <input required value={courseForm.maKhoa} onChange={(event) => setCourseForm({ ...courseForm, maKhoa: event.target.value })} placeholder="Ví dụ: CNTT" />
          </label>
          <label>Số tín chỉ
            <input required type="number" min={1} max={10} step={1} value={courseForm.soTinChi || ''} onChange={(event) => setCourseForm({ ...courseForm, soTinChi: Number(event.target.value) })} />
          </label>
          {formError ? <p className={styles.formError} role="alert">{formError}</p> : null}
        </div>
      </Dialog>

      <Dialog
        open={dialog === 'prerequisite'}
        onClose={() => setDialog(null)}
        ariaLabel="Thêm môn tiên quyết"
        header={<b>THÊM MÔN TIÊN QUYẾT</b>}
        footer={
          <>
            <button type="button" className={styles.ghost} onClick={() => setDialog(null)}>Huỷ</button>
            <button type="button" className={styles.primary} onClick={() => void addPrerequisite()} disabled={!prerequisiteCode || saving}>
              {saving ? 'Đang lưu…' : 'Thêm điều kiện'}
            </button>
          </>
        }
      >
        <div className={styles.form}>
          <p className={styles.dialogText}>Chọn môn học phải hoàn thành trước <b>{currentCourse?.maMonHoc} · {currentCourse?.tenMonHoc}</b>.</p>
          {choices.length ? (
            <Select ariaLabel="Chọn môn tiên quyết" value={prerequisiteCode} options={choices} onChange={setPrerequisiteCode} />
          ) : <p className={styles.muted}>Không còn môn phù hợp để thêm.</p>}
          {prerequisiteError ? <p className={styles.formError} role="alert">{prerequisiteError}</p> : null}
        </div>
      </Dialog>

      <Dialog
        open={dialog === 'delete'}
        onClose={() => setDialog(null)}
        ariaLabel="Xoá môn học"
        header={<b>XOÁ MÔN HỌC</b>}
        footer={
          <>
            <button type="button" className={styles.ghost} onClick={() => setDialog(null)}>Huỷ</button>
            <button type="button" className={styles.remove} onClick={() => void confirmDeleteCourse()} disabled={saving}>
              {saving ? 'Đang xoá…' : 'Xoá môn học'}
            </button>
          </>
        }
      >
        <p className={styles.dialogText}>
          Môn học <b>{deletingCourse?.maMonHoc} - {deletingCourse?.tenMonHoc}</b> sẽ bị xoá khỏi
          danh mục môn học.
        </p>
        {/* Nói trước điều kiện thay vì để người dùng bấm rồi mới gặp 409. */}
        <p className={styles.dialogText}>
          Nếu môn học đã được sử dụng trong lớp học phần hoặc dữ liệu học tập, hệ thống sẽ không
          cho phép xoá.
        </p>
      </Dialog>

      <Dialog
        open={dialog === 'reset'}
        onClose={() => setDialog(null)}
        ariaLabel="Khôi phục danh mục ban đầu"
        header={<b>KHÔI PHỤC DANH MỤC BAN ĐẦU</b>}
        footer={
          <>
            <button type="button" className={styles.ghost} onClick={() => setDialog(null)}>Huỷ</button>
            <button type="button" className={styles.remove} onClick={() => void confirmReset()} disabled={saving}>
              {saving ? 'Đang khôi phục…' : 'Khôi phục'}
            </button>
          </>
        }
      >
        <p className={styles.dialogText}>Thay toàn bộ dữ liệu đã chỉnh trên trình duyệt này bằng danh mục ban đầu.</p>
      </Dialog>
    </div>
  )
}
