package com.example.wallet_system.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
 
// ===== REQUEST DTOs =====
// Tách DTO khỏi Entity: Entity là DB model, DTO là API contract — không để lộ lẫn nhau.
 
public class AuthDto {
 
    public record RegisterRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
 
            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,
 
            @NotBlank(message = "Full name is required")
            String fullName
    ) {}
 
    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
 
            @NotBlank(message = "Password is required")
            String password
    ) {}
 
    public record RefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}
 
    // ===== RESPONSE DTOs =====
 
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn   // milliseconds, giúp client biết khi nào cần refresh
    ) {}
 
    public record RegisterResponse(
            Long userId,
            String email,
            String fullName
    ) {}
}
 
