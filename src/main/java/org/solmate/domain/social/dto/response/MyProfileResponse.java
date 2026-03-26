package org.solmate.domain.social.dto.response;

import org.solmate.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyProfileResponse {

    private Long userId;
    private String nickname;
    private String imageUrl;
    private long followerCount;
    private long followingCount;

    public static MyProfileResponse of(User user, long followerCount, long followingCount) {
        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getImageUrl(),
                followerCount,
                followingCount
        );
    }
}
