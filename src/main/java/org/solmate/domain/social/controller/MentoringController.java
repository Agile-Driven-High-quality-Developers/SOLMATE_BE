package org.solmate.domain.social.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.social.dto.request.MentoringRequest;
import org.solmate.domain.social.dto.response.MentoringResponse;
import org.solmate.domain.social.dto.response.MyMentoringStatusResponse;
import org.solmate.domain.social.service.MentoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1")
public class MentoringController {

    private final MentoringService mentoringService;

    /**
     * 내 멘토링 신청 현황 조회
     * - 프론트에서 유저 목록의 멘토 신청 버튼 활성화 여부 판단에 사용
     * - hasAcceptedMentor: 이미 수락된 멘토가 있으면 모든 버튼 비활성화
     * - pendingMentorIds: 해당 멘토에게 이미 신청 중이면 그 버튼만 비활성화
     */
    @Operation(summary = "내 멘토링 신청 현황 조회")
    @GetMapping("/mentor-requests/my")
    public ResponseEntity<ApiResponse<MyMentoringStatusResponse>> getMyMentoringStatus(
            @AuthenticationPrincipal Long menteeId
    ) {
        MyMentoringStatusResponse response = mentoringService.getMyMentoringStatus(menteeId);
        return ApiResponse.success(SuccessStatus.MENTORING_MY_STATUS_SUCCESS, response);
    }

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
}
