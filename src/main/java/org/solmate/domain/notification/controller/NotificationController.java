package org.solmate.domain.notification.controller;

import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.notification.dto.response.NotificationCountResponse;
import org.solmate.domain.notification.dto.response.NotificationResponse;
import org.solmate.domain.notification.enums.NotificationCategory;
import org.solmate.domain.notification.service.NotificationService;
import org.solmate.domain.social.dto.response.MentoringResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림에서 멘토 신청 수락
     * - 멘토가 MENTORING_REQUEST 알림에서 수락 버튼 클릭 시 호출
     * - 알림의 payload에서 mentoringRelationId를 꺼내 멘토링 관계를 ACCEPTED로 변경
     * - 멘티에게 수락 알림 전송
     */
    @Operation(summary = "알림에서 멘토 신청 수락")
    @PostMapping("/{notificationId}/mentor-request/accept")
    public ResponseEntity<ApiResponse<MentoringResponse>> acceptMentoring(
            @AuthenticationPrincipal Long mentorId,
            @PathVariable Long notificationId
    ) {
        MentoringResponse response = notificationService.acceptMentoringFromNotification(mentorId, notificationId);
        return ApiResponse.success(SuccessStatus.MENTORING_ACCEPT_SUCCESS, response);
    }

    /**
     * 알림에서 멘토 신청 거절
     * - 멘토가 MENTORING_REQUEST 알림에서 거절 버튼 클릭 시 호출
     * - 알림의 payload에서 mentoringRelationId를 꺼내 멘토링 관계를 REJECTED로 변경
     * - 멘티에게 별도 알림 없음
     */
    @Operation(summary = "알림에서 멘토 신청 거절")
    @PostMapping("/{notificationId}/mentor-request/reject")
    public ResponseEntity<ApiResponse<MentoringResponse>> rejectMentoring(
            @AuthenticationPrincipal Long mentorId,
            @PathVariable Long notificationId
    ) {
        MentoringResponse response = notificationService.rejectMentoringFromNotification(mentorId, notificationId);
        return ApiResponse.success(SuccessStatus.MENTORING_REJECT_SUCCESS, response);
    }

    @Operation(summary = "알림 목록 조회", description = "전체 또는 카테고리별 알림 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) NotificationCategory category) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, notificationService.getNotifications(userId, category));
    }

    @Operation(summary = "미읽음 알림 수 조회", description = "전체 및 카테고리별 미읽음 알림 수를 조회합니다.")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationCountResponse>> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, notificationService.getUnreadCount(userId));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(userId, notificationId);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, null);
    }
}
