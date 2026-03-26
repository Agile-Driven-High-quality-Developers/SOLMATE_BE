package org.solmate.domain.social.controller;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.ErrorStatus;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.social.dto.response.FollowListResponse;
import org.solmate.domain.social.dto.response.MyMenteeListResponse;
import org.solmate.domain.social.dto.response.MyMentorResponse;
import org.solmate.domain.social.dto.response.MyProfileResponse;
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

    @Operation(
            summary = "유저 목록 조회",
            description = """
                    전체 유저 목록을 조회합니다.

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
            @AuthenticationPrincipal Long currentUserId
    ) {
        UserListResponse response = userListService.getUserList(currentUserId);
        return ApiResponse.success(SuccessStatus.USER_LIST_SUCCESS, response);
    }

    @Operation(
            summary = "내 프로필 카드 조회",
            description = "현재 로그인한 유저의 프로필을 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal Long currentUserId
    ) {
        MyProfileResponse response = userListService.getMyProfile(currentUserId);
        return ApiResponse.success(SuccessStatus.MY_PROFILE_SUCCESS, response);
    }

    @Operation(
            summary = "유저 프로필 카드 조회",
            description = """
                    특정 유저의 프로필을 조회합니다.

                    **응답 필드**
                    - followerCount: 이 유저를 팔로우하는 사람 수
                    - followingCount: 이 유저가 팔로우하는 사람 수
                    - isFollowing: 현재 로그인 유저의 팔로우 여부
                    - mentoringStatus: NONE / PENDING / ACCEPTED

                    **주의**
                    - 탈퇴한 유저 조회 시 404 반환
                    - 본인 userId 입력 시 403 반환
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
            summary = "내 멘토 조회",
            description = """
                    현재 로그인한 유저의 멘토를 조회합니다.

                    **응답 필드**
                    - hasMentor=true  → userId, nickname, imageUrl 포함
                    - hasMentor=false → userId, nickname, imageUrl은 null
                    """
    )
    @GetMapping("/me/mentor")
    public ResponseEntity<ApiResponse<MyMentorResponse>> getMyMentor(
            @AuthenticationPrincipal Long currentUserId
    ) {
        MyMentorResponse response = userListService.getMyMentor(currentUserId);
        return ApiResponse.success(SuccessStatus.MY_MENTOR_SUCCESS, response);
    }

    @Operation(
            summary = "내 멘티 목록 조회",
            description = """
                    현재 로그인한 유저의 멘티 목록을 조회합니다.

                    **응답 필드**
                    - hasMentee=true  → mentees 리스트에 멘티 정보 포함
                    - hasMentee=false → mentees는 빈 리스트
                    """
    )
    @GetMapping("/me/mentees")
    public ResponseEntity<ApiResponse<MyMenteeListResponse>> getMyMentees(
            @AuthenticationPrincipal Long currentUserId
    ) {
        MyMenteeListResponse response = userListService.getMyMentees(currentUserId);
        return ApiResponse.success(SuccessStatus.MY_MENTEE_LIST_SUCCESS, response);
    }

    @Operation(
            summary = "내 팔로워 목록 조회",
            description = """
                    내 팔로워 목록을 커서 기반 무한 스크롤로 조회합니다.

                    **페이지네이션**
                    - 첫 페이지: cursor 파라미터 생략
                    - 다음 페이지: 이전 응답의 nextCursor 값을 cursor로 전달
                    - hasNext가 false이면 마지막 페이지
                    """
    )
    @GetMapping("/me/followers")
    public ResponseEntity<ApiResponse<FollowListResponse>> getMyFollowerList(
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "마지막으로 받은 유저의 userId (첫 페이지는 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 가져올 유저 수 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowListResponse response = userListService.getFollowerList(currentUserId, cursor, size);
        return ApiResponse.success(SuccessStatus.FOLLOWER_LIST_SUCCESS, response);
    }

    @Operation(
            summary = "내 팔로잉 목록 조회",
            description = """
                    내 팔로잉 목록을 커서 기반 무한 스크롤로 조회합니다.

                    **페이지네이션**
                    - 첫 페이지: cursor 파라미터 생략
                    - 다음 페이지: 이전 응답의 nextCursor 값을 cursor로 전달
                    - hasNext가 false이면 마지막 페이지
                    """
    )
    @GetMapping("/me/following")
    public ResponseEntity<ApiResponse<FollowListResponse>> getMyFollowingList(
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "마지막으로 받은 유저의 userId (첫 페이지는 생략)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 가져올 유저 수 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowListResponse response = userListService.getFollowingList(currentUserId, cursor, size);
        return ApiResponse.success(SuccessStatus.FOLLOWING_LIST_SUCCESS, response);
    }

    @Operation(
            summary = "타인 팔로워 목록 조회",
            description = """
                    특정 유저의 팔로워 목록을 커서 기반 무한 스크롤로 조회합니다.

                    **페이지네이션**
                    - 첫 페이지: cursor 파라미터 생략
                    - 다음 페이지: 이전 응답의 nextCursor 값을 cursor로 전달
                    - hasNext가 false이면 마지막 페이지

                    **주의**
                    - 본인 userId 입력 시 403 반환
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
        if (currentUserId.equals(userId)) {
            throw new GeneralException(ErrorStatus.USER_SELF_FOLLOW_LIST_FORBIDDEN);
        }
        FollowListResponse response = userListService.getFollowerList(userId, cursor, size);
        return ApiResponse.success(SuccessStatus.FOLLOWER_LIST_SUCCESS, response);
    }

    @Operation(
            summary = "타인 팔로잉 목록 조회",
            description = """
                    특정 유저의 팔로잉 목록을 커서 기반 무한 스크롤로 조회합니다.

                    **페이지네이션**
                    - 첫 페이지: cursor 파라미터 생략
                    - 다음 페이지: 이전 응답의 nextCursor 값을 cursor로 전달
                    - hasNext가 false이면 마지막 페이지

                    **주의**
                    - 본인 userId 입력 시 403 반환
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
        if (currentUserId.equals(userId)) {
            throw new GeneralException(ErrorStatus.USER_SELF_FOLLOW_LIST_FORBIDDEN);
        }
        FollowListResponse response = userListService.getFollowingList(userId, cursor, size);
        return ApiResponse.success(SuccessStatus.FOLLOWING_LIST_SUCCESS, response);
    }
}
