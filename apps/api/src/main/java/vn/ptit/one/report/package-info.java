/**
 * Thống kê học vụ — CHỈ ĐỌC, không sở hữu bảng nào.
 *
 * <p>Đọc tổng hợp {@code LopHocPhan}, {@code DangKyHocPhan}, {@code Diem} bằng
 * truy vấn gộp; không ghi gì. Không module nào phụ thuộc vào {@code report}.
 * Ngưỡng đạt lấy từ {@code GradePolicy} của module {@code grade} để thống kê và
 * bảng điểm không bao giờ tính khác nhau.
 *
 * <p>Cách tính (quyết định nhóm 02/10/2026):
 * <ul>
 *   <li>lớp và điểm theo cơ sở MỞ lớp (Host);</li>
 *   <li>chỉ tính lớp {@code MO} và {@code DA_KHOA} — bỏ lớp dự kiến và đã huỷ;</li>
 *   <li>lấp đầy = tổng đã đăng ký / tổng sức chứa trên cùng tập lớp, KHÔNG lấy
 *       trung bình phần trăm từng lớp;</li>
 *   <li>đạt/trượt chỉ tính điểm đã công bố; thiếu điểm là "chưa có kết quả";</li>
 *   <li>lượt đăng ký khác số sinh viên: một người học sáu môn là sáu lượt.</li>
 * </ul>
 *
 * <p>Phần 2: đây là chỗ đặt port {@code GlobalReport} — thay truy vấn một DB
 * bằng truy vấn phân tán, giữ nguyên hình dạng response.
 */
package vn.ptit.one.report;
