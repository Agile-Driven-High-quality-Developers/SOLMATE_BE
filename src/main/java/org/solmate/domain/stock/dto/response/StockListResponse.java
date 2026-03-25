package org.solmate.domain.stock.dto.response;

import java.math.BigDecimal;
import java.util.Map;

import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.stock.enums.SectorType;

public record StockListResponse(
        String tickerCode,
        String stockName,
        String stockLogo,
        SectorType sectorType,
        long currentPrice,
        double changeRate,
        BigDecimal total
) {
    public static StockListResponse ofWithClosePrice(Stock stock, long closePrice) {
        return new StockListResponse(
                stock.getTickerCode(),
                stock.getStockName(),
                stock.getStockLogo(),
                stock.getSectorType(),
                closePrice,
                0.0,
                stock.getTotal()
        );
    }

    public static StockListResponse of(Stock stock, Map<Object, Object> redisInfo) {
        long cur = 0;
        double chgRate = 0.0;

        if (redisInfo != null && !redisInfo.isEmpty()) {
            String curStr = (String) redisInfo.get("cur");
            String chgRateStr = (String) redisInfo.get("chgRate");
            if (curStr != null && !curStr.isBlank()) cur = Long.parseLong(curStr);
            if (chgRateStr != null && !chgRateStr.isBlank()) chgRate = Double.parseDouble(chgRateStr);
        }

        return new StockListResponse(
                stock.getTickerCode(),
                stock.getStockName(),
                stock.getStockLogo(),
                stock.getSectorType(),
                cur,
                chgRate,
                stock.getTotal()
        );
    }
}