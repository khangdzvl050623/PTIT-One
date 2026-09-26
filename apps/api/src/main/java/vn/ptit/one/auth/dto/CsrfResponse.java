package vn.ptit.one.auth.dto;

/** Tên header và giá trị CSRF; giá trị trùng cookie {@code XSRF-TOKEN}. */
public record CsrfResponse(String headerName, String token) {
}
