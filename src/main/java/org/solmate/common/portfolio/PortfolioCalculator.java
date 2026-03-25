package org.solmate.common.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeType;
import org.solmate.domain.trade.repository.HoldingsRepository;
import org.solmate.domain.trade.repository.TradeHistoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioCalculator {

    private final AccountRepository accountRepository;
    private final HoldingsRepository holdingsRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final StringRedisTemplate redisTemplate;

    // 실제 보유 현금 = Account.cash(매수 선차감 반영) + PENDING BUY 금액
    @Transactional(readOnly = true)
    public BigDecimal getCash(Long userId) {
        BigDecimal cash = accountRepository.findByUserId(userId)
                .map(Account::getCash)
                .orElse(BigDecimal.ZERO);

        BigDecimal pendingBuyAmount = tradeHistoryRepository
                .findPendingByUserIdAndTradeType(userId, TradeType.BUY)
                .stream()
                .map(t -> t.getPrice().multiply(t.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return cash.add(pendingBuyAmount).setScale(0, RoundingMode.HALF_UP);
    }

    // 총 평가금액 = (Holdings.quantity + PENDING SELL 수량) × 현재가의 합
    // Holdings.quantity는 매도 주문 접수 시 선차감되므로 PENDING SELL 수량을 복원해야 실제 보유 수량
    @Transactional(readOnly = true)
    public BigDecimal getTotalEvaluation(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);

        return holdingsList.stream()
                .map(h -> {
                    BigDecimal pendingSellQuantity = tradeHistoryRepository
                            .findPendingByUserIdAndTickerCodeAndTradeType(userId, h.getTickerCode(), TradeType.SELL)
                            .stream()
                            .map(TradeHistory::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalQuantity = h.getQuantity().add(pendingSellQuantity);
                    return getCurrentPrice(h.getTickerCode()).multiply(totalQuantity);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    // 총 수익금 = 총 자산(평가금액 + 현금) - 시드머니
    public BigDecimal getTotalReturnAmount(BigDecimal totalAsset, BigDecimal initialCash) {
        return totalAsset.subtract(initialCash).setScale(0, RoundingMode.HALF_UP);
    }

    // 총 수익률 = 총 수익금 / 시드머니 × 100
    public BigDecimal getTotalReturnRate(BigDecimal totalReturnAmount, BigDecimal initialCash) {
        if (initialCash.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return totalReturnAmount
                .divide(initialCash, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCurrentPrice(String ticker) {
        String curStr = (String) redisTemplate.opsForHash().get("stock:info:" + ticker, "cur");
        if (curStr == null) throw new GeneralException(ErrorStatus.STOCK_PRICE_NOT_FOUND);
        return new BigDecimal(curStr);
    }
}
