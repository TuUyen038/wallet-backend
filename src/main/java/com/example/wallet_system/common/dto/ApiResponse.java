package com.example.wallet_system.common.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
 
import java.time.LocalDateTime;
 
/**
 * Wrapper chung cho mọi API response.
 *
 * Q: Tại sao cần wrapper thay vì return data trực tiếp?
 * A: Chuẩn hóa response format — client luôn biết data ở đâu, error ở đâu.
 *    Dễ thêm metadata sau này (pagination, requestId, version) mà không breaking change.
 *
 * Response thành công:  { "success": true,  "data": {...},  "error": null }
 * Response lỗi:         { "success": false, "data": null,   "error": {...} }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)  // Không serialize field null
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }
 
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message), LocalDateTime.now());
    }
 
    public record ErrorDetail(String code, String message) {}
}
 

