package org.solmate.domain.stock.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.stock.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/stocks")
public class StockTotalController {

    private final StockService stockService;

    @Operation(summary = "시가총액 수동 업데이트", description = "전체 종목의 시가총액을 수동으로 업데이트합니다.")
    @PostMapping("/market-cap/update")
    public ResponseEntity<ApiResponse<Void>> updateMarketCap() {
        stockService.updateAllMarketCap();
        return ApiResponse.success(SuccessStatus.MARKET_CAP_UPDATE_SUCCESS);
    }
}
