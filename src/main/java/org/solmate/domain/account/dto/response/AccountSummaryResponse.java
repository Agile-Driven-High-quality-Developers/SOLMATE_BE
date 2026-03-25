package org.solmate.domain.account.dto.response;

import java.math.BigDecimal;

public record AccountSummaryResponse(
        BigDecimal totalAsset,
        BigDecimal totalAssetChangeAmount,
        BigDecimal totalAssetChangeRate,

        BigDecimal cash,
        BigDecimal initialCash,

        int holdingsCount,
        BigDecimal totalEvaluation,

        BigDecimal totalReturnRate,
        BigDecimal totalReturnAmount
) {}
