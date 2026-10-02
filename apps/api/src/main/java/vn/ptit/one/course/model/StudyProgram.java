package vn.ptit.one.course.model;

/** Chương trình đào tạo. Sinh viên thuộc đúng một CTĐT ({@code SinhVien.MaCTDT}). */
public record StudyProgram(
        String maCTDT,
        String tenCTDT,
        String maKhoa,
        int tongTinChi) {
}
