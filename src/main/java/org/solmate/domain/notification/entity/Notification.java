package org.solmate.domain.notification.entity;

import org.solmate.common.base.BaseEntity;
import org.solmate.domain.notification.enums.NotificationCategory;
import org.solmate.domain.notification.enums.NotificationType;
import org.solmate.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String payload;

    @Column(name = "act_url")
    private String actUrl;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationCategory category;

    @Builder
    public Notification(User user, NotificationType notificationType, String content,
                        String payload, String actUrl, NotificationCategory category) {
        this.user = user;
        this.notificationType = notificationType;
        this.content = content;
        this.payload = payload;
        this.actUrl = actUrl;
        this.category = category;
        this.isRead = false;
        this.isDeleted = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void delete() {
        this.isDeleted = true;
    }
}
