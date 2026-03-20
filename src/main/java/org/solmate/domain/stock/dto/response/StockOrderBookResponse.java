package org.solmate.domain.stock.dto.response;

import java.util.List;


public record StockOrderBookResponse(
        String stockCode,
        long currentPrice,
        double changeRate,
        List<PriceLevel> sellLevels,
        List<PriceLevel> buyLevels,
        String timestamp
) {
    public record PriceLevel(long price, long quantity) {}
}
