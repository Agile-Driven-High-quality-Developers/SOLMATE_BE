package org.solmate.domain.social.dto.response;

import org.solmate.domain.social.enums.UserMentoringStatus;
import org.solmate.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 유저 목록 아이템 응답 DTO
 *
 * 필드 설명:
 * - userId          : 유저 식별자
 * - nickname        : 닉네임
 * - imageUrl        : 프로필 이미지 S3 URL (없으면 null)
 * - followerCount   : 이 유저를 팔로우하는 사람 수
 * - followingCount  : 이 유저가 팔로우하는 사람 수
 * - isMe            : 현재 로그인 유저 본인 여부 (true면 팔로우/멘토 버튼 미표시)
 * - isFollowing     : 현재 로그인 유저가 이 유저를 팔로우 중인지 여부
 * - mentoringStatus : 현재 로그인 유저 기준 멘토링 상태 (NONE/PENDING/ACCEPTED)
 */
@Getter
@AllArgsConstructor
public class UserListItemResponse {

    private Long userId;
    private String nickname;
    private String imageUrl;
    private long followerCount;
    private long followingCount;
    private boolean isMe;
    private boolean isFollowing;
    private UserMentoringStatus mentoringStatus;

    public static UserListItemResponse of(
            User user,
            long followerCount,
            long followingCount,
            boolean isMe,
            boolean isFollowing,
            UserMentoringStatus mentoringStatus
    ) {
        return new UserListItemResponse(
                user.getId(),
                user.getNickname(),
                user.getImageUrl(),
                followerCount,
                followingCount,
                isMe,
                isFollowing,
                mentoringStatus
        );
    }
}
