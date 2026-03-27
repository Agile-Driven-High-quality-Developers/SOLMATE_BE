package org.solmate.common.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
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

    // 주문 가능 금액 = Account.cash (매수 선차감 반영된 실제 잔액)
    @Transactional(readOnly = true)
    public BigDecimal getCash(Long userId) {
        BigDecimal cash = accountRepository.findByUserId(userId)
                .map(Account::getCash)
                .orElse(BigDecimal.ZERO);

        return cash.setScale(0, RoundingMode.HALF_UP);
    }

    // 종목별 평가금액 (실제 보유 수량 = Holdings.quantity + PENDING SELL)
    @Transactional(readOnly = true)
    public List<PortfolioHoldingLine> getHoldingEvaluationLines(Long userId) {
        List<Holdings> holdingsList = holdingsRepository.findByUserId(userId);
        List<PortfolioHoldingLine> lines = new ArrayList<>();

        for (Holdings h : holdingsList) {
            BigDecimal pendingSellQuantity = tradeHistoryRepository
                    .findPendingByUserIdAndTickerCodeAndTradeType(userId, h.getTickerCode(), TradeType.SELL)
                    .stream()
                    .map(TradeHistory::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalQuantity = h.getQuantity().add(pendingSellQuantity);
            if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal evaluation = getCurrentPrice(h.getTickerCode()).multiply(totalQuantity);
            lines.add(new PortfolioHoldingLine(
                    h.getTickerCode(),
                    h.getStock().getStockName(),
                    evaluation));
        }

        lines.sort(Comparator.comparing(PortfolioHoldingLine::evaluation).reversed());
        return lines;
    }

    // 총 평가금액 = (Holdings.quantity + PENDING SELL 수량) × 현재가의 합
    @Transactional(readOnly = true)
    public BigDecimal getTotalEvaluation(Long userId) {
        return getHoldingEvaluationLines(userId).stream()
                .map(PortfolioHoldingLine::evaluation)
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
