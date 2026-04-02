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
        BigDecimal total,
        long volume
) {
    public static StockListResponse ofWithClosePrice(Stock stock, long closePrice, String logoUrl) {
        return new StockListResponse(
                stock.getTickerCode(),
                stock.getStockName(),
                logoUrl,
                stock.getSectorType(),
                closePrice,
                0.0,
                stock.getTotal(),
                0L
        );
    }

    public static StockListResponse of(Stock stock, Map<Object, Object> redisInfo, String logoUrl) {
        long cur = 0;
        double chgRate = 0.0;
        long vol = 0;
        BigDecimal marketCap = stock.getTotal();

        if (redisInfo != null && !redisInfo.isEmpty()) {
            String curStr = (String) redisInfo.get("cur");
            String chgRateStr = (String) redisInfo.get("chgRate");
            String volStr = (String) redisInfo.get("vol");
            String totalStr = (String) redisInfo.get("total");

            if (curStr != null && !curStr.isBlank()) cur = Long.parseLong(curStr);
            if (chgRateStr != null && !chgRateStr.isBlank()) chgRate = Double.parseDouble(chgRateStr);
            if (volStr != null && !volStr.isBlank()) vol = Long.parseLong(volStr);
            if (totalStr != null && !totalStr.isBlank()) marketCap = new BigDecimal(totalStr);
        }

        return new StockListResponse(
                stock.getTickerCode(),
                stock.getStockName(),
                logoUrl,
                stock.getSectorType(),
                cur,
                chgRate,
                marketCap,
                vol
        );
    }
}