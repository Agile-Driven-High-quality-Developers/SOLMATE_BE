package org.solmate.domain.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.portfolio.PortfolioCalculator;
import org.solmate.common.portfolio.PortfolioHoldingLine;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.account.dto.response.AccountSummaryResponse;
import org.solmate.domain.account.dto.response.HoldingRatioItem;
import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final PortfolioCalculator portfolioCalculator;

    public AccountSummaryResponse getSummary(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ACCOUNT_NOT_FOUND));

        BigDecimal initialCash = account.getInitialCash();
        BigDecimal cash = portfolioCalculator.getCash(userId);

        List<PortfolioHoldingLine> lines = portfolioCalculator.getHoldingEvaluationLines(userId);
        BigDecimal sumEvaluationRaw = lines.stream()
                .map(PortfolioHoldingLine::evaluation)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEvaluation = sumEvaluationRaw.setScale(0, RoundingMode.HALF_UP);

        List<HoldingRatioItem> holdingsRatio = buildHoldingsRatio(lines, sumEvaluationRaw);

        BigDecimal totalAsset = cash.add(totalEvaluation);
        BigDecimal totalReturnAmount = portfolioCalculator.getTotalReturnAmount(totalAsset, initialCash);
        BigDecimal totalReturnRate = portfolioCalculator.getTotalReturnRate(totalReturnAmount, initialCash);
        int holdingsCount = lines.size();

        return new AccountSummaryResponse(
                totalAsset,
                totalReturnAmount,
                totalReturnRate,
                cash,
                initialCash,
                holdingsCount,
                totalEvaluation,
                totalReturnRate,
                totalReturnAmount,
                holdingsRatio
        );
    }

    private List<HoldingRatioItem> buildHoldingsRatio(List<PortfolioHoldingLine> lines, BigDecimal sumRaw) {
        if (sumRaw.compareTo(BigDecimal.ZERO) == 0) {
            return lines.stream()
                    .map(l -> new HoldingRatioItem(
                            l.tickerCode(),
                            l.stockName(),
                            l.evaluation().setScale(0, RoundingMode.HALF_UP),
                            BigDecimal.ZERO))
                    .toList();
        }

        return lines.stream()
                .map(l -> new HoldingRatioItem(
                        l.tickerCode(),
                        l.stockName(),
                        l.evaluation().setScale(0, RoundingMode.HALF_UP),
                        l.evaluation()
                                .divide(sumRaw, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }
}
