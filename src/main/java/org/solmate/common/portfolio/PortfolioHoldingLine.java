package org.solmate.common.portfolio;

import java.math.BigDecimal;

public record PortfolioHoldingLine(
        String tickerCode,
        String stockName,
        BigDecimal evaluation
) {}
