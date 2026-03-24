package org.solmate.domain.stock.controller;

import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.stock.dto.response.CandleResponse;
import org.solmate.domain.stock.dto.response.StockListResponse;
import org.solmate.domain.stock.dto.response.StockQuoteResponse;
import org.solmate.domain.stock.service.CandleService;
import org.solmate.domain.stock.dto.response.StockOrderBookResponse;
import org.solmate.domain.stock.service.OrderBookService;
import org.solmate.domain.stock.service.StockService;
import org.solmate.external.ls.websocket.LsWebSocketClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final CandleService candleService;
    private final OrderBookService orderBookService;
    private final LsWebSocketClient lsWebSocketClient;

    @Operation(summary = "종목 리스트 조회 (현재가 포함)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StockListResponse>>> getStockList() {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, stockService.getStockList());
    }

    @Operation(summary = "주식 현재가 조회")
    @GetMapping("/{stockCode}/quote")
    public ResponseEntity<ApiResponse<StockQuoteResponse>> getQuote(@PathVariable String stockCode) {
        StockQuoteResponse response = stockService.getQuote(stockCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @Operation(
        summary = "분봉 조회",
        description = "unit=1: 1분봉(DB 직접 조회) / unit=5|30|60: DB 1분봉 집계 + Redis 현재 봉. 오늘 장 시간(09:00~15:30) 기준."
    )
    @GetMapping("/{stockCode}/candles/minute")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getMinuteCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "1") int unit) {
        if (unit != 1 && unit != 5 && unit != 30 && unit != 60) {
            throw new org.solmate.common.exception.GeneralException(
                    org.solmate.common.status.ErrorStatus.INVALID_CANDLE_UNIT);
        }
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getMinuteCandles(stockCode, unit));
    }

    @Operation(summary = "일봉 조회", description = "days 파라미터로 조회 기간 지정 (기본 30일)")
    @GetMapping("/{stockCode}/candles/daily")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getDailyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getDailyCandles(stockCode, days));
    }

    @Operation(summary = "주봉 조회", description = "weeks 파라미터로 조회 기간 지정 (기본 52주). DB 일봉 집계.")
    @GetMapping("/{stockCode}/candles/weekly")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getWeeklyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "52") int weeks) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getWeeklyCandles(stockCode, weeks));
    }

    @Operation(summary = "월봉 조회", description = "months 파라미터로 조회 기간 지정 (기본 60개월). DB 일봉 집계.")
    @GetMapping("/{stockCode}/candles/monthly")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getMonthlyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "60") int months) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getMonthlyCandles(stockCode, months));
    }

    @Operation(summary = "년봉 조회", description = "years 파라미터로 조회 기간 지정 (기본 10년). DB 일봉 집계.")
    @GetMapping("/{stockCode}/candles/yearly")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getYearlyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "10") int years) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getYearlyCandles(stockCode, years));
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
