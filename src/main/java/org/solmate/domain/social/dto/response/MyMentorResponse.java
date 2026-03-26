package org.solmate.domain.social.dto.response;

import org.solmate.domain.user.entity.User;

/**
 * 내 멘토 조회 응답 DTO
 *
 * 필드 설명:
 * - hasMentor : 수락된 멘토 존재 여부
 * - userId    : 멘토 유저 식별자 (hasMentor=false이면 null)
 */
public record MyMentorResponse(boolean hasMentor, Long userId) {

    public static MyMentorResponse of(User mentor) {
        return new MyMentorResponse(true, mentor.getId());
    }

    public static MyMentorResponse empty() {
        return new MyMentorResponse(false, null);
    }
}
