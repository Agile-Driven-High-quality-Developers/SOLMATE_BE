package org.solmate.domain.social.dto.response;

import java.util.List;

import org.solmate.domain.user.entity.User;

/**
 * 내 멘티 목록 조회 응답 DTO
 *
 * 필드 설명:
 * - hasMentee : 수락된 멘티 존재 여부
 * - mentees   : 멘티 목록 (없으면 빈 리스트)
 */
public record MyMenteeListResponse(boolean hasMentee, List<MenteeItem> mentees) {

    public record MenteeItem(Long userId) {
        public static MenteeItem of(User mentee) {
            return new MenteeItem(mentee.getId());
        }
    }

    public static MyMenteeListResponse of(List<User> mentees) {
        List<MenteeItem> items = mentees.stream().map(MenteeItem::of).toList();
        return new MyMenteeListResponse(!items.isEmpty(), items);
    }
}
