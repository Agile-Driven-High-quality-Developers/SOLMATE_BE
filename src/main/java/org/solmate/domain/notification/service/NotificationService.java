package org.solmate.domain.notification.service;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.notification.entity.Notification;
import org.solmate.domain.notification.enums.NotificationType;
import org.solmate.domain.notification.repository.NotificationRepository;
import org.solmate.domain.social.dto.response.MentoringResponse;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.social.repository.MentoringRepository;
import org.solmate.domain.social.service.MentoringService;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MentoringService mentoringService;
    private final MentoringRepository mentoringRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 알림에서 멘토 신청 수락
     * - 멘토가 알림 목록에서 수락 버튼을 눌렀을 때 호출됨
     * - 알림을 검증한 뒤 payload에서 mentoringRelationId를 꺼내 MentoringService에 위임
     * - relation이 없는 경우 = 다른 멘토가 먼저 수락해서 해당 행이 삭제된 상황
     *   → payload의 menteeId로 멘티의 멘토 존재 여부를 확인 후 적절한 에러 반환
     * - 수락 처리 후 해당 알림을 읽음 상태로 변경
     */
    @Transactional
    public MentoringResponse acceptMentoringFromNotification(Long mentorId, Long notificationId) {
        // 알림 존재 여부, 본인 여부, 타입 검증
        Notification notification = findAndValidateNotification(mentorId, notificationId, NotificationType.MENTORING_REQUEST);

        // 알림 payload 파싱
        JsonNode payload = parsePayload(notification);
        Long mentoringRelationId = payload.get("mentoringRelationId").asLong();

        // relation이 없으면 다른 멘토가 먼저 수락해서 행이 삭제된 상황
        // payload의 menteeId로 멘티에게 이미 멘토가 있는지 확인 후 에러 반환
        if (!mentoringRepository.existsById(mentoringRelationId)) {
            Long menteeId = payload.get("menteeId").asLong();
            User mentee = userRepository.findById(menteeId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
            if (mentoringRepository.existsByMenteeAndStatus(mentee, MentoringStatus.ACCEPTED)) {
                throw new GeneralException(ErrorStatus.MENTORING_ALREADY_HAS_MENTOR);
            }
            throw new GeneralException(ErrorStatus.MENTORING_RELATION_NOT_FOUND);
        }

        // 수락 처리하면 알림을 읽음으로 표시
        notification.markAsRead();

        return mentoringService.acceptMentoring(mentorId, mentoringRelationId);
    }

    /**
     * 알림에서 멘토 신청 거절
     * - 멘토가 알림 목록에서 거절 버튼을 눌렀을 때 호출됨
     * - 알림을 검증한 뒤 payload에서 mentoringRelationId를 꺼내 MentoringService에 위임
     * - 거절 시 멘티에게 별도 알림은 전송하지 않음
     * - 거절 처리 후 해당 알림을 읽음 상태로 변경
     */
    @Transactional
    public MentoringResponse rejectMentoringFromNotification(Long mentorId, Long notificationId) {
        // 알림 존재 여부, 본인 여부, 타입 검증
        Notification notification = findAndValidateNotification(mentorId, notificationId, NotificationType.MENTORING_REQUEST);

        // 알림 payload에서 어떤 멘토링 관계인지 식별
        Long mentoringRelationId = parsePayload(notification).get("mentoringRelationId").asLong();

        // 거절 처리하면 알림을 읽음으로 표시
        notification.markAsRead();

        return mentoringService.rejectMentoring(mentorId, mentoringRelationId);
    }

    /**
     * 알림 조회 및 공통 검증
     * - notificationId로 알림을 조회하고 아래 세 가지를 검증함
     *   1. 알림이 존재하는지
     *   2. 현재 로그인한 사용자가 이 알림의 수신자인지 (다른 사람의 알림에 접근 방지)
     *   3. 알림 타입이 기대한 타입인지 (예: 멘토링 신청 알림에서만 수락/거절 가능)
     */
    private Notification findAndValidateNotification(Long userId, Long notificationId, NotificationType expectedType) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));

        // 이 알림을 받은 사람이 현재 요청한 사람과 동일한지 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_UNAUTHORIZED);
        }

        // 멘토링 신청 알림(MENTORING_REQUEST)에서만 수락/거절 가능
        if (notification.getNotificationType() != expectedType) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_INVALID_TYPE);
        }

        return notification;
    }

    /**
     * 알림 payload 파싱
     * - 멘토링 신청 시 알림 payload에 저장된 형식: {"mentoringRelationId": 123, "menteeId": 3}
     * - payload가 없거나 형식이 올바르지 않으면 에러 반환
     */
    private JsonNode parsePayload(Notification notification) {
        try {
            return objectMapper.readTree(notification.getPayload());
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_INVALID_TYPE);
        }
    }
}
