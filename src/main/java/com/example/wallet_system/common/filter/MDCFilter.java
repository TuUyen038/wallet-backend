package com.example.wallet_system.common.filter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
 
import java.io.IOException;
import java.util.UUID;
 
/**
 * Gắn requestId vào MDC (Mapped Diagnostic Context) cho mọi request.
 *
 * Q: MDC là gì?
 * A: MDC là thread-local map của SLF4J. Mọi log.info/warn/error trong cùng 1 request
 *    sẽ tự động đính kèm requestId mà không cần truyền tay qua từng method.
 *
 * Q: Tại sao cần requestId?
 * A: Khi có lỗi, ta search log theo requestId để thấy toàn bộ flow của 1 request đó.
 *    Không có requestId, log của nhiều request xen lẫn nhau rất khó đọc.
 *
 * Log output ví dụ:
 *   2024-01-01 [requestId=abc-123] INFO  AuthService - User logged in: alice@wallet.com
 *   2024-01-01 [requestId=abc-123] DEBUG JwtService  - Generated token, expires in 900000ms
 */
@Component
@Order(1)  // Chạy trước mọi filter khác
@Slf4j
public class MDCFilter implements Filter {
 
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
 
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
 
        HttpServletRequest httpRequest = (HttpServletRequest) request;
 
        // Ưu tiên dùng requestId do client gửi lên (nếu có), không thì tự sinh
        // Giúp trace request end-to-end từ client → server log
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
 
        MDC.put(REQUEST_ID_KEY, requestId);
        log.debug("Incoming request: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
 
        try {
            chain.doFilter(request, response);
        } finally {
            // QUAN TRỌNG: Phải clear MDC sau mỗi request.
            // Thread pool tái dụng thread — không clear thì requestId của request cũ
            // sẽ lẫn vào request mới.
            MDC.clear();
        }
    }
}
 
