package vn.ptit.one.course.model;

/**
 * Một quan hệ tiên quyết giữa hai môn, kèm tên để giao diện không phải tra thêm.
 *
 * <p>{@code loai} hiện LUÔN là {@code TIEN_QUYET}: bảng {@code MonHocTienQuyet}
 * không có cột loại quan hệ. Trường này có sẵn để thêm {@code HOC_TRUOC} và
 * {@code SONG_HANH} về sau mà không phải đổi hình dạng API — khi đó mới cần
 * migration thêm cột.
 */
public record CourseRelation(
        String loai,
        String maMonHoc,
        String tenMonHoc,
        String maMonYeuCau,
        String tenMonYeuCau) {

    public static final String TIEN_QUYET = "TIEN_QUYET";
}
