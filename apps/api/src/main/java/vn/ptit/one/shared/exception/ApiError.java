package vn.ptit.one.shared.exception;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Hợp đồng lỗi dùng chung cho mọi API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Map<String, String> fieldErrors, String traceId) {

    public static ApiError of(String code, String message, String traceId) {
        return new ApiError(code, message, null, traceId);
    }
}
