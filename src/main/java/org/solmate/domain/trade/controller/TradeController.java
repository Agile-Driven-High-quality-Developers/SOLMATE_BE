package org.solmate.domain.trade.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.trade.dto.response.TradeHistoryResponse;
import org.solmate.domain.trade.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Trade", description = "매매 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "종목별 매매내역 리스트 조회")
    @GetMapping("/{tickerCode}")
    public ResponseEntity<ApiResponse<TradeHistoryResponse>> getTradeHistories(
            @PathVariable String tickerCode,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, tradeService.getTradeHistories(userId, tickerCode));
    }
}
