package org.solmate.domain.stock.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.domain.stock.service.CandleLoadService;
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

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CandleLoadService candleLoadService;
    private final StockRepository stockRepository;

    @Operation(
            summary = "과거 캔들 데이터 일괄 적재",
            description = "전체 종목의 분봉(최근 minuteDays일)과 일봉(최근 dailyDays일)을 LS API로 적재합니다. "
                        + "최초 1회 수동 호출용이며, 이미 DB에 존재하는 데이터는 자동으로 스킵됩니다. "
                        + "200종목 기준 약 20분 소요됩니다."
    )
    @PostMapping("/load")
    public ResponseEntity<ApiResponse<Void>> loadAll(
            @RequestParam(defaultValue = "30") int minuteDays,
            @RequestParam(defaultValue = "3650") int dailyDays
    ) {
        List<String> codes = stockRepository.findAllTickerCodes();
        int total = codes.size();
        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 일괄 적재 시작] 종목={}개, 분봉={}일, 일봉={}일", total, minuteDays, dailyDays);
        log.info("└─────────────────────────────────────────────");

        String today      = LocalDate.now(KST).format(DATE_FMT);
        String minuteFrom = LocalDate.now(KST).minusDays(minuteDays).format(DATE_FMT);
        String dailyFrom  = LocalDate.now(KST).minusDays(dailyDays).format(DATE_FMT);

        int minuteSuccess = 0, minuteFail = 0, dailySuccess = 0, dailyFail = 0;

        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            log.info("[{}/{}] 적재 중: {}", i + 1, total, code);
            if (candleLoadService.loadUnifiedMinuteCandles(code, minuteFrom, today, "U")) minuteSuccess++;
            else minuteFail++;
            if (candleLoadService.loadDailyFromMinuteCandles(code, dailyFrom, today)) dailySuccess++;
            else dailyFail++;
        }

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 일괄 적재 완료]");
        log.info("│ 분봉: 성공 {}건 / 실패 {}건", minuteSuccess, minuteFail);
        log.info("│ 일봉: 성공 {}건 / 실패 {}건", dailySuccess, dailyFail);
        log.info("└─────────────────────────────────────────────");
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @Operation(
            summary = "특정 종목 캔들 단건 적재 (t8412, 정규장만)",
            description = "특정 종목의 분봉/일봉을 LS t8412 API로 적재합니다. 정규장(09:00~15:30)만 제공됩니다. "
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
            boolean ok = candleLoadService.loadMinuteCandles(stockCode, sdate, edate);
            log.info("│ 분봉 적재 {}", ok ? "완료" : "실패");
        }
        if (daily) {
            boolean ok = candleLoadService.loadDailyCandles(stockCode, sdate, edate);
            log.info("│ 일봉 적재 {}", ok ? "완료" : "실패");
        }
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @Operation(
            summary = "특정 종목 통합 분봉 적재 (t8452, NXT 프리/에프터 포함)",
            description = "t8452 API로 KRX+NXT 통합 1분봉을 적재합니다. "
                        + "exchgubun: K=KRX, N=NXT, U=통합(기본). "
                        + "N 또는 U 선택 시 프리마켓(08:00~08:50), 에프터마켓(15:40~20:00) 포함 여부 확인 가능. "
                        + "DB에 이미 존재하는 데이터는 자동으로 스킵됩니다."
    )
    @PostMapping("/stock/{stockCode}/unified")
    public ResponseEntity<ApiResponse<Void>> loadStockUnified(
            @PathVariable String stockCode,
            @RequestParam String sdate,
            @RequestParam String edate,
            @RequestParam(defaultValue = "U") String exchgubun
    ) {
        log.info("┌─────────────────────────────────────────────");
        log.info("│ [통합분봉 적재 시작] stockCode={}, {}~{}, exchgubun={}", stockCode, sdate, edate, exchgubun);
        log.info("└─────────────────────────────────────────────");

        boolean ok = candleLoadService.loadUnifiedMinuteCandles(stockCode, sdate, edate, exchgubun);
        log.info("│ 통합분봉 적재 {}", ok ? "완료" : "실패");
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @Operation(
            summary = "특정 종목 캔들 재적재",
            description = "실패한 종목 코드 목록을 받아 분봉/일봉을 재시도합니다."
    )
    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<Void>> retry(
            @RequestBody List<String> stockCodes,
            @RequestParam(defaultValue = "30") int minuteDays,
            @RequestParam(defaultValue = "3650") int dailyDays
    ) {
        String today      = LocalDate.now(KST).format(DATE_FMT);
        String minuteFrom = LocalDate.now(KST).minusDays(minuteDays).format(DATE_FMT);
        String dailyFrom  = LocalDate.now(KST).minusDays(dailyDays).format(DATE_FMT);

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 재적재 시작] 종목={}개", stockCodes.size());
        log.info("└─────────────────────────────────────────────");

        int minuteSuccess = 0, minuteFail = 0, dailySuccess = 0, dailyFail = 0;

        for (int i = 0; i < stockCodes.size(); i++) {
            String code = stockCodes.get(i);
            log.info("[{}/{}] 재적재 중: {}", i + 1, stockCodes.size(), code);
            if (candleLoadService.loadUnifiedMinuteCandles(code, minuteFrom, today, "U")) minuteSuccess++;
            else minuteFail++;
            if (candleLoadService.loadDailyFromMinuteCandles(code, dailyFrom, today)) dailySuccess++;
            else dailyFail++;
        }

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 재적재 완료]");
        log.info("│ 분봉: 성공 {}건 / 실패 {}건", minuteSuccess, minuteFail);
        log.info("│ 일봉: 성공 {}건 / 실패 {}건", dailySuccess, dailyFail);
        log.info("└─────────────────────────────────────────────");
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }
}
