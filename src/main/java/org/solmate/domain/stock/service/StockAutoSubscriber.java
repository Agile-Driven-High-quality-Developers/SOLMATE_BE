package org.solmate.domain.stock.service;

import java.util.List;

import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.external.ls.websocket.LsWebSocketClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAutoSubscriber {

    private final StockRepository stockRepository;
    private final LsWebSocketClient lsWebSocketClient;

    @EventListener(ApplicationReadyEvent.class)
    public void autoSubscribe() {
        List<String> codes = stockRepository.findAllTickerCodes();
        log.info("자동 구독 시작: 총 {}개 종목", codes.size());

        List<String> targets = codes.stream().limit(200).toList();
        for (int i = 0; i < targets.size(); i++) {
            lsWebSocketClient.subscribe(targets.get(i));
            log.info("자동 구독 진행: {}/{} - {}", i + 1, targets.size(), targets.get(i));
            try {
                Thread.sleep(50); // 50ms 간격으로 전송
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("자동 구독 등록 완료: {}개 종목", targets.size());
    }
}
