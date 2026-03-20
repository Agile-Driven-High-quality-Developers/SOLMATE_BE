package org.solmate.domain.trade.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.trade.dto.response.TradeHistoryResponse;
import org.solmate.domain.trade.service.TradeHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Trade", description = "주문 조회/취소 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TradeController {

    private final TradeHistoryService tradeHistoryService;

    /**
     * 거래 주문 목록 조회 (스크롤 기반)
     */
    @Operation(summary = "거래 주문 목록 조회")
    @GetMapping("/stocks/{stockCode}/orders")
    public ResponseEntity<ApiResponse<List<TradeHistoryResponse>>> getOrders(
            @AuthenticationPrincipal Long userId,
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "10") int limit,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorCreatedAt,

            @RequestParam(required = false)
            Long cursorId
    ) {
        List<TradeHistoryResponse> response =
                tradeHistoryService.getOrders(userId, stockCode, limit, cursorCreatedAt, cursorId);

        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    /**
     * 주문 취소
     */
    @Operation(summary = "주문 취소")
    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        tradeHistoryService.cancelOrder(userId, orderId);
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }
}