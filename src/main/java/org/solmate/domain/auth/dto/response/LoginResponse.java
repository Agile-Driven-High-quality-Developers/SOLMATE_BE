package org.solmate.domain.auth.dto.response;

public record LoginResponse(
        Long userId,
        String nickname,
        String accessToken,
        String refreshToken
) {
}