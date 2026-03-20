package org.solmate.domain.stock.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.stock.dto.response.StockOrderBookResponse;
import org.solmate.domain.stock.dto.response.StockQuoteResponse;
import org.solmate.domain.stock.service.OrderBookService;
import org.solmate.domain.stock.service.StockService;
import org.solmate.external.ls.websocket.LsWebSocketClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Stock", description = "주식 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;
    private final OrderBookService orderBookService;
    private final LsWebSocketClient lsWebSocketClient;

    @Operation(summary = "주식 현재가 조회")
    @GetMapping("/{stockCode}/quote")
    public ResponseEntity<ApiResponse<StockQuoteResponse>> getQuote(@PathVariable String stockCode) {
        StockQuoteResponse response = stockService.getQuote(stockCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @Operation(summary = "주식 실시간 시세 구독")
    @PostMapping("/{stockCode}/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(@PathVariable String stockCode) {
        lsWebSocketClient.subscribe(stockCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @Operation(summary = "주식 실시간 시세 구독 해제")
    @DeleteMapping("/{stockCode}/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(@PathVariable String stockCode) {
        lsWebSocketClient.unsubscribe(stockCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @Operation(summary = "실시간 호가 조회 (매도 5단계 / 매수 5단계)")
    @GetMapping("/{stockCode}/orderbook")
    public ResponseEntity<ApiResponse<StockOrderBookResponse>> getOrderBook(@PathVariable String stockCode) {
        StockOrderBookResponse response = orderBookService.getOrderBook(stockCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }
}
