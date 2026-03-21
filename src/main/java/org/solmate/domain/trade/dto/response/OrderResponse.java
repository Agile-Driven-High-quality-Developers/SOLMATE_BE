package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;

import org.solmate.domain.trade.entity.TradeHistory;

public record OrderResponse(
    Long orderId,
    String ticker,
    String orderType,
    String tradeType,
    BigDecimal price,
    BigDecimal quantity,
    String status
) {
    public static OrderResponse of(TradeHistory tradeHistory) {
        return new OrderResponse(
            tradeHistory.getId(),
            tradeHistory.getStock().getTickerCode(),
            tradeHistory.getOrderType().name(),
            tradeHistory.getTradeType().name(),
            tradeHistory.getPrice(),
            tradeHistory.getQuantity(),
            tradeHistory.getTradeStatus().name()
        );
    }
}
