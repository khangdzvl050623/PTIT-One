import styles from './InfoTable.module.scss'

export interface InfoRow {
  label: string
  /** `null`/rỗng hiện dấu gạch — không bịa giá trị cho trường chưa có. */
  value: string | number | null
}

export interface InfoTableProps {
  rows: readonly InfoRow[]
}

/** Bảng nhãn – giá trị hai cột như phiếu thông tin của cổng đào tạo. */
export function InfoTable({ rows }: InfoTableProps) {
  return (
    <dl className={styles.table}>
      {rows.map((row) => (
        <div key={row.label} className={styles.row}>
          <dt className={styles.label}>{row.label}</dt>
          <dd className={styles.value}>
            {row.value === null || row.value === '' ? '—' : row.value}
          </dd>
        </div>
      ))}
    </dl>
  )
}
