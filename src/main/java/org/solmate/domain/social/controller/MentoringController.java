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
     * 멘토 신청 취소 (멘티만 가능)
     * - PENDING(대기 중) 상태의 신청만 취소 가능
     * - 멘토에게 전송된 알림도 함께 삭제
     */
    @Operation(summary = "멘토 신청 취소")
    @DeleteMapping("/mentor-requests/{mentorUserId}/pending")
    public ResponseEntity<ApiResponse<Void>> cancelMentoringRequest(
            @AuthenticationPrincipal Long menteeId,
            @PathVariable Long mentorUserId
    ) {
        mentoringService.cancelMentoringRequest(menteeId, mentorUserId);
        return ApiResponse.success(SuccessStatus.MENTORING_REQUEST_CANCEL_SUCCESS, null);
    }

    /**
     * 멘토링 취소 (멘티만 가능)
     * - ACCEPTED(수락된 관계) 상태만 취소 가능
     * - mentorUserId 기반으로 관계를 조회하므로 프론트에서 relationId 불필요
     */
    @Operation(summary = "멘토링 취소")
    @DeleteMapping("/mentor-requests/{mentorUserId}")
    public ResponseEntity<ApiResponse<Void>> cancelMentoring(
            @AuthenticationPrincipal Long menteeId,
            @PathVariable Long mentorUserId
    ) {
        mentoringService.cancelMentoring(menteeId, mentorUserId);
        return ApiResponse.success(SuccessStatus.MENTORING_CANCEL_SUCCESS, null);
    }
}
