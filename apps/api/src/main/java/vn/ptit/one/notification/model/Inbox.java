package vn.ptit.one.notification.model;

import java.util.List;

/** Một trang hộp thư, kèm số chưa đọc của TOÀN hộp thư (cho chuông). */
public record Inbox(int soChuaDoc, int trang, int kichThuoc, List<InboxItem> thongBao) {
}
