package org.solmate.domain.stock.controller;

import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.stock.dto.response.CandleResponse;
import org.solmate.domain.stock.dto.response.StockHoldingResponse;
import org.solmate.domain.stock.dto.response.StockListResponse;
import org.solmate.domain.stock.dto.response.StockQuoteResponse;
import org.solmate.domain.stock.service.CandleService;
import org.solmate.domain.stock.dto.response.StockOrderBookResponse;
import org.solmate.domain.stock.service.OrderBookService;
import org.solmate.domain.stock.service.StockService;
import org.solmate.domain.trade.service.HoldingsService;
import org.solmate.external.ls.websocket.LsWebSocketClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final HoldingsService holdingsService;
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
        description = "unit=1: 1분봉 / unit=5|30|60: DB 1분봉 집계 + Redis 현재 봉. " +
                      "days 미지정 시 unit별 기본값 적용 (1분=5일, 5분=20일, 30분=60일, 60분=90일). " +
                      "to 지정 시 해당 Unix epoch(초) 이전 데이터 조회 (TradingView 무한스크롤용)"
    )
    @GetMapping("/{stockCode}/candles/minute")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getMinuteCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "1") int unit,
            @RequestParam(defaultValue = "0") int days,
            @RequestParam(required = false) Long to) {
        if (unit != 1 && unit != 5 && unit != 30 && unit != 60) {
            throw new org.solmate.common.exception.GeneralException(
                    org.solmate.common.status.ErrorStatus.INVALID_CANDLE_UNIT);
        }
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getMinuteCandles(stockCode, unit, days, to));
    }

    @Operation(summary = "일봉 조회", description = "days 미지정 시 365일. to 지정 시 해당 Unix epoch(초) 이전 데이터 조회 (TradingView 무한스크롤용)")
    @GetMapping("/{stockCode}/candles/daily")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getDailyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "365") int days,
            @RequestParam(required = false) Long to) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getDailyCandles(stockCode, days, to));
    }

    @Operation(summary = "주봉 조회", description = "weeks 미지정 시 260주(5년). DB 일봉 집계. to 지정 시 해당 Unix epoch(초) 이전 데이터 조회 (TradingView 무한스크롤용)")
    @GetMapping("/{stockCode}/candles/weekly")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getWeeklyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "260") int weeks,
            @RequestParam(required = false) Long to) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getWeeklyCandles(stockCode, weeks, to));
    }

    @Operation(summary = "월봉 조회", description = "months 미지정 시 120개월(10년). DB 일봉 집계. to 지정 시 해당 Unix epoch(초) 이전 데이터 조회 (TradingView 무한스크롤용)")
    @GetMapping("/{stockCode}/candles/monthly")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getMonthlyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "120") int months,
            @RequestParam(required = false) Long to) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getMonthlyCandles(stockCode, months, to));
    }

    @Operation(summary = "년봉 조회", description = "years 미지정 시 20년. DB 일봉 집계. to 지정 시 해당 Unix epoch(초) 이전 데이터 조회 (TradingView 무한스크롤용)")
    @GetMapping("/{stockCode}/candles/yearly")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getYearlyCandles(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "20") int years,
            @RequestParam(required = false) Long to) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getYearlyCandles(stockCode, years, to));
    }

    @Operation(summary = "[벤치마크] 5분봉 직접 조회 (five_minute_candle)")
    @GetMapping("/{stockCode}/candles/five-minute/table")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getFiveMinFromTable(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "20") int days) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getFiveMinFromTable(stockCode, days));
    }

    @Operation(summary = "[벤치마크] 5분봉 집계 조회 (minute_candle)")
    @GetMapping("/{stockCode}/candles/five-minute/aggregate")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getFiveMinFromAggregate(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "20") int days) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getFiveMinFromAggregate(stockCode, days));
    }

    @Operation(summary = "[벤치마크] 30분봉 직접 조회 (thirty_minute_candle)")
    @GetMapping("/{stockCode}/candles/thirty-minute/table")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getThirtyMinFromTable(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "46") int days) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getThirtyMinFromTable(stockCode, days));
    }

    @Operation(summary = "[벤치마크] 30분봉 집계 조회 (minute_candle)")
    @GetMapping("/{stockCode}/candles/thirty-minute/aggregate")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getThirtyMinFromAggregate(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "46") int days) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getThirtyMinFromAggregate(stockCode, days));
    }

    @Operation(summary = "[벤치마크] 60분봉 직접 조회 (thirty_minute_candle 집계)")
    @GetMapping("/{stockCode}/candles/sixty-minute/table")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getSixtyMinFromTable(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "90") int days) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getSixtyMinFromTable(stockCode, days));
    }

    @Operation(summary = "[벤치마크] 60분봉 집계 조회 (minute_candle)")
    @GetMapping("/{stockCode}/candles/sixty-minute/aggregate")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getSixtyMinFromAggregate(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "90") int days) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getSixtyMinFromAggregate(stockCode, days));
    }

    @Operation(summary = "[벤치마크] 주봉 직접 조회 (weekly_candle)")
    @GetMapping("/{stockCode}/candles/weekly-bench/table")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getWeeklyFromTable(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "260") int weeks) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getWeeklyFromTable(stockCode, weeks));
    }

    @Operation(summary = "[벤치마크] 주봉 집계 조회 (daily_candle)")
    @GetMapping("/{stockCode}/candles/weekly-bench/aggregate")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getWeeklyFromAggregate(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "260") int weeks) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getWeeklyFromAggregate(stockCode, weeks));
    }

    @Operation(summary = "[벤치마크] 월봉 직접 조회 (monthly_candle)")
    @GetMapping("/{stockCode}/candles/monthly-bench/table")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getMonthlyFromTable(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "120") int months) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getMonthlyFromTable(stockCode, months));
    }

    @Operation(summary = "[벤치마크] 월봉 집계 조회 (daily_candle)")
    @GetMapping("/{stockCode}/candles/monthly-bench/aggregate")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getMonthlyFromAggregate(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "120") int months) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getMonthlyFromAggregate(stockCode, months));
    }

    @Operation(summary = "[벤치마크] 년봉 직접 조회 (monthly_candle 집계)")
    @GetMapping("/{stockCode}/candles/yearly-bench/table")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getYearlyFromTable(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "20") int years) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getYearlyFromTable(stockCode, years));
    }

    @Operation(summary = "[벤치마크] 년봉 집계 조회 (daily_candle)")
    @GetMapping("/{stockCode}/candles/yearly-bench/aggregate")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getYearlyFromAggregate(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "20") int years) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, candleService.getYearlyFromAggregate(stockCode, years));
    }

    @Operation(summary = "종목 보유현황 조회", description = "보유수량/평균매수가/평가금액/수익률 조회. 수익률은 호출 시점 현재가 기준.")
    @GetMapping("/{stockCode}/holding")
    public ResponseEntity<ApiResponse<StockHoldingResponse>> getStockHolding(
            @PathVariable String stockCode,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, holdingsService.getStockHolding(userId, stockCode));
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
