package org.solmate.domain.notification.dto.response;

import java.time.LocalDateTime;

import org.solmate.domain.notification.entity.Notification;
import org.solmate.domain.notification.enums.NotificationCategory;
import org.solmate.domain.notification.enums.NotificationType;

public record NotificationResponse(
    Long notificationId,
    NotificationType notificationType,
    NotificationCategory category,
    String content,
    boolean isRead,
    String actUrl,
    LocalDateTime createdAt,
    String eventType
) {
    public static NotificationResponse of(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getNotificationType(),
            notification.getCategory(),
            notification.getContent(),
            notification.isRead(),
            notification.getActUrl(),
            notification.getCreatedAt(),
            "CREATE"
        );
    }

    public static NotificationResponse deleted(Long notificationId) {
        return new NotificationResponse(notificationId, null, null, null, false, null, null, "DELETE");
    }
}
