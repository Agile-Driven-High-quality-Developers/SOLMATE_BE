package org.solmate.domain.social.dto.response;

import org.solmate.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 팔로워 / 팔로잉 목록 아이템 응답 DTO
 *
 * 필드 설명:
 * - userId   : 유저 식별자
 * - nickname : 닉네임
 * - imageUrl : 프로필 이미지 S3 URL (없으면 null)
 */
@Getter
@AllArgsConstructor
public class FollowListItemResponse {

    private Long userId;
    private String nickname;
    private String imageUrl;

    public static FollowListItemResponse of(User user) {
        return new FollowListItemResponse(
                user.getId(),
                user.getNickname(),
                user.getImageUrl()
        );
    }
}
