package org.solmate.external.ls.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LsTokenResponse(
        @JsonProperty("access_token")
		String accessToken,
        @JsonProperty("token_type")
		String tokenType,
        @JsonProperty("expire_in")
		Long expiresIn
) {
}
