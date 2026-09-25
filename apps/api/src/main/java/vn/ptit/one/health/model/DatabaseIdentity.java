package vn.ptit.one.health.model;

/**
 * Database và tài khoản mà API thực sự đang nối tới.
 *
 * <p>Đây là thứ duy nhất trả lời được câu "API có đang ghi nhầm database không".
 * Cấu hình có thể nói một đằng, kết nối thật đi một nẻo.
 */
public record DatabaseIdentity(String databaseName, String loginName) {
}
