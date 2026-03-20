package org.solmate.domain.trade.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.trade.dto.request.BuyOrderRequest;
import org.solmate.domain.trade.dto.request.SellOrderRequest;
import org.solmate.domain.trade.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Trade", description = "모의투자 주문 API")
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "매수 주문", description = "시장가/지정가 매수 주문을 접수합니다.")
    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<Long>> buyOrder(Authentication authentication, @RequestBody BuyOrderRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        Long orderId = tradeService.buyOrder(userId, request);
        return ApiResponse.success(SuccessStatus.BUY_ORDER_SUCCESS, orderId);
    }

    @Operation(summary = "매도 주문", description = "시장가/지정가 매도 주문을 접수합니다.")
    @PostMapping("/sell")
    public ResponseEntity<ApiResponse<Long>> sellOrder(Authentication authentication, @RequestBody SellOrderRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        Long orderId = tradeService.sellOrder(userId, request);
        return ApiResponse.success(SuccessStatus.SELL_ORDER_SUCCESS, orderId);
    }
}
