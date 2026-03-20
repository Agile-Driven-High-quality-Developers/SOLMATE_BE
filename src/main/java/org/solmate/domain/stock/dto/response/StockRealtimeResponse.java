package org.solmate.domain.stock.dto.response;

import org.solmate.external.ls.dto.websocket.LsWsStockResponse;

public record StockRealtimeResponse(
        String stockCode,
        long currentPrice,
        long changePrice,
        double changeRate,
        long highPrice,
        long lowPrice,
        long volume,
        String chetime
) {
    public static StockRealtimeResponse from(LsWsStockResponse.Body body) {
        return new StockRealtimeResponse(
                body.shcode(),
                Long.parseLong(body.price().trim()),
                Long.parseLong(body.change().trim()),
                Double.parseDouble(body.drate().trim()),
                Long.parseLong(body.high().trim()),
                Long.parseLong(body.low().trim()),
                Long.parseLong(body.volume().trim()),
                body.chetime()
        );
    }
}
