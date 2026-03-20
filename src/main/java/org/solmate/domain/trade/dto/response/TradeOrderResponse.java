package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.OrderType;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.enums.TradeType;

public record TradeOrderResponse(
        Long orderId,
        String stockCode,
        String stockName,
        BigDecimal price,
        BigDecimal quantity,
        TradeType tradeType,
        TradeStatus tradeStatus,
        OrderType orderType,
        LocalDateTime createdAt
) {
    public static TradeOrderResponse from(TradeHistory tradeHistory) {
        return new TradeOrderResponse(
                tradeHistory.getId(),
                tradeHistory.getStock().getTickerCode(),
                tradeHistory.getStock().getStockName(),
                tradeHistory.getPrice(),
                tradeHistory.getQuantity(),
                tradeHistory.getTradeType(),
                tradeHistory.getTradeStatus(),
                tradeHistory.getOrderType(),
                tradeHistory.getCreatedAt()
        );
    }
}
