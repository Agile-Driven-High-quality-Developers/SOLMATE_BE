package org.solmate.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
        String accessToken,
        String idToken,
        String tokenType
) {
}