package org.solmate.external.ls.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.solmate.domain.stock.dto.response.StockRealtimeResponse;
import org.solmate.domain.stock.service.CandleAccumulatorService;
import org.solmate.domain.stock.service.OrderBookService;
import org.solmate.external.ls.LsProperties;
import org.solmate.external.ls.dto.websocket.LsWsOrderBookResponse;
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

import com.fasterxml.jackson.databind.JsonNode;
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
    private final OrderBookService orderBookService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();

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
                    scheduleReconnect();
                } else {
                    this.session = sess;
                    log.info("LS WebSocket 연결 성공");
                    resubscribeAll();
                }
            });
        } catch (Exception e) {
            log.error("LS WebSocket 연결 오류", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        log.info("LS WebSocket 5초 후 재연결 시도");
        reconnectScheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    private void resubscribeAll() {
        if (subscribedCodes.isEmpty()) return;
        log.info("LS WebSocket 재구독 시도: {}", subscribedCodes);
        String token = lsTokenService.getToken();
        subscribedCodes.forEach(code -> {
            sendMessage(LsWsRequest.subscribe(token, code));
            sendMessage(LsWsRequest.subscribeOrderBook(token, code));
        });
    }

    public void subscribe(String stockCode) {
        if (subscribedCodes.contains(stockCode)) return;
        String token = lsTokenService.getToken();
        sendMessage(LsWsRequest.subscribe(token, stockCode));
        sendMessage(LsWsRequest.subscribeOrderBook(token, stockCode));
        subscribedCodes.add(stockCode);
        log.info("LS WebSocket 구독: {}", stockCode);
    }

    public void unsubscribe(String stockCode) {
        if (!subscribedCodes.contains(stockCode)) return;
        String token = lsTokenService.getToken();
        sendMessage(LsWsRequest.unsubscribe(token, stockCode));
        sendMessage(LsWsRequest.unsubscribeOrderBook(token, stockCode));
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
            log.info("LS WebSocket 전송: {}", json);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("LS WebSocket 메시지 전송 실패", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.info("LS WebSocket 수신: {}", payload);

            // tr_cd로 라우팅
            JsonNode node = objectMapper.readTree(payload);
            JsonNode headerNode = node.get("header");
            if (headerNode == null) return;

            String trCd = headerNode.path("tr_cd").asText();

            if ("US3".equals(trCd)) {
                LsWsStockResponse response = objectMapper.treeToValue(node, LsWsStockResponse.class);
                if (response.body() == null) return;
                candleAccumulatorService.accumulate(response.body());
                messagingTemplate.convertAndSend("/topic/stocks/" + response.body().shcode(),
                        StockRealtimeResponse.from(response.body()));

            } else if ("UH1".equals(trCd)) {
                LsWsOrderBookResponse response = objectMapper.treeToValue(node, LsWsOrderBookResponse.class);
                if (response.body() == null) return;
                orderBookService.save(response.body());
            }
        } catch (Exception e) {
            log.warn("LS WebSocket 메시지 파싱 실패: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("LS WebSocket 연결 종료: {}", status);
        this.session = null;
        lsTokenService.clearToken();
        scheduleReconnect();
    }
}
