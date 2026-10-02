package vn.ptit.one.timetable.model;

/**
 * Một buổi học trong tuần của lớp. {@code thu}: 2 = thứ Hai … 8 = Chủ nhật.
 * Tuần tính từ {@code HocKy.NgayBatDau}.
 */
public record ScheduleSlot(
        int thu,
        int tietBatDau,
        int soTiet,
        String phongHoc,
        int tuanBatDau,
        int tuanKetThuc) {

    /** Tiết cuối cùng (bao gồm). Dùng để xét chồng tiết. */
    public int tietKetThuc() {
        return tietBatDau + soTiet - 1;
    }

    public boolean trungTiet(ScheduleSlot khac) {
        return thu == khac.thu
                && tietBatDau <= khac.tietKetThuc() && khac.tietBatDau <= tietKetThuc();
    }

    public boolean trungTuan(ScheduleSlot khac) {
        return tuanBatDau <= khac.tuanKetThuc && khac.tuanBatDau <= tuanKetThuc;
    }
}
