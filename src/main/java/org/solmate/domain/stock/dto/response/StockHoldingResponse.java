package org.solmate.domain.stock.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record StockHoldingResponse(
        BigDecimal holdingQuantity,
        BigDecimal availableSellQuantity,
        BigDecimal averageBuyPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitAmount,
        BigDecimal profitRate
) {
    public static StockHoldingResponse of(
            BigDecimal holdingsQuantity,
            BigDecimal pendingSellQuantity,
            BigDecimal avgPrice,
            BigDecimal currentPrice) {

        // 보유수량 = Holdings 수량(매도 접수 시 차감됨) + PENDING SELL 수량
        BigDecimal holdingQuantity = holdingsQuantity.add(pendingSellQuantity);

        // 즉시 매도 가능 수량 = Holdings 수량 (PENDING SELL 제외)
        BigDecimal availableSellQuantity = holdingsQuantity;

        if (holdingQuantity.compareTo(BigDecimal.ZERO) == 0 || avgPrice.compareTo(BigDecimal.ZERO) == 0) {
            return new StockHoldingResponse(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        BigDecimal evaluationAmount = currentPrice.multiply(holdingQuantity).setScale(0, RoundingMode.HALF_UP);
        BigDecimal profitAmount = currentPrice.subtract(avgPrice).multiply(holdingQuantity).setScale(0, RoundingMode.HALF_UP);
        BigDecimal profitRate = currentPrice.subtract(avgPrice)
                .divide(avgPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        return new StockHoldingResponse(
                holdingQuantity,
                availableSellQuantity,
                avgPrice,
                evaluationAmount,
                profitAmount,
                profitRate
        );
    }
}
