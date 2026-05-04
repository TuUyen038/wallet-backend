package com.example.wallet_system.config;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
 
/**
 * Xử lý toàn bộ logic tạo và validate JWT access token.
 *
 * Q: Tại sao để JwtService trong config/ thay vì auth/?
 * A: JwtService là infrastructure concern (tạo/validate token), không phải business logic.
 *    Sau này cả auth domain lẫn các domain khác đều có thể cần validate token.
 *
 * Q: Tại sao dùng HMAC-SHA256 (HS256) thay vì RSA?
 * A: HS256 đủ tốt cho monolith — 1 secret key, đơn giản hơn.
 *    RSA (RS256) cần thiết khi nhiều service cần verify token mà không cần secret
 *    (microservice pattern). Với project này HS256 là đúng lựa chọn.
 */
@Service
@Slf4j
public class JwtService {
 
    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
 
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }
 
    public String generateAccessToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
 
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }
 
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }
 
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
 
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
 
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
 
