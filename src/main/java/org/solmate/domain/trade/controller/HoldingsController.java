package org.solmate.domain.trade.controller;

import java.math.BigDecimal;
import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.trade.dto.response.HoldingsResponse;
import org.solmate.domain.trade.service.HoldingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Holdings", description = "보유 종목 API")
@RestController
@RequestMapping("/api/holdings")
@RequiredArgsConstructor
public class HoldingsController {

    private final HoldingsService holdingsService;

    @Operation(summary = "보유 종목 조회", description = "로그인한 사용자의 보유 종목 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HoldingsResponse>>> getHoldings(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessStatus.HOLDINGS_SUCCESS, holdingsService.getHoldings(userId));
    }


    @Operation(summary = "타인 보유 종목 조회", description = "타인의 보유 종목 목록을 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<HoldingsResponse>>> getOtherHoldings(@PathVariable Long userId) {
        return ApiResponse.success(SuccessStatus.HOLDINGS_SUCCESS, holdingsService.getHoldings(userId));
    }

    @Operation(summary = "보유 현금 조회", description = "계좌 잔고 + 미체결 매수 주문 금액을 합산한 실제 보유 현금을 조회합니다.")
    @GetMapping("/cash")
    public ResponseEntity<ApiResponse<BigDecimal>> getCash(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, holdingsService.getCash(userId));
    }
}
