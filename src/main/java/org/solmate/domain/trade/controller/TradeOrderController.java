package org.solmate.domain.trade.controller;

import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.trade.dto.response.TradeOrderResponse;
import org.solmate.domain.trade.service.TradeOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Trade", description = "거래 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;

    @Operation(summary = "종목별 주문 목록 조회")
    @GetMapping("/stocks/{stockCode}/orders")
    public ResponseEntity<ApiResponse<List<TradeOrderResponse>>> getOrders(
            @AuthenticationPrincipal Long userId,
            @PathVariable String stockCode
    ) {
        List<TradeOrderResponse> response = tradeOrderService.getOrders(userId, stockCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @Operation(summary = "주문 취소")
    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        tradeOrderService.cancelOrder(userId, orderId);
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }
}
