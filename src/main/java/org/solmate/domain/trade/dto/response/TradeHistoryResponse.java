package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.OrderType;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.enums.TradeType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeHistoryResponse {

    private Long orderId;

    private TradeType tradeType;
    private OrderType orderType;

    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal totalAmount;

    private TradeStatus tradeStatus;
    private LocalDateTime createdAt;

    private boolean cancelable;

    public static TradeHistoryResponse from(TradeHistory entity) {
        return TradeHistoryResponse.builder()
                .orderId(entity.getId())
                .tradeType(entity.getTradeType())
                .orderType(entity.getOrderType())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .totalAmount(entity.getPrice().multiply(entity.getQuantity()))
                .tradeStatus(entity.getTradeStatus())
                .createdAt(entity.getCreatedAt())
                .cancelable(entity.getTradeStatus() == TradeStatus.PENDING)
                .build();
    }
}