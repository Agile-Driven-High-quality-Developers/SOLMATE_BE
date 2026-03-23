package org.solmate.domain.social.dto.response;

import org.solmate.domain.social.entity.MentoringRelation;

public record MentoringResponse(String status) {
    public static MentoringResponse of(MentoringRelation mentoringRelation) {
        return new MentoringResponse(mentoringRelation.getStatus().name());
    }
}
