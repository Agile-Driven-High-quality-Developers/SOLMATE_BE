package org.solmate.domain.social.dto.response;

import java.util.List;

public record MyMentoringStatusResponse(
        boolean hasAcceptedMentor,
        List<Long> pendingMentorIds
) {
}
