package com.example.wallet_system.auth.controller;
import com.example.wallet_system.auth.dto.AuthDto;
import com.example.wallet_system.auth.service.AuthService;
import com.example.wallet_system.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
 
    private final AuthService authService;
 
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.RegisterResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
 
        AuthDto.RegisterResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }
 
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
 
        AuthDto.TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
 
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refresh(
            @Valid @RequestBody AuthDto.RefreshRequest request) {
 
        AuthDto.TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
 
