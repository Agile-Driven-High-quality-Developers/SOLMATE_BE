package org.solmate.domain.stock.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.domain.stock.service.CandleLoadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
        log.info("[캔들 일괄 적재 요청] 종목 수={}, 분봉={}일, 일봉={}일", codes.size(), minuteDays, dailyDays);

        String today      = LocalDate.now().format(DATE_FMT);
        String minuteFrom = LocalDate.now().minusDays(minuteDays).format(DATE_FMT);
        String dailyFrom  = LocalDate.now().minusDays(dailyDays).format(DATE_FMT);

        for (String code : codes) {
            candleLoadService.loadMinuteCandles(code, minuteFrom, today);
            candleLoadService.loadDailyCandles(code, dailyFrom, today);
        }

        log.info("[캔들 일괄 적재 완료]");
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }
}
