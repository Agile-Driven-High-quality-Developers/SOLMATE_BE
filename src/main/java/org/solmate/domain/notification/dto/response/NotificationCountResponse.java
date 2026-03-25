package org.solmate.domain.notification.dto.response;

public record NotificationCountResponse(
    long total,
    long social,
    long trading,
    long mentoring
) {}
