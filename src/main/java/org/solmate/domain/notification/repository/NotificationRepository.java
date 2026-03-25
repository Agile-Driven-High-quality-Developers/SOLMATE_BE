package org.solmate.domain.notification.repository;

import java.util.List;

import org.solmate.domain.notification.entity.Notification;
import org.solmate.domain.notification.enums.NotificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findAllByUserIdAndCategoryAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, NotificationCategory category);

    long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);

    long countByUserIdAndCategoryAndIsReadFalseAndIsDeletedFalse(Long userId, NotificationCategory category);
}
