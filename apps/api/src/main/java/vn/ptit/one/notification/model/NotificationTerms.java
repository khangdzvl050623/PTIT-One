package vn.ptit.one.notification.model;

import java.util.List;

/** Từ vựng của thông báo — khớp từng ký tự với CHECK trong V4. */
public final class NotificationTerms {

    public static final String SOAN = "SOAN";
    public static final String TU_DONG = "TU_DONG";

    public static final String NHAP = "NHAP";
    public static final String DA_GUI = "DA_GUI";

    public static final String THONG_THUONG = "THONG_THUONG";
    public static final String QUAN_TRONG = "QUAN_TRONG";
    public static final List<String> MUC_DO = List.of(THONG_THUONG, QUAN_TRONG);

    public static final String TOAN_TRUONG = "TOAN_TRUONG";
    public static final String CO_SO = "CO_SO";
    public static final String LOP_HOC_PHAN = "LOP_HOC_PHAN";
    /** Chỉ cho thông báo tự sinh gửi riêng một người. */
    public static final String CA_NHAN = "CA_NHAN";

    public static final String SINH_VIEN = "SINH_VIEN";
    public static final String GIANG_VIEN = "GIANG_VIEN";
    public static final String TAT_CA = "TAT_CA";
    public static final List<String> DOI_TUONG = List.of(SINH_VIEN, GIANG_VIEN, TAT_CA);

    public static final String DANG_KY = "DANG_KY";
    public static final String HUY_DANG_KY = "HUY_DANG_KY";
    public static final String CONG_BO_DIEM = "CONG_BO_DIEM";
    public static final String SUA_DIEM = "SUA_DIEM";
    public static final String LOP_BI_HUY = "LOP_BI_HUY";
    /** Admin cơ sở gỡ một sinh viên khỏi một lớp. */
    public static final String GO_GHI_DANH = "GO_GHI_DANH";

    private NotificationTerms() {
    }
}
