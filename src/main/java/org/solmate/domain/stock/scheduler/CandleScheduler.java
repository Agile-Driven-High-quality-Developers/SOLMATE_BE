package org.solmate.domain.stock.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.solmate.domain.stock.service.CandleAccumulatorService;
import org.solmate.domain.stock.service.CandleLoadService;
import org.solmate.external.ls.websocket.LsWebSocketClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class CandleScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CandleAccumulatorService candleAccumulatorService;
    private final CandleLoadService candleLoadService;
    private final LsWebSocketClient lsWebSocketClient;

    // 매 분 00초 - 1분봉 DB 저장
    @Scheduled(cron = "0 * * * * MON-FRI")
    public void flushMinuteCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        log.debug("1분봉 스케줄러 실행 - 구독 종목 수: {}", codes.size());
        codes.forEach(candleAccumulatorService::flushMinuteCandle);
    }

    // 매 5분 - 5분봉 Redis 초기화
    @Scheduled(cron = "0 */5 * * * MON-FRI")
    public void flush5MinCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        log.debug("5분봉 스케줄러 실행");
        codes.forEach(candleAccumulatorService::flush5MinCandle);
    }

    // 매 30분 - 30분봉 Redis 초기화
    @Scheduled(cron = "0 */30 * * * MON-FRI")
    public void flush30MinCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        log.debug("30분봉 스케줄러 실행");
        codes.forEach(candleAccumulatorService::flush30MinCandle);
    }

    // 매 정시 - 60분봉 Redis 초기화
    @Scheduled(cron = "0 0 * * * MON-FRI")
    public void flush60MinCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        log.debug("60분봉 스케줄러 실행");
        codes.forEach(candleAccumulatorService::flush60MinCandle);
    }

    // 에프터마켓 종료 20:00 - 일봉 DB 저장
    @Scheduled(cron = "0 0 20 * * MON-FRI")
    public void flushDailyCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        log.debug("일봉 스케줄러 실행");
        codes.forEach(candleAccumulatorService::flushDailyCandle);
    }

    // 매일 오전 07:00 - 전 영업일 분봉/일봉 백필 (서버 다운 등으로 누락된 데이터 보정)
    @Async
    @Scheduled(cron = "0 0 7 * * MON-FRI")
    public void backfillCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        if (codes.isEmpty()) return;

        String yesterday = lastTradingDay(LocalDate.now(KST)).format(DATE_FMT);
        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 백필 시작] 종목={}개, 대상={}", codes.size(), yesterday);
        log.info("└─────────────────────────────────────────────");

        int minuteSuccess = 0, minuteFail = 0, dailySuccess = 0, dailyFail = 0;
        for (String code : codes) {
            if (candleLoadService.loadUnifiedMinuteCandles(code, yesterday, yesterday, "U")) minuteSuccess++;
            else minuteFail++;
            if (candleLoadService.loadUnifiedDailyCandles(code, yesterday, yesterday)) dailySuccess++;
            else dailyFail++;
        }
        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 백필 완료] 분봉 성공={}건/실패={}건, 일봉 성공={}건/실패={}건",
                minuteSuccess, minuteFail, dailySuccess, dailyFail);
        log.info("└─────────────────────────────────────────────");
    }

    // 직전 영업일 계산 (월요일이면 금요일, 그 외는 어제)
    private LocalDate lastTradingDay(LocalDate date) {
        LocalDate prev = date.minusDays(1);
        if (prev.getDayOfWeek() == DayOfWeek.SUNDAY)   return prev.minusDays(1); // 일 → 금
        if (prev.getDayOfWeek() == DayOfWeek.SATURDAY) return prev.minusDays(1); // 토 → 금
        return prev;
    }
}
