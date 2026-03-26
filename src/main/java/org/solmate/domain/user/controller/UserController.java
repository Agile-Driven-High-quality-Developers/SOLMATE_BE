package org.solmate.domain.user.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.user.dto.request.PasswordCheckRequest;
import org.solmate.domain.user.dto.request.WithdrawRequest;
import org.solmate.domain.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "유저 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "비밀번호 확인", description = "현재 비밀번호가 맞는지 확인합니다.")
    @PostMapping("/password/check")
    public ResponseEntity<ApiResponse<Void>> checkPassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PasswordCheckRequest request) {
        userService.checkPassword(userId, request.password());
        return ApiResponse.success(SuccessStatus.PASSWORD_CHECK_SUCCESS, null);
    }

    @Operation(summary = "회원 탈퇴", description = "현재 비밀번호 확인 후 회원 탈퇴 처리합니다. (soft delete)")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid WithdrawRequest request) {
        userService.withdrawWithPassword(userId, request.password());
        return ApiResponse.success(SuccessStatus.WITHDRAW_SUCCESS, null);
    }

    @Operation(summary = "프로필 이미지 삭제", description = "프로필 이미지를 삭제하고 기본 이미지로 되돌립니다.")
    @DeleteMapping("/profile-image")
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage(
            @AuthenticationPrincipal Long userId) {
        userService.deleteProfileImage(userId);
        return ApiResponse.success(SuccessStatus.PROFILE_IMAGE_DELETE_SUCCESS, null);
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = """
                    변경하려는 닉네임의 사용 가능 여부를 확인합니다.

                    **검사 순서**
                    1. 현재 로그인한 사용자의 닉네임과 동일한 경우 → `400` 반환
                    2. 다른 사용자가 이미 사용 중인 닉네임인 경우 → `409` 반환
                    3. 사용 가능한 닉네임인 경우 → `200` 반환

                    **클라이언트 권장:** 닉네임 변경 폼에서 '중복 확인' 버튼 클릭 시 호출하고, `200` 응답을 받은 경우에만 변경 요청을 허용하세요.""")
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Void>> checkNickname(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "중복 확인할 닉네임", example = "솔메이트123", required = true)
            @RequestParam String nickname) {
        userService.checkNicknameForUser(userId, nickname);
        return ApiResponse.success(SuccessStatus.NICKNAME_CHECK_SUCCESS, null);
    }

    @Operation(summary = "프로필 업데이트", description = "프로필 이미지와 닉네임을 변경합니다. 각 항목은 선택적으로 전송 가능합니다.")
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestPart(required = false) MultipartFile image,
            @RequestPart(required = false) String nickname) {
        userService.updateProfile(userId, image, nickname);
        return ApiResponse.success(SuccessStatus.PROFILE_UPDATE_SUCCESS, null);
    }
}
