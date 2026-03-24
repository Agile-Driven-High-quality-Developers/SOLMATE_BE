package org.solmate.domain.trade.service;

import java.math.BigDecimal;
import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.s3.S3Service;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.solmate.domain.stock.dto.response.StockHoldingResponse;
import org.solmate.domain.trade.dto.response.HoldingsResponse;
import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeType;
import org.solmate.domain.trade.repository.HoldingsRepository;
import org.solmate.domain.trade.repository.TradeHistoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingsService {

    private final HoldingsRepository holdingsRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public List<HoldingsResponse> getHoldings(Long userId) {
        List<Holdings> holdings = holdingsRepository.findByUserId(userId);

        return holdings.stream()
            .map(h -> HoldingsResponse.of(h, getCurrentPrice(h.getTickerCode()), s3Service))
            .toList();
    }

    // 특정 종목의 보유현황 조회 - PENDING 매도 주문까지 반영해 실제 보유수량을 계산 후 현재가 기준 평가손익 반환
    @Transactional(readOnly = true)
    public StockHoldingResponse getStockHolding(Long userId, String stockCode) {
        // Holdings 조회 (없으면 수량/평균단가 0)
        // Holdings.quantity는 매도 주문 접수 시 선차감되어 있으므로 PENDING SELL 수량을 다시 더해야 함
        Holdings holdings = holdingsRepository.findByUserIdAndTickerCode(userId, stockCode)
                .orElse(null);

        BigDecimal holdingsQuantity = holdings != null ? holdings.getQuantity() : BigDecimal.ZERO;
        BigDecimal avgPrice = holdings != null ? holdings.getAvgPrice() : BigDecimal.ZERO;

        // PENDING SELL 수량 합산
        List<TradeHistory> pendingSells = tradeHistoryRepository
                .findPendingByUserIdAndTickerCodeAndTradeType(userId, stockCode, TradeType.SELL);
        BigDecimal pendingSellQuantity = pendingSells.stream()
                .map(TradeHistory::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 현재가 조회 (없으면 0)
        BigDecimal currentPrice = getCurrentPriceOrZero(stockCode);

        return StockHoldingResponse.of(holdingsQuantity, pendingSellQuantity, avgPrice, currentPrice);
    }

    // 보유 현금 조회 - Account.cash(매수 접수 시 선차감) + PENDING BUY 금액 합산
    @Transactional(readOnly = true)
    public BigDecimal getCash(Long userId) {
        BigDecimal cash = accountRepository.findByUserId(userId)
                .map(Account::getCash)
                .orElse(BigDecimal.ZERO);

        List<TradeHistory> pendingBuys = tradeHistoryRepository
                .findPendingByUserIdAndTradeType(userId, TradeType.BUY);
        BigDecimal pendingBuyAmount = pendingBuys.stream()
                .map(t -> t.getPrice().multiply(t.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return cash.add(pendingBuyAmount);
    }

    private BigDecimal getCurrentPrice(String ticker) {
        String curStr = (String) redisTemplate.opsForHash().get("stock:info:" + ticker, "cur");
        if (curStr == null) throw new GeneralException(ErrorStatus.STOCK_PRICE_NOT_FOUND);
        return new BigDecimal(curStr);
    }

    private BigDecimal getCurrentPriceOrZero(String ticker) {
        String curStr = (String) redisTemplate.opsForHash().get("stock:info:" + ticker, "cur");
        return curStr != null ? new BigDecimal(curStr) : BigDecimal.ZERO;
    }
}
