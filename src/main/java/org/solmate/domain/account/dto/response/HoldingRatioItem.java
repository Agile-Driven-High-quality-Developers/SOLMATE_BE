package org.solmate.domain.account.dto.response;

import java.math.BigDecimal;

public record HoldingRatioItem(
        String tickerCode,
        String stockName,
        BigDecimal evaluation,
        BigDecimal ratio
) {}
