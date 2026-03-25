package org.solmate.domain.social.service;

import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.notification.entity.Notification;
import org.solmate.domain.notification.enums.NotificationCategory;
import org.solmate.domain.notification.enums.NotificationType;
import org.solmate.domain.notification.repository.NotificationRepository;
import org.solmate.domain.social.dto.response.MentoringResponse;
import org.solmate.domain.social.entity.MentoringRelation;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.social.repository.MentoringRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentoringService {

    private final MentoringRepository mentoringRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /**
     * 멘토 신청
     * - 유저 목록에서 특정 유저에게 멘토 신청 버튼을 눌렀을 때 호출됨
     * - 여러 명에게 동시에 신청 가능하지만, 동일한 멘토에게 중복 신청은 불가
     * - 신청 성공 시 멘토에게 알림이 전송되며, 알림 payload에 mentoringRelationId가 포함됨
     *   (멘토가 알림에서 수락/거절 버튼 클릭 시 해당 ID를 사용)
     */
    @Transactional
    public MentoringResponse requestMentoring(Long menteeId, Long mentorId) {
        // 자기 자신에게는 신청할 수 없음 (프론트에서도 막지만 백엔드에서도 방어)
        if (menteeId.equals(mentorId)) {
            throw new GeneralException(ErrorStatus.MENTORING_SELF_REQUEST);
        }

        User mentee = userRepository.findById(menteeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 이미 수락된 멘토가 있으면 신청 불가
        // (프론트에서 버튼 비활성화로 막지만, 직접 API 호출하는 경우 대비)
        if (mentoringRepository.existsByMenteeAndStatus(mentee, MentoringStatus.ACCEPTED)) {
            throw new GeneralException(ErrorStatus.MENTORING_ALREADY_HAS_MENTOR);
        }

        // 같은 멘토에게 이미 대기 중인 요청이 있으면 중복 신청 불가
        if (mentoringRepository.existsByMentorAndMenteeAndStatus(mentor, mentee, MentoringStatus.PENDING)) {
            throw new GeneralException(ErrorStatus.MENTORING_ALREADY_REQUESTED);
        }

        // 멘토링 관계 생성 (초기 상태: PENDING, 요청 시각 자동 기록)
        MentoringRelation mentoringRelation = MentoringRelation.builder()
                .mentor(mentor)
                .mentee(mentee)
                .build();
        mentoringRepository.save(mentoringRelation);

        // 멘토에게 알림 전송
        // payload에 mentoringRelationId와 menteeId를 담음
        // menteeId는 수락 시 행이 삭제된 경우에도 멘티의 멘토 존재 여부를 확인하기 위해 사용
        String payload = String.format("{\"mentoringRelationId\":%d,\"menteeId\":%d}",
                mentoringRelation.getId(), mentee.getId());
        Notification notification = Notification.builder()
                .user(mentor)
                .notificationType(NotificationType.MENTORING_REQUEST)
                .category(NotificationCategory.MENTORING)
                .content(mentee.getNickname() + "님이 멘토 신청을 보냈습니다.")
                .payload(payload)
                .build();
        notificationRepository.save(notification);

        return MentoringResponse.of(mentoringRelation);
    }

    /**
     * 멘토 신청 수락
     * - NotificationService에서 알림의 payload를 파싱한 후 호출됨
     * - 멘티가 여러 멘토에게 신청을 보낸 경우, 다른 멘토가 먼저 수락했을 수 있으므로
     *   수락 시점에 멘티의 멘토 존재 여부를 다시 한번 확인함
     * - 수락 성공 시 멘티에게 수락 알림 전송
     */
    @Transactional
    public MentoringResponse acceptMentoring(Long mentorId, Long relationId) {
        MentoringRelation relation = mentoringRepository.findById(relationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MENTORING_RELATION_NOT_FOUND));

        // 해당 멘토링 요청의 수신자(멘토) 본인인지 확인
        if (!relation.getMentor().getId().equals(mentorId)) {
            throw new GeneralException(ErrorStatus.MENTORING_UNAUTHORIZED);
        }

        // 이미 수락 또는 거절된 요청인지 확인 (PENDING 상태만 처리 가능)
        if (relation.getStatus() != MentoringStatus.PENDING) {
            throw new GeneralException(ErrorStatus.MENTORING_ALREADY_RESPONDED);
        }

        // 멘티가 이미 다른 멘토에게 수락된 상태인지 확인
        // (A, B 두 멘토에게 신청했을 때 A가 먼저 수락하면 B가 수락하려 할 때 막음)
        if (mentoringRepository.existsByMenteeAndStatus(relation.getMentee(), MentoringStatus.ACCEPTED)) {
            throw new GeneralException(ErrorStatus.MENTORING_ALREADY_HAS_MENTOR);
        }

        // 멘토링 관계를 수락 상태로 변경 (ACCEPTED, 응답 시각 자동 기록)
        relation.accept();

        // 멘티가 다른 멘토에게 보낸 나머지 PENDING 요청 삭제
        // (멘토는 1명만 가질 수 있으므로 수락된 관계 외 나머지는 불필요)
        mentoringRepository.deleteAllByMenteeAndStatusAndIdNot(
                relation.getMentee(), MentoringStatus.PENDING, relation.getId());

        // 멘티에게 수락 알림 전송 (거절과 달리 수락 시에만 알림 발송)
        Notification notification = Notification.builder()
                .user(relation.getMentee())
                .notificationType(NotificationType.MENTORING_ACCEPTED)
                .category(NotificationCategory.MENTORING)
                .content(relation.getMentor().getNickname() + "님이 멘토 신청을 수락하셨습니다.")
                .build();
        notificationRepository.save(notification);

        return MentoringResponse.of(relation);
    }

    /**
     * 멘토링 취소 (멘티만 가능)
     * - PENDING(신청 대기 중) 또는 ACCEPTED(수락된 관계) 상태 모두 취소 가능
     * - relation의 mentee_id가 현재 사용자와 다르면 403 → 멘토가 호출해도 여기서 차단됨
     */
    @Transactional
    public void cancelMentoring(Long menteeId, Long relationId) {
        MentoringRelation relation = mentoringRepository.findById(relationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MENTORING_RELATION_NOT_FOUND));

        // 본인(멘티)의 관계인지 확인 → 멘토가 호출하면 mentee_id != menteeId 이므로 차단
        if (!relation.getMentee().getId().equals(menteeId)) {
            throw new GeneralException(ErrorStatus.MENTORING_UNAUTHORIZED);
        }

        // ACCEPTED 상태(이미 맺어진 멘토-멘티 관계)만 취소 가능
        if (relation.getStatus() != MentoringStatus.ACCEPTED) {
            throw new GeneralException(ErrorStatus.MENTORING_NOT_ACCEPTED);
        }

        mentoringRepository.delete(relation);
    }

    /**
     * 멘토 신청 거절
     * - NotificationService에서 알림의 payload를 파싱한 후 호출됨
     * - 거절 시 멘티에게 별도 알림은 전송하지 않음
     */
    @Transactional
    public MentoringResponse rejectMentoring(Long mentorId, Long relationId) {
        MentoringRelation relation = mentoringRepository.findById(relationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MENTORING_RELATION_NOT_FOUND));

        // 해당 멘토링 요청의 수신자(멘토) 본인인지 확인
        if (!relation.getMentor().getId().equals(mentorId)) {
            throw new GeneralException(ErrorStatus.MENTORING_UNAUTHORIZED);
        }

        // 이미 수락 또는 거절된 요청인지 확인 (PENDING 상태만 처리 가능)
        if (relation.getStatus() != MentoringStatus.PENDING) {
            throw new GeneralException(ErrorStatus.MENTORING_ALREADY_RESPONDED);
        }

        // 멘토링 관계를 거절 상태로 변경 (REJECTED, 응답 시각 자동 기록)
        relation.reject();

        return MentoringResponse.of(relation);
    }
}