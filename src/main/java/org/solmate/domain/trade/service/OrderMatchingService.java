package org.solmate.domain.trade.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.solmate.domain.notification.entity.Notification;
import org.solmate.domain.notification.enums.NotificationCategory;
import org.solmate.domain.notification.enums.NotificationType;
import org.solmate.domain.notification.repository.NotificationRepository;
import org.solmate.domain.notification.service.NotificationService;
import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.enums.TradeType;
import org.solmate.domain.trade.entity.TradeDiary;
import org.solmate.domain.trade.enums.TradeDiaryStatus;
import org.solmate.domain.trade.repository.HoldingsRepository;
import org.solmate.domain.trade.repository.TradeDiaryRepository;
import org.solmate.domain.trade.repository.TradeHistoryRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMatchingService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final TradeDiaryRepository tradeDiaryRepository;
    private final HoldingsRepository holdingsRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ORDER_LOCK_PREFIX = "lock:order:";
    private static final long LOCK_EXPIRE_SECONDS = 5;

    @Transactional
    public void match(String ticker) {
        String curStr = (String) redisTemplate.opsForHash().get("stock:info:" + ticker, "cur");
        if (curStr == null) return;

        BigDecimal currentPrice = new BigDecimal(curStr);

        matchBuyOrders(ticker, currentPrice);
        matchSellOrders(ticker, currentPrice);
    }

    // 매수 체결: score(주문가) >= currentPrice 인 주문 체결
    private void matchBuyOrders(String ticker, BigDecimal currentPrice) {
        String key = "orders:buy:" + ticker;

        Set<String> orders = redisTemplate.opsForZSet()
            .rangeByScore(key, currentPrice.doubleValue(), Double.MAX_VALUE);

        if (orders == null || orders.isEmpty()) return;

        for (String json : orders) {
            try {
                JsonNode node = objectMapper.readTree(json);
                Long orderId = node.get("orderId").asLong();
                Long userId = node.get("userId").asLong();
                BigDecimal quantity = new BigDecimal(node.get("quantity").asText());
                double orderPrice = redisTemplate.opsForZSet().score(key, json);

                // Redis 락 획득 시도 (실패하면 다른 스레드가 처리 중 → 스킵)
                String lockKey = ORDER_LOCK_PREFIX + orderId;
                Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(locked)) continue;

                try {
                    TradeHistory tradeHistory = tradeHistoryRepository.findById(orderId).orElse(null);
                    if (tradeHistory == null || tradeHistory.getTradeStatus() != TradeStatus.PENDING) continue;

                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null) continue;

                    // 차액 환불: (주문가 - 현재가) * 수량
                    BigDecimal refund = BigDecimal.valueOf(orderPrice)
                        .subtract(currentPrice)
                        .multiply(quantity);

                    Account account = accountRepository.findByUser(user).orElse(null);
                    if (account != null && refund.compareTo(BigDecimal.ZERO) > 0) {
                        account.addCash(refund);
                    }

                    // Holdings 업데이트
                    updateHoldingsOnBuy(user, tradeHistory, currentPrice, quantity);

                    // TradeHistory 체결 처리
                    tradeHistory.updateStatus(TradeStatus.FILLED);

                    // TradeDiary 상태 업데이트
                    tradeDiaryRepository.findByTradeHistoryId(orderId)
                        .ifPresent(diary -> diary.updateStatus(TradeDiaryStatus.FILLED));

                    // Redis ZSet에서 제거
                    redisTemplate.opsForZSet().remove(key, json);

                    // 알림 저장
                    saveNotification(user, tradeHistory, currentPrice, quantity);

                    // 팔로워 알림 저장
                    notificationService.saveTradeNotifications(user, tradeHistory.getId(), ticker, tradeHistory.getStock().getStockName(), "BUY", currentPrice, quantity);

                    log.info("매수 체결 완료 - orderId: {}, ticker: {}, price: {}, quantity: {}", orderId, ticker, currentPrice, quantity);

                } finally {
                    // 락 해제
                    redisTemplate.delete(lockKey);
                }

            } catch (Exception e) {
                log.error("매수 체결 처리 실패 - json: {}", json, e);
            }
        }
    }

    // 매도 체결: score(주문가) <= currentPrice 인 주문 체결
    private void matchSellOrders(String ticker, BigDecimal currentPrice) {
        String key = "orders:sell:" + ticker;

        Set<String> orders = redisTemplate.opsForZSet()
            .rangeByScore(key, 0, currentPrice.doubleValue());

        if (orders == null || orders.isEmpty()) return;

        for (String json : orders) {
            try {
                JsonNode node = objectMapper.readTree(json);
                Long orderId = node.get("orderId").asLong();
                Long userId = node.get("userId").asLong();
                BigDecimal quantity = new BigDecimal(node.get("quantity").asText());

                // Redis 락 획득 시도 (실패하면 다른 스레드가 처리 중 → 스킵)
                String lockKey = ORDER_LOCK_PREFIX + orderId;
                Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(locked)) continue;

                try {
                    TradeHistory tradeHistory = tradeHistoryRepository.findById(orderId).orElse(null);
                    if (tradeHistory == null || tradeHistory.getTradeStatus() != TradeStatus.PENDING) continue;

                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null) continue;

                    // 현재가 * 수량 → Account에 입금
                    Account account = accountRepository.findByUser(user).orElse(null);
                    if (account != null) {
                        account.addCash(currentPrice.multiply(quantity));
                    }

                    // TradeHistory 체결 처리
                    tradeHistory.updateStatus(TradeStatus.FILLED);

                    // TradeDiary 상태 업데이트
                    tradeDiaryRepository.findByTradeHistoryId(orderId)
                        .ifPresent(diary -> diary.updateStatus(TradeDiaryStatus.FILLED));

                    // Redis ZSet에서 제거
                    redisTemplate.opsForZSet().remove(key, json);

                    // 알림 저장
                    saveNotification(user, tradeHistory, currentPrice, quantity);

                    // 팔로워 알림 저장
                    notificationService.saveTradeNotifications(user, tradeHistory.getId(), ticker, tradeHistory.getStock().getStockName(), "SELL", currentPrice, quantity);

                    log.info("매도 체결 완료 - orderId: {}, ticker: {}, price: {}, quantity: {}", orderId, ticker, currentPrice, quantity);

                } finally {
                    // 락 해제
                    redisTemplate.delete(lockKey);
                }

            } catch (Exception e) {
                log.error("매도 체결 처리 실패 - json: {}", json, e);
            }
        }
    }

    // 체결 알림 저장
    private void saveNotification(User user, TradeHistory tradeHistory, BigDecimal currentPrice, BigDecimal quantity) {
        String stockName = tradeHistory.getStock().getStockName();
        String ticker = tradeHistory.getStock().getTickerCode();
        String side = tradeHistory.getTradeType() == TradeType.BUY ? "BUY" : "SELL";
        String tradeTypeStr = tradeHistory.getTradeType() == TradeType.BUY ? "매수" : "매도";
        String content = String.format("[%s] %s %s주가 %s원에 체결되었습니다.",
            stockName, tradeTypeStr, quantity.toPlainString(), currentPrice.toPlainString());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                "orderId", tradeHistory.getId(),
                "stockCode", ticker,
                "stockName", stockName,
                "side", side,
                "filledPrice", currentPrice,
                "filledQuantity", quantity
            ));
        } catch (Exception e) {
            payload = null;
        }

        Notification notification = Notification.builder()
            .user(user)
            .notificationType(NotificationType.TRADE)
            .category(NotificationCategory.TRADING)
            .content(content)
            .payload(payload)
            .build();

        notificationRepository.save(notification);
    }

    // 매수 체결 시 Holdings 업데이트 (없으면 생성, 있으면 평균단가 재계산)
    private void updateHoldingsOnBuy(User user, TradeHistory tradeHistory, BigDecimal currentPrice, BigDecimal quantity) {
        String ticker = tradeHistory.getStock().getTickerCode();

        Holdings holdings = holdingsRepository.findByUserAndTickerCode(user, ticker).orElse(null);

        if (holdings == null) {
            Holdings newHoldings = Holdings.builder()
                .user(user)
                .stock(tradeHistory.getStock())
                .tickerCode(ticker)
                .quantity(quantity)
                .avgPrice(currentPrice)
                .returnRate(BigDecimal.ZERO)
                .build();
            holdingsRepository.save(newHoldings);
        } else {
            BigDecimal existQty = holdings.getQuantity();
            BigDecimal existAvg = holdings.getAvgPrice();

            BigDecimal newQty = existQty.add(quantity);
            BigDecimal newAvg = existQty.multiply(existAvg)
                .add(quantity.multiply(currentPrice))
                .divide(newQty, 4, RoundingMode.HALF_UP);

            holdings.updateQuantityAndAvgPrice(newQty, newAvg);
        }
    }
}
