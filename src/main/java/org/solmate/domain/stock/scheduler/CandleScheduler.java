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

    // 매 분 00초에 실행
    @Scheduled(cron = "0 * * * * *")
    public void flushMinuteCandles() {
        Set<String> subscribedCodes = lsWebSocketClient.getSubscribedCodes();
        log.info("1분봉 스케줄러 실행 - 구독 종목 수: {}", subscribedCodes.size());
        subscribedCodes.forEach(candleAccumulatorService::flushToDb);
    }
}
