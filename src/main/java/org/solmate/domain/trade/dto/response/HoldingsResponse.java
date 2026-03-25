package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.solmate.common.s3.S3Service;
import org.solmate.domain.trade.entity.Holdings;

public record HoldingsResponse(
        String tickerCode,
        String stockName,
        String stockLogo,
        BigDecimal quantity,
        BigDecimal avgPrice,
        BigDecimal currentPrice,
        BigDecimal evaluation,
        BigDecimal returnRate,
        BigDecimal returnAmount
) {
    public static HoldingsResponse of(Holdings holdings, BigDecimal currentPrice, S3Service s3Service) {
        BigDecimal quantity = holdings.getQuantity();
        BigDecimal avgPrice = holdings.getAvgPrice();

        BigDecimal evaluation = currentPrice.multiply(quantity).setScale(0, RoundingMode.HALF_UP);
        BigDecimal returnAmount = currentPrice.subtract(avgPrice).multiply(quantity).setScale(0, RoundingMode.HALF_UP);
        BigDecimal returnRate = avgPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : currentPrice.subtract(avgPrice)
                        .divide(avgPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        String logoKey = holdings.getStock().getStockLogo();
        String stockLogo = (logoKey != null) ? s3Service.buildFileUrl(logoKey) : null;

        return new HoldingsResponse(
                holdings.getTickerCode(),
                holdings.getStock().getStockName(),
                stockLogo,
                quantity,
                avgPrice,
                currentPrice,
                evaluation,
                returnRate,
                returnAmount
        );
    }

    // 프로필용 (내/타인/멘토/멘티) - avgPrice null
    public static HoldingsResponse ofProfile(Holdings holdings, BigDecimal currentPrice, S3Service s3Service) {
        BigDecimal quantity = holdings.getQuantity();
        BigDecimal avgPrice = holdings.getAvgPrice();

        BigDecimal evaluation = currentPrice.multiply(quantity).setScale(0, RoundingMode.HALF_UP);
        BigDecimal returnAmount = currentPrice.subtract(avgPrice).multiply(quantity).setScale(0, RoundingMode.HALF_UP);
        BigDecimal returnRate = avgPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : currentPrice.subtract(avgPrice)
                        .divide(avgPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        String logoKey = holdings.getStock().getStockLogo();
        String stockLogo = (logoKey != null) ? s3Service.buildFileUrl(logoKey) : null;

        return new HoldingsResponse(
                holdings.getTickerCode(),
                holdings.getStock().getStockName(),
                stockLogo,
                quantity,
                null,
                currentPrice,
                evaluation,
                returnRate,
                returnAmount
        );
    }
}
