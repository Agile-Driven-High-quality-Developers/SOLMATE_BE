package org.solmate.domain.account.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AccountSummaryResponse(
        BigDecimal totalAsset,
        BigDecimal totalAssetChangeAmount,
        BigDecimal totalAssetChangeRate,

        BigDecimal cash,
        BigDecimal initialCash,

        int holdingsCount,
        BigDecimal totalEvaluation,

        BigDecimal totalReturnRate,
        BigDecimal totalReturnAmount,

        List<HoldingRatioItem> holdingsRatio
) {}
