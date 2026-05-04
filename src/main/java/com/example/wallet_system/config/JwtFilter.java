package com.example.wallet_system.config;
import com.example.wallet_system.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
 
import java.io.IOException;
 
/**
 * Chạy 1 lần mỗi request — extract JWT từ header, validate, set SecurityContext.
 *
 * Q: Tại sao extends OncePerRequestFilter?
 * A: Đảm bảo filter chỉ chạy đúng 1 lần mỗi request, tránh trường hợp
 *    Spring dispatch request nội bộ làm filter chạy lại.
 *
 * Flow:
 *   Request → JwtFilter → extract token → validate → set Authentication → Controller
 *                                        ↓ invalid
 *                                   pass through (SecurityConfig sẽ reject)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
 
    private final JwtService jwtService;
    private final UserRepository userRepository;
 
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
 
        final String authHeader = request.getHeader("Authorization");
 
        // Không có token → pass through, SecurityConfig sẽ quyết định reject hay không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
 
        final String token = authHeader.substring(7);
 
        if (!jwtService.isTokenValid(token)) {
            log.warn("Invalid or expired JWT token");
            filterChain.doFilter(request, response);
            return;
        }
 
        final String email = jwtService.extractEmail(token);
 
        // Chỉ set authentication nếu chưa authenticated (tránh override)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userRepository.findByEmail(email)
                    .orElse(null);
 
            if (userDetails != null) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user: {}", email);
            }
        }
 
        filterChain.doFilter(request, response);
    }
}
 

