package org.solmate.domain.stock.scheduler;

import java.util.Set;

import org.solmate.domain.stock.service.CandleAccumulatorService;
import org.solmate.external.ls.websocket.LsWebSocketClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CandleScheduler {

    private final CandleAccumulatorService candleAccumulatorService;
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

    // 장 마감 15:30 - 일봉 DB 저장
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void flushDailyCandles() {
        Set<String> codes = lsWebSocketClient.getSubscribedCodes();
        log.debug("일봉 스케줄러 실행");
        codes.forEach(candleAccumulatorService::flushDailyCandle);
    }
}
