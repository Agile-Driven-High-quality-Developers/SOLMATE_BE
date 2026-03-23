package org.solmate.domain.trade.controller;

import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.trade.dto.request.UpdateTradeDiaryRequest;
import org.solmate.domain.trade.dto.response.TradeDiaryDetailResponse;
import org.solmate.domain.trade.dto.response.TradeDiaryListResponse;
import org.solmate.domain.trade.service.TradeDiaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "TradeDiary", description = "매매일지 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class TradeDiaryController {

    private final TradeDiaryService tradeDiaryService;

    @Operation(summary = "내 매매일지 목록 조회", description = "내 매매일지 목록을 최신순으로 조회합니다. stockName으로 종목명 검색 가능합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TradeDiaryListResponse>>> getMyDiaries(
            Authentication authentication,
            @RequestParam(required = false) String stockName) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(SuccessStatus.SUCCESS_200, tradeDiaryService.getMyDiaries(userId, stockName));
    }

    @Operation(summary = "매매일지 상세 조회", description = "매매일지 상세 정보와 댓글 목록을 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<TradeDiaryDetailResponse>> getDiaryDetail(
            @PathVariable Long diaryId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(SuccessStatus.SUCCESS_200, tradeDiaryService.getDiaryDetail(diaryId, userId));
    }

    @Operation(summary = "매매일지 수정", description = "매매일지 내용을 수정합니다.")
    @PatchMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<TradeDiaryDetailResponse>> updateDiary(
            @PathVariable Long diaryId,
            Authentication authentication,
            @RequestBody UpdateTradeDiaryRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(SuccessStatus.SUCCESS_200, tradeDiaryService.updateDiary(diaryId, userId, request));
    }
}
