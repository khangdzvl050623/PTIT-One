package vn.ptit.one.course.model;

import java.time.LocalDate;

/** Học kỳ dùng chung toàn trường. `ngayBatDau` là mốc suy ra số tuần cho lịch học. */
public record Term(
        String maHocKy,
        String tenHocKy,
        String namHoc,
        LocalDate ngayBatDau,
        LocalDate ngayKetThuc) {
}
