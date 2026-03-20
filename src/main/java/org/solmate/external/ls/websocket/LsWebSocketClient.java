package org.solmate.external.ls.websocket;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.solmate.domain.stock.dto.response.CandleResponse;
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
import org.solmate.domain.market.service.MarketIndicatorService;

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

    // 현재 구독 중인 종목 코드 목록 반환 (스케줄러에서 사용)
    public Set<String> getSubscribedCodes() {
        return subscribedCodes;
    }

    // 앱 시작 시 LS WebSocket 서버에 자동 연결

    private final MarketIndicatorService marketIndicatorService;

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

    // 연결 실패 또는 종료 시 5초 후 재연결 예약
    private void scheduleReconnect() {
        log.info("LS WebSocket 5초 후 재연결 시도");
        reconnectScheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    // 재연결 후 기존 구독 종목 전체 재구독
    private void resubscribeAll() {
        if (subscribedCodes.isEmpty()) return;
        log.info("LS WebSocket 재구독 시도: {}", subscribedCodes);
        String token = lsTokenService.getToken();
        subscribedCodes.forEach(code -> sendMessage(LsWsRequest.subscribe(token, code)));
    }

    // LS WebSocket에 종목 실시간 체결 구독 요청
    public void subscribe(String stockCode) {
        if (subscribedCodes.contains(stockCode)) return;
        sendMessage(LsWsRequest.subscribe(lsTokenService.getToken(), stockCode));
        subscribedCodes.add(stockCode);
        log.info("LS WebSocket 구독: {}", stockCode);
    }

    // LS WebSocket에 종목 구독 해제 요청
    public void unsubscribe(String stockCode) {
        if (!subscribedCodes.contains(stockCode)) return;
        sendMessage(LsWsRequest.unsubscribe(lsTokenService.getToken(), stockCode));
        subscribedCodes.remove(stockCode);
        log.info("LS WebSocket 구독 해제: {}", stockCode);
    }

    // LS WebSocket 세션으로 JSON 메시지 전송
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

    // LS WebSocket으로부터 체결 데이터 수신 시 봉 누적 및 STOMP 브로드캐스트
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
                marketIndicatorService.saveIndex(response);
                return;
            }

            // 환율 추가
            if ("CUR".equals(trCd)) {
                LsWsCurrencyResponse response = objectMapper.readValue(message.getPayload(), LsWsCurrencyResponse.class);
                marketIndicatorService.saveCurrency(response);
                return;
            }

            // 종목 데이터 처리 (기존 코드)
            LsWsStockResponse response = objectMapper.readValue(message.getPayload(), LsWsStockResponse.class);
            if (response.header() == null || response.body() == null) return;

            String stockCode = response.body().shcode();

            // 모든 봉 Redis 누적
            candleAccumulatorService.accumulate(response.body());

            // 현재가/등락 정보 브로드캐스트 (종목 상단 현재가 표시용)
            messagingTemplate.convertAndSend(
                    "/topic/stocks/" + stockCode + "/quote",
                    StockRealtimeResponse.from(response.body())
            );

            // 봉별 토픽으로 현재 진행 중인 캔들 브로드캐스트
            broadcastCandle(stockCode, "candle:1min:",  "/topic/stocks/" + stockCode + "/candle/1min");
            broadcastCandle(stockCode, "candle:5min:",  "/topic/stocks/" + stockCode + "/candle/5min");
            broadcastCandle(stockCode, "candle:30min:", "/topic/stocks/" + stockCode + "/candle/30min");
            broadcastCandle(stockCode, "candle:60min:", "/topic/stocks/" + stockCode + "/candle/60min");
            broadcastCandle(stockCode, "candle:1day:",  "/topic/stocks/" + stockCode + "/candle/1day");

        } catch (Exception e) {
            log.warn("LS WebSocket 메시지 파싱 실패: {}", message.getPayload());
        }
    }

    // Redis에서 해당 봉 데이터를 읽어 STOMP 토픽으로 브로드캐스트
    private void broadcastCandle(String stockCode, String redisPrefix, String topic) {
        try {
            Map<Object, Object> data = candleAccumulatorService.getCurrentCandle(stockCode, redisPrefix);
            if (data.isEmpty()) return;

            String startTime = (String) data.get("startTime");
            LocalDateTime candleTime = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            messagingTemplate.convertAndSend(topic, CandleResponse.fromRedis(data, candleTime));
        } catch (Exception e) {
            log.warn("캔들 브로드캐스트 실패 - prefix: {}, stockCode: {}", redisPrefix, stockCode);
        }
    }

    // LS WebSocket 연결 종료 시 세션 초기화 후 재연결 시도
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("LS WebSocket 연결 종료: {}", status);
        this.session = null;
        lsTokenService.clearToken();
        scheduleReconnect();
    }
}
