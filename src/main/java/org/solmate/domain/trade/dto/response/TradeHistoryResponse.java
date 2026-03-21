package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;
import java.util.List;

import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;

public record TradeHistoryResponse(
        String stockCode,
        String stockName,
        List<OrderItem> orders
) {
    public record OrderItem(
            Long orderId,
            String side,
            String sideLabel,
            String orderType,
            String orderTypeLabel,
            BigDecimal orderPrice,
            BigDecimal quantity,
            BigDecimal orderAmount,
            String status,
            String statusLabel,
            boolean cancelable
    ) {
        public static OrderItem from(TradeHistory trade) {
            return new OrderItem(
                    trade.getId(),
                    trade.getTradeType().name(),
                    trade.getTradeType().getLabel(),
                    trade.getOrderType().name(),
                    trade.getOrderType().getLabel(),
                    trade.getPrice(),
                    trade.getQuantity(),
                    trade.getPrice().multiply(trade.getQuantity()),
                    trade.getTradeStatus().name(),
                    trade.getTradeStatus().getLabel(),
                    trade.getTradeStatus() == TradeStatus.PENDING
            );
        }
    }

    public static TradeHistoryResponse of(String stockCode, String stockName, List<TradeHistory> trades) {
        return new TradeHistoryResponse(
                stockCode,
                stockName,
                trades.stream().map(OrderItem::from).toList()
        );
    }
}
