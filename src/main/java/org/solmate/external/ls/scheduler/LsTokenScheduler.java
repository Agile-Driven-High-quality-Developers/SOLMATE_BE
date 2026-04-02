package org.solmate.external.ls.scheduler;

import org.solmate.external.ls.websocket.LsWebSocketClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsTokenScheduler {

    private final LsWebSocketClient lsWebSocketClient;

    @Scheduled(cron = "0 55 6 * * *")
    public void refreshToken() {
        log.info("LS 토큰 만료 예정 - WebSocket 재연결 시작");
        lsWebSocketClient.reconnect();
    }
}
