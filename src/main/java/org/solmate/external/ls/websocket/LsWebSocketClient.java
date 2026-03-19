package org.solmate.external.ls.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.solmate.domain.stock.service.CandleAccumulatorService;
import org.solmate.external.ls.LsProperties;
import org.solmate.external.ls.dto.websocket.LsWsRequest;
import org.solmate.external.ls.dto.websocket.LsWsStockResponse;
import org.solmate.external.ls.service.LsTokenService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsWebSocketClient extends TextWebSocketHandler {

    private final LsProperties lsProperties;
    private final LsTokenService lsTokenService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CandleAccumulatorService candleAccumulatorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketSession session;
    private final Set<String> subscribedCodes = ConcurrentHashMap.newKeySet();

    public Set<String> getSubscribedCodes() {
        return subscribedCodes;
    }

    @PostConstruct
    public void connect() {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            client.execute(this, lsProperties.getWsUrl()).whenComplete((sess, ex) -> {
                if (ex != null) {
                    log.error("LS WebSocket 연결 실패", ex);
                } else {
                    this.session = sess;
                    log.info("LS WebSocket 연결 성공");
                }
            });
        } catch (Exception e) {
            log.error("LS WebSocket 연결 오류", e);
        }
    }

    public void subscribe(String stockCode) {
        if (subscribedCodes.contains(stockCode)) return;
        sendMessage(LsWsRequest.subscribe(lsTokenService.getToken(), stockCode));
        subscribedCodes.add(stockCode);
        log.info("LS WebSocket 구독: {}", stockCode);
    }

    public void unsubscribe(String stockCode) {
        if (!subscribedCodes.contains(stockCode)) return;
        sendMessage(LsWsRequest.unsubscribe(lsTokenService.getToken(), stockCode));
        subscribedCodes.remove(stockCode);
        log.info("LS WebSocket 구독 해제: {}", stockCode);
    }

    private void sendMessage(LsWsRequest request) {
        try {
            if (session == null || !session.isOpen()) {
                log.warn("LS WebSocket 세션이 열려있지 않습니다.");
                return;
            }
            String json = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("LS WebSocket 메시지 전송 실패", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            log.info("LS WebSocket 수신: {}", message.getPayload());
            LsWsStockResponse response = objectMapper.readValue(message.getPayload(), LsWsStockResponse.class);
            if (response.header() == null || response.body() == null) return;

            String stockCode = response.header().tr_key();
            candleAccumulatorService.accumulate(response.body());
            messagingTemplate.convertAndSend("/topic/stocks/" + stockCode, response.body());
        } catch (Exception e) {
            log.warn("LS WebSocket 메시지 파싱 실패: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("LS WebSocket 연결 종료: {}", status);
        this.session = null;
        subscribedCodes.clear();
    }
}
