/**
 * Kho ảnh ngoài (Cloudinary). Kỹ thuật dùng chung: nhận byte ảnh, trả URL —
 * không biết ảnh đó là của sinh viên hay của ai, module nghiệp vụ tự quyết.
 *
 * <p>DB chỉ lưu URL, không lưu byte. Ảnh đại diện không phải dữ liệu nghiệp vụ,
 * và Phần 2 phân mảnh `SinhVien` nên cột nhị phân sẽ làm phình mọi snapshot.
 */
package vn.ptit.one.shared.media;
