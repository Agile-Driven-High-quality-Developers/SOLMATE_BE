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

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.market.dto.response.MarketIndicatorRealtimeResponse;
import org.solmate.domain.market.service.MarketIndicatorService;
import org.solmate.domain.stock.dto.response.CandleResponse;
import org.solmate.domain.stock.dto.response.StockOrderBookResponse;
import org.solmate.domain.stock.dto.response.StockRealtimeResponse;
import org.solmate.domain.stock.service.CandleAccumulatorService;
import org.solmate.domain.stock.service.OrderBookService;
import org.solmate.domain.stock.service.StockInfoService;
import org.solmate.domain.trade.service.OrderMatchingService;
import org.solmate.external.ls.LsProperties;
import org.solmate.external.ls.dto.websocket.LsWsCurrencyResponse;
import org.solmate.external.ls.dto.websocket.LsWsIndexResponse;
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
    private final StockInfoService stockInfoService;
    private final OrderBookService orderBookService;
    private final MarketIndicatorService marketIndicatorService;
    private final OrderMatchingService orderMatchingService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocketSession session;
    private final Set<String> subscribedCodes = ConcurrentHashMap.newKeySet();

    // 현재 구독 중인 종목 코드 목록 반환 (스케줄러에서 사용)
    public Set<String> getSubscribedCodes() {
        return subscribedCodes;
    }

    public void addSubscribedCode(String stockCode) {
        subscribedCodes.add(stockCode);
    }

    // 앱 시작 시 LS WebSocket 서버에 자동 연결
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

    public void reconnect() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            log.error("LS WebSocket 세션 종료 실패", e);
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
        log.info("LS WebSocket 재구독 시작: {}개 종목", subscribedCodes.size());
        String token = lsTokenService.getToken();
        subscribedCodes.forEach(code -> {
            sendMessage(LsWsRequest.subscribe(token, code));
            sendMessage(LsWsRequest.subscribeOrderBook(token, code));
        });
        log.info("LS WebSocket 재구독 완료: {}개 종목 - {}", subscribedCodes.size(), subscribedCodes);
    }

    // LS WebSocket에 종목 실시간 체결 구독 요청
    public void subscribe(String stockCode) {
        if (subscribedCodes.contains(stockCode)) return;
        String token = lsTokenService.getToken();
        sendMessage(LsWsRequest.subscribe(token, stockCode));
        sendMessage(LsWsRequest.subscribeOrderBook(token, stockCode));
        subscribedCodes.add(stockCode);
        log.debug("LS WebSocket 구독: {}", stockCode);
    }

    public void unsubscribe(String stockCode) {
        if (!subscribedCodes.contains(stockCode)) return;
        String token = lsTokenService.getToken();
        sendMessage(LsWsRequest.unsubscribe(token, stockCode));
        sendMessage(LsWsRequest.unsubscribeOrderBook(token, stockCode));
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
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("LS WebSocket 메시지 전송 실패", e);
        }
    }

    // LS WebSocket으로부터 체결 데이터 수신 시 봉 누적 및 STOMP 브로드캐스트
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            JsonNode node = objectMapper.readTree(payload);
            JsonNode headerNode = node.get("header");
            if (headerNode == null || node.get("body") == null || node.get("body").isNull()) return;

            String trCd = headerNode.path("tr_cd").asText();

            if ("IJ_".equals(trCd)) {
                LsWsIndexResponse response = objectMapper.treeToValue(node, LsWsIndexResponse.class);
                marketIndicatorService.saveIndex(response);
                messagingTemplate.convertAndSend("/topic/market/indicators",
                        new ApiResponse<>(true, SuccessStatus.SUCCESS_200.getCode(), SuccessStatus.SUCCESS_200.getMessage(), MarketIndicatorRealtimeResponse.fromIndex(response)));

            } else if ("CUR".equals(trCd)) {
                LsWsCurrencyResponse response = objectMapper.treeToValue(node, LsWsCurrencyResponse.class);
                marketIndicatorService.saveCurrency(response);
                messagingTemplate.convertAndSend("/topic/market/indicators",
                        new ApiResponse<>(true, SuccessStatus.SUCCESS_200.getCode(), SuccessStatus.SUCCESS_200.getMessage(), MarketIndicatorRealtimeResponse.fromCurrency(response)));

            } else if ("US3".equals(trCd)) {
                LsWsStockResponse response = objectMapper.treeToValue(node, LsWsStockResponse.class);
                if (response.body() == null) return;
                String stockCode = response.body().shcode();
                candleAccumulatorService.accumulate(response.body());
                stockInfoService.update(response.body());
                orderMatchingService.match(stockCode);
                messagingTemplate.convertAndSend("/topic/stocks/" + stockCode + "/quote",
                        StockRealtimeResponse.from(response.body()));
                broadcastCandle(stockCode, "candle:1min:",  "/topic/stocks/" + stockCode + "/candle/1min");
                broadcastCandle(stockCode, "candle:5min:",  "/topic/stocks/" + stockCode + "/candle/5min");
                broadcastCandle(stockCode, "candle:30min:", "/topic/stocks/" + stockCode + "/candle/30min");
                broadcastCandle(stockCode, "candle:60min:", "/topic/stocks/" + stockCode + "/candle/60min");
                broadcastCandle(stockCode, "candle:1day:",  "/topic/stocks/" + stockCode + "/candle/1day");

            } else if ("UH1".equals(trCd)) {
                LsWsOrderBookResponse response = objectMapper.treeToValue(node, LsWsOrderBookResponse.class);
                if (response.body() == null) return;
                String stockCode = response.body().shcode();
                orderBookService.save(response.body());
                StockOrderBookResponse orderBook = orderBookService.getOrderBook(stockCode);
                if (orderBook != null) {
                    messagingTemplate.convertAndSend("/topic/orderbook/" + stockCode, orderBook);
                }
            }
        } catch (Exception e) {
            log.warn("LS WebSocket 메시지 파싱 실패: {}", e.getMessage());
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
