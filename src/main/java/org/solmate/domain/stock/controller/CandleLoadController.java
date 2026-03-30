package org.solmate.domain.stock.controller;

import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.stock.service.CandleLoadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/candles")
public class CandleLoadController {

    private final CandleLoadService candleLoadService;

    @Operation(
            summary = "과거 캔들 데이터 일괄 적재",
            description = "전체 종목의 분봉(최근 minuteDays일)과 일봉(최근 dailyDays일)을 통합 API(t8452/t8451)로 적재합니다. "
                        + "백그라운드에서 실행되며 즉시 202를 반환합니다. "
                        + "이미 DB에 존재하는 데이터는 자동으로 스킵됩니다."
    )
    @PostMapping("/load")
    public ResponseEntity<ApiResponse<Void>> loadAll(
            @RequestParam(defaultValue = "30") int minuteDays,
            @RequestParam(defaultValue = "3650") int dailyDays
    ) {
        candleLoadService.loadAllAsync(minuteDays, dailyDays);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new ApiResponse<>(true, SuccessStatus.SUCCESS_200.getCode(), "캔들 일괄 적재가 백그라운드에서 시작됐습니다. 서버 로그를 확인하세요.", null)
        );
    }

    @Operation(
            summary = "특정 종목 캔들 단건 적재 (t8452/t8451, 통합)",
            description = "특정 종목의 분봉/일봉을 통합 API(t8452/t8451)로 적재합니다. "
                        + "KRX+NXT 통합 데이터이며 프리마켓(08:00~08:50), 에프터마켓(15:40~20:00) 포함. "
                        + "DB에 이미 존재하는 데이터는 자동으로 스킵됩니다."
    )
    @PostMapping("/stock/{stockCode}")
    public ResponseEntity<ApiResponse<Void>> loadStock(
            @PathVariable String stockCode,
            @RequestParam String sdate,
            @RequestParam String edate,
            @RequestParam(defaultValue = "true") boolean minute,
            @RequestParam(defaultValue = "true") boolean daily
    ) {
        log.info("┌─────────────────────────────────────────────");
        log.info("│ [종목 캔들 적재 시작] stockCode={}, {}~{}, 분봉={}, 일봉={}",
                stockCode, sdate, edate, minute, daily);
        log.info("└─────────────────────────────────────────────");

        if (minute) {
            boolean ok = candleLoadService.loadUnifiedMinuteCandles(stockCode, sdate, edate, "U");
            log.info("│ 분봉 적재 {}", ok ? "완료" : "실패");
        }
        if (daily) {
            boolean ok = candleLoadService.loadUnifiedDailyCandles(stockCode, sdate, edate);
            log.info("│ 일봉 적재 {}", ok ? "완료" : "실패");
        }
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @Operation(
            summary = "특정 종목 캔들 재적재",
            description = "실패한 종목 코드 목록을 받아 분봉/일봉을 재시도합니다. "
                        + "백그라운드에서 실행되며 즉시 202를 반환합니다."
    )
    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<Void>> retry(
            @RequestBody List<String> stockCodes,
            @RequestParam(defaultValue = "30") int minuteDays,
            @RequestParam(defaultValue = "3650") int dailyDays
    ) {
        candleLoadService.retryAsync(stockCodes, minuteDays, dailyDays);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new ApiResponse<>(true, SuccessStatus.SUCCESS_200.getCode(), "캔들 재적재가 백그라운드에서 시작됐습니다. 서버 로그를 확인하세요.", null)
        );
    }
}
