package org.solmate.domain.account.service;

import java.math.BigDecimal;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.portfolio.PortfolioCalculator;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.account.dto.response.AccountSummaryResponse;
import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.solmate.domain.trade.repository.HoldingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final HoldingsRepository holdingsRepository;
    private final PortfolioCalculator portfolioCalculator;

    public AccountSummaryResponse getSummary(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ACCOUNT_NOT_FOUND));

        BigDecimal initialCash = account.getInitialCash();
        BigDecimal cash = portfolioCalculator.getCash(userId);
        BigDecimal totalEvaluation = portfolioCalculator.getTotalEvaluation(userId);
        BigDecimal totalAsset = cash.add(totalEvaluation);
        BigDecimal totalReturnAmount = portfolioCalculator.getTotalReturnAmount(totalAsset, initialCash);
        BigDecimal totalReturnRate = portfolioCalculator.getTotalReturnRate(totalReturnAmount, initialCash);
        int holdingsCount = holdingsRepository.findByUserId(userId).size();

        return new AccountSummaryResponse(
                totalAsset,
                totalReturnAmount,
                totalReturnRate,
                cash,
                initialCash,
                holdingsCount,
                totalEvaluation,
                totalReturnRate,
                totalReturnAmount
        );
    }
}
