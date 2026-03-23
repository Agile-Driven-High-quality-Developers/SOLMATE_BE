package org.solmate.domain.market.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.market.dto.response.MarketIndicatorResponse;
import org.solmate.domain.market.service.MarketIndicatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Market", description = "지수 API")
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketIndicatorController {

    private final MarketIndicatorService marketIndicatorService;

    @Operation(summary = "시장 지표 조회 (KOSPI, KOSDAQ, USD/KRW)")
    @GetMapping("/market-indicators")
    public ResponseEntity<ApiResponse<MarketIndicatorResponse>> getMarketIndicators() {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, marketIndicatorService.getMarketIndicators());
    }
}