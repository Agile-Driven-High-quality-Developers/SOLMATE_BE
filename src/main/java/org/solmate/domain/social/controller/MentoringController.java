package org.solmate.domain.social.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.social.dto.request.MentoringRequest;
import org.solmate.domain.social.dto.response.MentoringResponse;
import org.solmate.domain.social.service.MentoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Mentoring", description = "멘토링 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MentoringController {

    private final MentoringService mentoringService;

    /**
     * 멘토 신청
     * - 유저 목록에서 멘토 신청 버튼 클릭 시 호출
     * - 신청 성공 시 멘토에게 알림 전송 (payload에 mentoringRelationId 포함)
     */
    @Operation(summary = "멘토 신청")
    @PostMapping("/mentor-requests")
    public ResponseEntity<ApiResponse<MentoringResponse>> requestMentoring(
            @AuthenticationPrincipal Long menteeId,
            @RequestBody MentoringRequest request
    ) {
        MentoringResponse response = mentoringService.requestMentoring(menteeId, request.mentorUserId());
        return ApiResponse.success(SuccessStatus.MENTORING_REQUEST_SUCCESS, response);
    }

    /**
     * 멘토링 취소 (멘티만 가능)
     * - PENDING(신청 대기) 또는 ACCEPTED(수락된 관계) 상태 모두 취소 가능
     * - 멘토가 호출하면 서비스 레이어에서 403 반환
     */
    @Operation(summary = "멘토링 취소")
    @DeleteMapping("/mentor-requests/{relationId}")
    public ResponseEntity<ApiResponse<Void>> cancelMentoring(
            @AuthenticationPrincipal Long menteeId,
            @PathVariable Long relationId
    ) {
        mentoringService.cancelMentoring(menteeId, relationId);
        return ApiResponse.success(SuccessStatus.MENTORING_CANCEL_SUCCESS, null);
    }
}
