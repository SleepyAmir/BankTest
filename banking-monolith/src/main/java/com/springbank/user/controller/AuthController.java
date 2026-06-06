package com.springbank.user.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.user.dto.LoginRequestDto;
import com.springbank.user.dto.RefreshTokenRequestDto;
import com.springbank.user.dto.TokenResponseDto;
import com.springbank.user.dto.UserRegistrationDto;
import com.springbank.user.dto.UserResponseDto;
import com.springbank.user.security.CustomUserDetailsService;
import com.springbank.user.security.JwtTokenProvider;
import com.springbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(@RequestBody LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        return ResponseEntity.ok(ApiResponse.success("Login successful",
                new TokenResponseDto(accessToken, refreshToken, "Bearer", 3600), "/api/auth/login"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@RequestBody UserRegistrationDto dto) {
        UserResponseDto user = userService.registerUser(dto);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user, "/api/auth/register"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refresh(@RequestBody RefreshTokenRequestDto request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = jwtTokenProvider.getUsernameFromToken(request.refreshToken());
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateAccessToken(auth);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(auth);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed",
                new TokenResponseDto(newAccessToken, newRefreshToken, "Bearer", 3600),
                "/api/auth/refresh"));
    }
}
