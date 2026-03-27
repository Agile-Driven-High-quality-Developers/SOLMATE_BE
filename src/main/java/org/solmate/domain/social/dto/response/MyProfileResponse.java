package org.solmate.domain.social.dto.response;

import org.solmate.domain.auth.enums.OAuthProvider;
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
    private String provider;

    public static MyProfileResponse of(User user, long followerCount, long followingCount, OAuthProvider provider) {
        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getImageUrl(),
                followerCount,
                followingCount,
                provider.name()
        );
    }
}
