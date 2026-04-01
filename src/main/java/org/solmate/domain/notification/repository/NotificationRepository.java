package org.solmate.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.solmate.domain.notification.entity.Notification;
import org.solmate.domain.notification.enums.NotificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findAllByUserIdAndCategoryAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, NotificationCategory category);

    long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);

    long countByUserIdAndCategoryAndIsReadFalseAndIsDeletedFalse(Long userId, NotificationCategory category);

    // 멘토 신청 취소 시 멘토에게 전송된 알림 조회 (payload의 mentoringRelationId로 식별)
    @Query(value = "SELECT * FROM notification WHERE user_id = :mentorId AND notification_type = 'MENTORING_REQUEST' AND is_deleted = false AND payload::jsonb->>'mentoringRelationId' = CAST(:relationId AS TEXT)", nativeQuery = true)
    Optional<Notification> findMentoringRequestNotification(@Param("mentorId") Long mentorId, @Param("relationId") Long relationId);

    // 탈퇴 시 해당 유저가 발신자인 알림 소프트 딜리트
    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true WHERE n.senderId = :senderId")
    void softDeleteBySenderId(@Param("senderId") Long senderId);
}
