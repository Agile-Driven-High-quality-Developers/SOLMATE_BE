package org.solmate.domain.social.enums;

/**
 * 유저 목록 / 프로필에서 현재 로그인 유저 기준 멘토링 관계 상태
 *
 * - NONE     : 멘토 신청 가능 상태 (신청한 적 없음)
 * - PENDING  : 해당 유저에게 멘토 신청을 보낸 상태 (승인 대기 중)
 * - ACCEPTED : 해당 유저가 나의 멘토로 수락된 상태
 *
 * 프론트 버튼 표시 기준:
 *   NONE     → "멘토신청" 버튼 (hasAcceptedMentor=true 이면 비활성화)
 *   PENDING  → "신청완료" 버튼 (노란 테두리)
 *   ACCEPTED → "멘토" 버튼 (노란색 채움)
 *   isMe=true → 버튼 없음 (— 표시)
 */
public enum UserMentoringStatus {
    NONE,
    PENDING,
    ACCEPTED
}
