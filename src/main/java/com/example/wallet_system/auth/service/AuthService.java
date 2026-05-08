package com.example.wallet_system.auth.service;
import com.example.wallet_system.auth.dto.AuthDto;
import com.example.wallet_system.auth.entity.RefreshToken;
import com.example.wallet_system.auth.repository.RefreshTokenRepository;
import com.example.wallet_system.common.exception.AppException;
import com.example.wallet_system.config.JwtService;
import com.example.wallet_system.user.entity.User;
import com.example.wallet_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
 
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
 
    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;
 
    @Transactional
    public AuthDto.RegisterResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new AppException.EmailAlreadyExistsException(request.email());
        }
 
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .build();
 
        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());
 
        return new AuthDto.RegisterResponse(saved.getId(), saved.getEmail(), saved.getFullName());
    }
 
    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(AppException.InvalidCredentialsException::new);
 
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", request.email());
            throw new AppException.InvalidCredentialsException();
        }
 
        // Revoke token cũ trước khi tạo token mới
        // Q: Tại sao revoke token cũ khi login?
        // A: Nếu account bị compromise, attacker không thể dùng refresh token cũ
        //    sau khi user login lại và đổi mật khẩu.
        refreshTokenRepository.revokeAllByUserId(user.getId());
 
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = createRefreshToken(user);
 
        log.info("User logged in: {}", user.getEmail());
 
        return new AuthDto.TokenResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs()
        );
    }
 
    @Transactional
    public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new AppException.InvalidTokenException("Refresh token not found"));
 
        if (!refreshToken.isValid()) {
            log.warn("Attempted to use invalid refresh token for user: {}",
                    refreshToken.getUser().getEmail());
            // Revoke toàn bộ token của user — có thể là dấu hiệu token bị đánh cắp
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId());
            throw new AppException.InvalidTokenException("Refresh token is expired or revoked");
        }
 
        // Rotate refresh token — mỗi lần refresh tạo token mới, invalidate token cũ
        // Q: Tại sao rotate thay vì dùng lại?
        // A: Nếu refresh token bị leak, attacker chỉ dùng được 1 lần. Lần sau
        //    user dùng token cũ sẽ bị reject → phát hiện ra compromise.
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
 
        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getEmail());
        String newRefreshToken = createRefreshToken(user);
 
        log.debug("Token refreshed for user: {}", user.getEmail());
 
        return new AuthDto.TokenResponse(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTokenExpirationMs()
        );
    }
 
    private String createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiredAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
 
        return refreshTokenRepository.save(token).getToken();
    }
}
 

// TODO: refresh cần token lưu trong HttpOnly cookie
// TODO: hash refresh token trước khi lưu vào DB