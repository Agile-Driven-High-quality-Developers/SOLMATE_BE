package org.solmate.domain.auth.dto.response;

public record LoginResponse(
        String nickname,
        String accessToken,
        String refreshToken
) {
}