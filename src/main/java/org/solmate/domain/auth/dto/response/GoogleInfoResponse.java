package org.solmate.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleInfoResponse(
        String id,
        String email,
        String name,
        @JsonProperty("picture") String imageUrl
) {
}