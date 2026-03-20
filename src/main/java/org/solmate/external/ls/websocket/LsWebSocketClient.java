package org.solmate.external.ls.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.solmate.domain.stock.dto.response.StockRealtimeResponse;
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

import org.solmate.external.ls.dto.websocket.LsWsIndexResponse;
import org.solmate.external.ls.dto.websocket.LsWsCurrencyResponse;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsWebSocketClient extends TextWebSocketHandler {

    private final LsProperties lsProperties;
    private final LsTokenService lsTokenService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CandleAccumulatorService candleAccumulatorService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocketSession session;
    private final Set<String> subscribedCodes = ConcurrentHashMap.newKeySet();

    public Set<String> getSubscribedCodes() {
        return subscribedCodes;
    }

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KOSPI_REDIS_KEY = "market:indicator:KOSPI";
    private static final String KOSDAQ_REDIS_KEY = "market:indicator:KOSDAQ";
    private static final String USD_KRW_REDIS_KEY = "market:indicator:USD_KRW";

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

                    String token = lsTokenService.getToken();
                    sendMessage(LsWsRequest.subscribeIndex(token, "001")); // KOSPI
                    sendMessage(LsWsRequest.subscribeIndex(token, "301")); // KOSDAQ
                    sendMessage(LsWsRequest.subscribeCurrency(token, "USD")); // 환율
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
        subscribedCodes.forEach(code -> sendMessage(LsWsRequest.subscribe(token, code)));
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
            log.info("LS WebSocket 전송: {}", json);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("LS WebSocket 메시지 전송 실패", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            log.info("LS WebSocket 수신: {}", message.getPayload());

            // tr_cd 먼저 확인
            var root = objectMapper.readTree(message.getPayload());
            var header = root.get("header");
            if (header == null || root.get("body") == null || root.get("body").isNull()) return;

            String trCd = header.get("tr_cd") != null ? header.get("tr_cd").asText() : "";

            // 지수 데이터 처리
            if ("IJ_".equals(trCd)) {
                LsWsIndexResponse response = objectMapper.readValue(message.getPayload(), LsWsIndexResponse.class);
                handleIndexMessage(response);
                return;
            }

            // 환율 추가
            if ("CUR".equals(trCd)) {
                LsWsCurrencyResponse response = objectMapper.readValue(message.getPayload(), LsWsCurrencyResponse.class);
                handleCurrencyMessage(response);
                return;
            }

            // 종목 데이터 처리 (기존 코드)
            LsWsStockResponse response = objectMapper.readValue(message.getPayload(), LsWsStockResponse.class);
            if (response.header() == null || response.body() == null) return;

            String stockCode = response.body().shcode();
            candleAccumulatorService.accumulate(response.body());
            messagingTemplate.convertAndSend("/topic/stocks/" + stockCode, StockRealtimeResponse.from(response.body()));

        } catch (Exception e) {
            log.warn("LS WebSocket 메시지 파싱 실패: {}", message.getPayload());
        }
    }

    // 지수 처리 메서드 추가
    private void handleIndexMessage(LsWsIndexResponse response) {
        try {
            if (response.body() == null) return;

            String trKey = response.header().tr_key();
            String redisKey = "001".equals(trKey) ? KOSPI_REDIS_KEY : KOSDAQ_REDIS_KEY;

            String json = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("cur", response.body().jisu())
                            .put("change", response.body().change())
                            .put("rate", response.body().drate())
                            .put("sign", response.body().sign())
                            .put("high", response.body().highjisu())
                            .put("low", response.body().lowjisu())
                            .put("asOf", response.body().time())
            );

            stringRedisTemplate.opsForValue().set(redisKey, json);
            log.info("시장 지표 Redis 저장 완료 - {}: {}", redisKey, json);

        } catch (Exception e) {
            log.error("시장 지표 Redis 저장 실패: {}", e.getMessage());
        }
    }

    private void handleCurrencyMessage(LsWsCurrencyResponse response) {
        try {
            if (response.body() == null) return;

            String json = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("cur", response.body().price())
                            .put("change", response.body().change())
                            .put("rate", response.body().drate())
                            .put("sign", response.body().sign())
                            .put("high", response.body().high())
                            .put("low", response.body().low())
                            .put("asOf", response.body().time())
            );

            stringRedisTemplate.opsForValue().set(USD_KRW_REDIS_KEY, json);
            log.info("환율 Redis 저장 완료 - {}: {}", USD_KRW_REDIS_KEY, json);

        } catch (Exception e) {
            log.error("환율 Redis 저장 실패: {}", e.getMessage());
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
