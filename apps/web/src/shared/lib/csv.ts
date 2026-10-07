export type CsvCell = string | number | null

function cell(value: CsvCell): string {
  const text = value === null ? '' : String(value)
  // Bọc nháy khi có dấu phẩy, nháy hoặc xuống dòng — đúng chuẩn CSV.
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

/**
 * Tải bảng dạng CSV (nút "Xuất Excel"). Có BOM UTF-8 để Excel đọc đúng tiếng
 * Việt khi mở trực tiếp — không có BOM Excel đoán sai bảng mã, ra chữ lỗi.
 * `null` ra ô trống, không bao giờ thành 0.
 */
export function downloadCsv(rows: readonly (readonly CsvCell[])[], fileName: string): void {
  const csv = rows.map((r) => r.map(cell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob(['﻿', csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}
