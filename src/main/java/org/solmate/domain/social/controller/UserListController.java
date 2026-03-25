package org.solmate.domain.social.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.social.dto.response.FollowListResponse;
import org.solmate.domain.social.dto.response.UserListResponse;
import org.solmate.domain.social.dto.response.UserProfileResponse;
import org.solmate.domain.social.service.UserListService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "UserList", description = "유저 목록 / 프로필 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserListController {

    private final UserListService userListService;

    /**
     * 유저 목록 조회 (커서 기반 무한 스크롤)
     *
     * - 첫 페이지 요청 시 cursor 파라미터 생략
     * - 이후 응답의 nextCursor 값을 cursor로 전달하여 다음 페이지 조회
     * - hasAcceptedMentor가 true이면 프론트에서 NONE 상태 멘토신청 버튼 비활성화
     * - 각 유저의 팔로우 여부(isFollowing), 멘토링 상태(mentoringStatus) 포함
     * - 본인은 isMe=true로 표시 (팔로우/멘토 버튼 미표시)
     *
     * mentoringStatus 값:
     *   NONE     → 멘토신청 버튼
     *   PENDING  → 신청완료 버튼 (노란 테두리)
     *   ACCEPTED → 멘토 버튼 (노란색 채움)
     */
    @Operation(
            summary = "유저 목록 조회",
            description = """
                    커서 기반 무한 스크롤로 유저 목록을 조회합니다.

                    **페이지네이션**
                    - 첫 페이지: cursor 파라미터 생략
                    - 다음 페이지: 이전 응답의 nextCursor 값을 cursor로 전달
                    - hasNext가 false이면 마지막 페이지

                    **멘토링 버튼 상태**
                    - hasAcceptedMentor=true → NONE 상태의 멘토신청 버튼 전체 비활성화
                    - mentoringStatus=NONE → 멘토신청 버튼
                    - mentoringStatus=PENDING → 신청완료 버튼
                    - mentoringStatus=ACCEPTED → 멘토 버튼

                    **팔로우 버튼 상태**
                    - isFollowing=false → 팔로우 버튼
                    - isFollowing=true → 팔로잉 버튼
                    - isMe=true → 버튼 없음 (본인)
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<UserListResponse>> getUserList(
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "마지막으로 받은 유저의 userId (첫 페이지는 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 가져올 유저 수 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        UserListResponse response = userListService.getUserList(currentUserId, cursor, size);
        return ApiResponse.success(SuccessStatus.USER_LIST_SUCCESS, response);
    }

    /**
     * 유저 프로필 단건 조회
     *
     * - 팔로워 수, 팔로잉 수, 팔로우 여부, 멘토링 상태 포함
     * - 탈퇴한 유저는 조회 불가 (404 반환)
     * - 본인 조회 시 isMe=true, isFollowing=false
     * - 총 수익률 / 총 수익은 추후 구현 예정
     */
    @Operation(
            summary = "유저 프로필 카드 조회",
            description = """
                    특정 유저의 프로필을 조회합니다.

                    **응답 필드**
                    - followerCount: 이 유저를 팔로우하는 사람 수
                    - followingCount: 이 유저가 팔로우하는 사람 수
                    - isMe: 본인 여부 (true이면 팔로우/멘토 버튼 미표시)
                    - isFollowing: 현재 로그인 유저의 팔로우 여부
                    - mentoringStatus: NONE / PENDING / ACCEPTED

                    **주의**
                    - 탈퇴한 유저 조회 시 404 반환
                    - 총 수익률 / 총 수익은 추후 추가 예정
                    """
    )
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "조회할 유저의 userId")
            @PathVariable Long userId
    ) {
        UserProfileResponse response = userListService.getUserProfile(currentUserId, userId);
        return ApiResponse.success(SuccessStatus.USER_PROFILE_SUCCESS, response);
    }

    @Operation(
            summary = "팔로워 목록 조회",
            description = """
                    특정 유저의 팔로워 목록을 커서 기반 무한 스크롤로 조회합니다.

                    **페이지네이션**
                    - 첫 페이지: cursor 파라미터 생략
                    - 다음 페이지: 이전 응답의 nextCursor 값을 cursor로 전달
                    - hasNext가 false이면 마지막 페이지
                    """
    )
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<FollowListResponse>> getFollowerList(
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "조회할 유저의 userId")
            @PathVariable Long userId,
            @Parameter(description = "마지막으로 받은 유저의 userId (첫 페이지는 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 가져올 유저 수 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowListResponse response = userListService.getFollowerList(userId, cursor, size);
        return ApiResponse.success(SuccessStatus.FOLLOWER_LIST_SUCCESS, response);
    }

    @Operation(
            summary = "팔로잉 목록 조회",
            description = """
                    특정 유저의 팔로잉 목록을 커서 기반 무한 스크롤로 조회합니다.

                    **페이지네이션**
                    - 첫 페이지: cursor 파라미터 생략
                    - 다음 페이지: 이전 응답의 nextCursor 값을 cursor로 전달
                    - hasNext가 false이면 마지막 페이지
                    """
    )
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<FollowListResponse>> getFollowingList(
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "조회할 유저의 userId")
            @PathVariable Long userId,
            @Parameter(description = "마지막으로 받은 유저의 userId (첫 페이지는 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 가져올 유저 수 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowListResponse response = userListService.getFollowingList(userId, cursor, size);
        return ApiResponse.success(SuccessStatus.FOLLOWING_LIST_SUCCESS, response);
    }
}
