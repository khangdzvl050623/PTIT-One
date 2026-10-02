package vn.ptit.one.timetable.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Thay TOÀN BỘ lịch của lớp. Danh sách rỗng nghĩa là xoá hết lịch. */
public record SetScheduleRequest(
        @Size(max = 14, message = "Tối đa 14 buổi mỗi tuần.")
        List<@Valid SlotRequest> buoiHoc) {

    public record SlotRequest(
            @Min(value = 2, message = "Thứ phải từ 2 đến 8.")
            @Max(value = 8, message = "Thứ phải từ 2 đến 8.") int thu,
            @Min(value = 1, message = "Tiết bắt đầu tối thiểu là 1.")
            @Max(value = 12, message = "Tiết bắt đầu tối đa là 12.") int tietBatDau,
            @Min(value = 1, message = "Số tiết tối thiểu là 1.")
            @Max(value = 12, message = "Số tiết tối đa là 12.") int soTiet,
            String phongHoc,
            @Min(value = 1, message = "Tuần bắt đầu tối thiểu là 1.") int tuanBatDau,
            @Min(value = 1, message = "Tuần kết thúc tối thiểu là 1.") int tuanKetThuc) {
    }
}
