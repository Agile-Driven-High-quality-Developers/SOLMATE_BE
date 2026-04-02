package org.solmate.domain.trade.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.domain.trade.dto.request.BuyOrderRequest;
import org.solmate.domain.trade.dto.request.SellOrderRequest;
import org.solmate.common.s3.S3Service;
import org.solmate.domain.trade.dto.response.OrderResponse;
import org.solmate.domain.trade.dto.response.TradeHistoryResponse;
import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.trade.entity.TradeDiary;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.OrderType;
import org.solmate.domain.trade.enums.TradeDiaryStatus;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.enums.TradeType;
import org.solmate.domain.trade.repository.HoldingsRepository;
import org.solmate.domain.trade.repository.TradeDiaryRepository;
import org.solmate.domain.trade.repository.TradeHistoryRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final HoldingsRepository holdingsRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final TradeDiaryRepository tradeDiaryRepository;
    private final StringRedisTemplate redisTemplate;
    private final S3Service s3Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse buyOrder(Long userId, BuyOrderRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Account account = accountRepository.findByUserWithLock(user)
            .orElseThrow(() -> new GeneralException(ErrorStatus.ACCOUNT_NOT_FOUND));

        Stock stock = stockRepository.findByTickerCode(request.ticker())
            .orElseThrow(() -> new GeneralException(ErrorStatus.STOCK_NOT_FOUND));

        // Redis 현재가 조회
        BigDecimal currentPrice = getCurrentPrice(request.ticker());

        // 시장가면 현재가, 지정가면 요청 price 사용
        BigDecimal price = OrderType.MARKET.equals(request.orderType())
            ? currentPrice
            : request.price();

        // 상/하한가 클램핑
        price = clampPrice(price, currentPrice);

        // 호가 단위 보정
        price = adjustToTickSize(price);

        // 잔액 검증
        BigDecimal totalCost = price.multiply(request.quantity());
        if (account.getCash().compareTo(totalCost) < 0) {
            throw new GeneralException(ErrorStatus.INSUFFICIENT_CASH);
        }

        // 현금 선차감
        account.subtractCash(totalCost);

        // TradeHistory 저장
        TradeHistory tradeHistory = TradeHistory.builder()
            .user(user)
            .stock(stock)
            .price(price)
            .quantity(request.quantity())
            .tradeType(TradeType.BUY)
            .tradeStatus(TradeStatus.PENDING)
            .orderType(request.orderType())
            .build();
        tradeHistoryRepository.saveAndFlush(tradeHistory);

        // TradeDiary 저장
        TradeDiary tradeDiary = TradeDiary.builder()
            .user(user)
            .tradeHistory(tradeHistory)
            .content(request.diary())
            .status(TradeDiaryStatus.PENDING)
            .build();
        tradeDiaryRepository.save(tradeDiary);

        // Redis ZSet에 주문 추가
        addOrderToRedis("orders:buy:" + request.ticker(), tradeHistory.getId(), userId, price, request.quantity());

        OrderResponse response = OrderResponse.of(tradeHistory);
        messagingTemplate.convertAndSend("/topic/trades/" + userId, TradeHistoryResponse.OrderItem.from(tradeHistory));
        return response;
    }

    @Transactional
    public OrderResponse sellOrder(Long userId, SellOrderRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Stock stock = stockRepository.findByTickerCode(request.ticker())
            .orElseThrow(() -> new GeneralException(ErrorStatus.STOCK_NOT_FOUND));

        Holdings holdings = holdingsRepository.findByUserAndTickerCodeWithLock(user, request.ticker())
            .orElseThrow(() -> new GeneralException(ErrorStatus.INSUFFICIENT_HOLDINGS));

        // 가용 수량 검증 (PENDING SELL 수량 제외)
        BigDecimal pendingSellQuantity = tradeHistoryRepository
                .findPendingByUserIdAndTickerCodeAndTradeType(userId, request.ticker(), TradeType.SELL)
                .stream()
                .map(TradeHistory::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableQuantity = holdings.getQuantity().subtract(pendingSellQuantity);
        if (availableQuantity.compareTo(request.quantity()) < 0) {
            throw new GeneralException(ErrorStatus.INSUFFICIENT_HOLDINGS);
        }

        // Redis 현재가 조회
        BigDecimal currentPrice = getCurrentPrice(request.ticker());

        // 시장가면 현재가, 지정가면 요청 price 사용
        BigDecimal price = OrderType.MARKET.equals(request.orderType())
            ? currentPrice
            : request.price();

        // 상/하한가 클램핑
        price = clampPrice(price, currentPrice);

        // 호가 단위 보정
        price = adjustToTickSize(price);

        // TradeHistory 저장
        // avgPriceSnapshot: 매도 시점의 평균단가 스냅샷 저장 (이후 추가 매수 시 avgPrice가 바뀌어도 수익 계산 가능)
        TradeHistory tradeHistory = TradeHistory.builder()
            .user(user)
            .stock(stock)
            .price(price)
            .quantity(request.quantity())
            .tradeType(TradeType.SELL)
            .tradeStatus(TradeStatus.PENDING)
            .orderType(request.orderType())
            .avgPriceSnapshot(holdings.getAvgPrice())
            .build();
        tradeHistoryRepository.saveAndFlush(tradeHistory);

        // TradeDiary 저장
        TradeDiary tradeDiary = TradeDiary.builder()
            .user(user)
            .tradeHistory(tradeHistory)
            .content(request.diary())
            .status(TradeDiaryStatus.PENDING)
            .build();
        tradeDiaryRepository.save(tradeDiary);

        // Redis ZSet에 주문 추가
        addOrderToRedis("orders:sell:" + request.ticker(), tradeHistory.getId(), userId, price, request.quantity());

        OrderResponse response = OrderResponse.of(tradeHistory);
        messagingTemplate.convertAndSend("/topic/trades/" + userId, TradeHistoryResponse.OrderItem.from(tradeHistory));
        return response;
    }

    @Transactional(readOnly = true)
    public List<TradeHistoryResponse.PortfolioItem> getPortfolioTrades(Long userId) {
        return tradeHistoryRepository.findFilledByUserId(userId).stream()
                .map(trade -> TradeHistoryResponse.PortfolioItem.of(trade, s3Service))
                .toList();
    }

    @Transactional(readOnly = true)
    public TradeHistoryResponse getTradeHistories(Long userId, String tickerCode) {
        List<TradeHistory> trades = tradeHistoryRepository.findByUserIdAndTickerCode(userId, tickerCode);

        String stockName = trades.isEmpty()
            ? stockRepository.findByTickerCode(tickerCode)
                .map(Stock::getStockName)
                .orElse("")
            : trades.get(0).getStock().getStockName();

        return TradeHistoryResponse.of(tickerCode, stockName, trades);
    }

    // Redis에서 현재가 조회
    private BigDecimal getCurrentPrice(String ticker) {
        String curStr = (String) redisTemplate.opsForHash().get("stock:info:" + ticker, "cur");
        if (curStr == null) throw new GeneralException(ErrorStatus.STOCK_PRICE_NOT_FOUND);
        return new BigDecimal(curStr);
    }

    // 상/하한가 클램핑 (±30%)
    private BigDecimal clampPrice(BigDecimal price, BigDecimal currentPrice) {
        BigDecimal upper = currentPrice.multiply(new BigDecimal("1.3"));
        BigDecimal lower = currentPrice.multiply(new BigDecimal("0.7"));
        return price.min(upper).max(lower);
    }

    // 호가 단위 보정 (내림)
    private BigDecimal adjustToTickSize(BigDecimal price) {
        int tickSize = getTickSize(price);
        BigDecimal tick = new BigDecimal(tickSize);
        return price.divide(tick, 0, RoundingMode.FLOOR).multiply(tick);
    }

    // 주가 범위별 호가 단위
    private int getTickSize(BigDecimal price) {
        int p = price.intValue();
        if (p < 2000) return 1;
        if (p < 5000) return 5;
        if (p < 20000) return 10;
        if (p < 50000) return 50;
        if (p < 200000) return 100;
        if (p < 500000) return 500;
        return 1000;
    }

    // Redis ZSet에 주문 추가
    private void addOrderToRedis(String key, Long orderId, Long userId, BigDecimal price, BigDecimal quantity) {
        try {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", orderId);
            orderData.put("userId", userId);
            orderData.put("quantity", quantity);
            orderData.put("timestamp", Instant.now().toEpochMilli());

            String json = objectMapper.writeValueAsString(orderData);
            redisTemplate.opsForZSet().add(key, json, price.doubleValue());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 주문 직렬화 실패", e);
        }
    }
}
