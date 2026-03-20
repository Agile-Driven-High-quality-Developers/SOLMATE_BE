package org.solmate.domain.market.controller;

import lombok.RequiredArgsConstructor;
import org.solmate.domain.market.dto.response.MarketIndicatorResponse;
import org.solmate.domain.market.service.MarketIndicatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MarketIndicatorController {

    private final MarketIndicatorService marketIndicatorService;

    @GetMapping("/market-indicators")
    public ResponseEntity<MarketIndicatorResponse> getMarketIndicators() {
        return ResponseEntity.ok(marketIndicatorService.getMarketIndicators());
    }
}