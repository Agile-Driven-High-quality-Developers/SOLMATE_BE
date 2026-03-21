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
            String side = trade.getTradeType().name();
            String sideLabel = side.equals("BUY") ? "매수" : "매도";

            String orderType = trade.getOrderType().name();
            String orderTypeLabel = orderType.equals("LIMIT") ? "지정가" : "시장가";

            String status;
            String statusLabel;
            switch (trade.getTradeStatus()) {
                case EXECUTED -> { status = "FILLED";    statusLabel = "체결"; }
                case CANCELLED -> { status = "CANCELLED"; statusLabel = "취소"; }
                default ->        { status = "PENDING";   statusLabel = "대기"; }
            }

            boolean cancelable = trade.getTradeStatus() == TradeStatus.PENDING;
            BigDecimal orderAmount = trade.getPrice().multiply(trade.getQuantity());

            return new OrderItem(
                    trade.getId(),
                    side, sideLabel,
                    orderType, orderTypeLabel,
                    trade.getPrice(),
                    trade.getQuantity(),
                    orderAmount,
                    status, statusLabel,
                    cancelable
            );
        }
    }

    public static TradeHistoryResponse of(String stockCode, String stockName, List<TradeHistory> trades) {
        List<OrderItem> orders = trades.stream()
                .map(OrderItem::from)
                .toList();
        return new TradeHistoryResponse(stockCode, stockName, orders);
    }
}
