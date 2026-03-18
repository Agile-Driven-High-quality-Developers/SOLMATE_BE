package org.solmate.domain.auth.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.auth.dto.request.LoginRequest;
import org.solmate.domain.auth.dto.request.SignUpRequest;
import org.solmate.domain.auth.dto.response.LoginResponse;
import org.solmate.domain.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody @Valid SignUpRequest request) {
        authService.signUp(request);
        return ApiResponse.success(SuccessStatus.SIGNUP_SUCCESS);
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(request, response);
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, loginResponse);
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<String>> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        String newAccessToken = authService.reissue(refreshToken);
        return ApiResponse.success(SuccessStatus.REISSUE_SUCCESS, newAccessToken);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String bearer = request.getHeader("Authorization");
        String accessToken = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
        authService.logout(accessToken, refreshToken, response);
        return ApiResponse.success(SuccessStatus.LOGOUT_SUCCESS);
    }
}