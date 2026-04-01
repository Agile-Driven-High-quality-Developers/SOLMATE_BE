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

    // 매 분 00초: 1분봉 DB 저장
    @Scheduled(cron = "0 * * * * MON-FRI")
    public void flushMinuteCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        codes.forEach(candleAccumulatorService::flushMinuteCandle);
    }

    // 매 5분: 5분봉 DB 저장
    @Scheduled(cron = "0 */5 * * * MON-FRI")
    public void flush5MinCandles() {
        lsWebSocketClient.getSubscribedCodes().forEach(candleAccumulatorService::flush5MinCandle);
    }

    // 매 30분: 30분봉 DB 저장
    @Scheduled(cron = "0 */30 * * * MON-FRI")
    public void flush30MinCandles() {
        lsWebSocketClient.getSubscribedCodes().forEach(candleAccumulatorService::flush30MinCandle);
    }

    // 매 정시: 60분봉 Redis 초기화
    @Scheduled(cron = "0 0 * * * MON-FRI")
    public void flush60MinCandles() {
        lsWebSocketClient.getSubscribedCodes().forEach(candleAccumulatorService::flush60MinCandle);
    }

    // 20:00 에프터마켓 종료: 일봉 DB 저장 + 이번 달 마지막 영업일이면 월봉도 저장
    @Scheduled(cron = "0 0 20 * * MON-FRI")
    public void flushDailyCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        codes.forEach(candleAccumulatorService::flushDailyCandle);

        if (isLastTradingDayOfMonth(LocalDate.now(KST))) {
            log.info("[월봉 flush] 이번 달 마지막 영업일 - 월봉 DB 저장 시작");
            codes.forEach(candleAccumulatorService::flushMonthlyCandle);
        }
    }

    // 매주 금요일 20:00: 주봉 DB 저장
    @Scheduled(cron = "0 0 20 * * FRI")
    public void flushWeeklyCandles() {
        log.info("[주봉 flush] 금요일 장 마감 - 주봉 DB 저장 시작");
        lsWebSocketClient.getSubscribedCodes().forEach(candleAccumulatorService::flushWeeklyCandle);
    }

    // 05:30 전 영업일 캔들 백필 (누락 데이터 보정) - DB 전체 종목 대상
    @Async
    @Scheduled(cron = "0 30 5 * * MON-FRI")
    public void backfillCandles() {
        String yesterday = lastTradingDay(LocalDate.now(KST)).format(DATE_FMT);
        candleLoadService.backfillYesterday(yesterday);
    }

    // 이번 달의 마지막 영업일(월~금)인지 판단
    private boolean isLastTradingDayOfMonth(LocalDate today) {
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
        // 말일부터 역으로 탐색해서 첫 영업일 찾기
        while (lastDay.getDayOfWeek() == DayOfWeek.SATURDAY
                || lastDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lastDay = lastDay.minusDays(1);
        }
        return today.equals(lastDay);
    }

    // 직전 영업일 계산
    private LocalDate lastTradingDay(LocalDate date) {
        LocalDate prev = date.minusDays(1);
        if (prev.getDayOfWeek() == DayOfWeek.SUNDAY)   return prev.minusDays(2);
        if (prev.getDayOfWeek() == DayOfWeek.SATURDAY) return prev.minusDays(1);
        return prev;
    }
}
