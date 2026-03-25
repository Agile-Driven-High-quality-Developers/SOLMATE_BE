package org.solmate.domain.stock.scheduler;

import org.solmate.domain.stock.service.StockService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class StockScheduler {

    private final StockService stockService;

    // 평일 오전 8:50 - 시가총액 업데이트
    @Scheduled(cron = "0 50 8 * * MON-FRI")
    public void updateMarketCap() {
        log.debug("시가총액 업데이트 스케줄러 실행");
        stockService.updateAllMarketCap();
    }
}
