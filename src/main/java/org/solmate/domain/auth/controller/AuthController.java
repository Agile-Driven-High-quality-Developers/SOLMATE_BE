package org.solmate.domain.auth.controller;

import java.util.Map;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.auth.dto.request.ConfirmEmailVerificationRequest;
import org.solmate.domain.auth.dto.request.EmailVerificationRequest;
import org.solmate.domain.auth.dto.request.LoginRequest;
import org.solmate.domain.auth.dto.request.PasswordResetRequest;
import org.solmate.domain.auth.dto.request.SignUpRequest;
import org.solmate.domain.auth.dto.response.LoginResponse;
import org.solmate.domain.auth.service.AuthService;
import org.solmate.domain.auth.service.EmailVerificationService;
import org.solmate.domain.auth.service.GoogleService;
import org.springframework.http.ResponseEntity;
import org.solmate.domain.user.service.UserService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final GoogleService googleService;
    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    @Operation(summary = "닉네임 중복 확인")
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String nickname) {
        userService.checkNicknameNotDuplicated(nickname);
        return ApiResponse.success(SuccessStatus.NICKNAME_CHECK_SUCCESS);
    }

    @Operation(summary = "인증 메일 발송")
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(
            @RequestBody @Valid EmailVerificationRequest request
    ) {
        emailVerificationService.requestEmailVerificationCode(request.email());
        return ApiResponse.success(SuccessStatus.EMAIL_SEND_SUCCESS);
    }

    @Operation(summary = "이메일 인증 코드 확인")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody @Valid ConfirmEmailVerificationRequest request
    ) {
        emailVerificationService.confirmEmailVerificationCode(request.email(), request.emailVerificationCode());
        return ApiResponse.success(SuccessStatus.EMAIL_VERIFY_SUCCESS);
    }

    @Operation(summary = "비밀번호 재설정 인증 메일 발송", description = "비밀번호 재설정을 위한 인증 코드를 이메일로 발송합니다.")
    @PostMapping("/password/email/send")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetEmail(
            @RequestBody @Valid EmailVerificationRequest request
    ) {
        emailVerificationService.requestPasswordResetCode(request.email());
        return ApiResponse.success(SuccessStatus.EMAIL_SEND_SUCCESS);
    }

    @Operation(summary = "비밀번호 재설정", description = "이메일 인증 완료 후 새 비밀번호로 변경합니다.")
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid PasswordResetRequest request
    ) {
        authService.resetPassword(request.email(), request.newPassword());
        return ApiResponse.success(SuccessStatus.PASSWORD_RESET_SUCCESS);
    }

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


    @GetMapping("/google/authorize-uri")
    @Operation(summary = "구글 로그인 URL 조회")
    public ResponseEntity<ApiResponse<Map<String, String>>> getGoogleAuthorizeUri() {
        String authorizeUri = googleService.getGoogleAuthorizeUri();
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, Map.of("authorizeUri", authorizeUri));
    }

    @GetMapping("/google/callback")
    @Operation(summary = "구글 로그인 콜백")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = googleService.loginWithGoogle(code, response);
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, loginResponse);
    }
}