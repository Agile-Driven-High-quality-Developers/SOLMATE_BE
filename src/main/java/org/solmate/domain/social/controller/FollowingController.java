package org.solmate.domain.social.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.social.service.FollowingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Follow", description = "팔로우 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/follows")
public class FollowingController {

    private final FollowingService followingService;

    /**
     * 팔로우 신청
     * - 유저 목록 또는 프로필에서 팔로우 버튼 클릭 시 호출
     * - userId: 팔로우할 대상 유저의 ID
     */
    @Operation(summary = "팔로우 신청", description = "특정 유저를 팔로우합니다. 자기 자신 또는 이미 팔로우한 유저에게는 신청할 수 없습니다.")
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> follow(
            @AuthenticationPrincipal Long followerId,
            @PathVariable Long userId
    ) {
        followingService.follow(followerId, userId);
        return ApiResponse.success(SuccessStatus.FOLLOW_SUCCESS, null);
    }

    /**
     * 팔로우 취소
     * - 유저 목록 또는 프로필에서 팔로잉 버튼 클릭 시 호출
     * - userId: 팔로우를 취소할 대상 유저의 ID
     */
    @Operation(summary = "팔로우 취소", description = "팔로우 중인 유저를 언팔로우합니다. 팔로우 관계가 없는 경우 404를 반환합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @AuthenticationPrincipal Long followerId,
            @PathVariable Long userId
    ) {
        followingService.unfollow(followerId, userId);
        return ApiResponse.success(SuccessStatus.UNFOLLOW_SUCCESS, null);
    }
}
