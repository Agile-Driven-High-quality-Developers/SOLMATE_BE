package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.solmate.common.s3.S3Service;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.enums.TradeType;

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

    public record PortfolioItem(
            String stockName,
            String stockLogo,
            String tickerCode,
            String tradeType,
            String tradeTypeLabel,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount,
            BigDecimal profitAmount,
            BigDecimal profitRate,
            LocalDateTime tradedAt
    ) {
        public static PortfolioItem of(TradeHistory trade, S3Service s3Service) {
            String logoKey = trade.getStock().getStockLogo();
            String stockLogo = (logoKey != null) ? s3Service.buildFileUrl(logoKey) : null;

            BigDecimal profitAmount = null;
            BigDecimal profitRate = null;

            if (trade.getTradeType() == TradeType.SELL && trade.getAvgPriceSnapshot() != null
                    && trade.getAvgPriceSnapshot().compareTo(BigDecimal.ZERO) != 0) {
                profitAmount = trade.getPrice()
                        .subtract(trade.getAvgPriceSnapshot())
                        .multiply(trade.getQuantity())
                        .setScale(0, RoundingMode.HALF_UP);
                profitRate = trade.getPrice()
                        .subtract(trade.getAvgPriceSnapshot())
                        .divide(trade.getAvgPriceSnapshot(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            return new PortfolioItem(
                    trade.getStock().getStockName(),
                    stockLogo,
                    trade.getStock().getTickerCode(),
                    trade.getTradeType().name(),
                    trade.getTradeType().getLabel(),
                    trade.getQuantity(),
                    trade.getPrice(),
                    trade.getPrice().multiply(trade.getQuantity()).setScale(0, RoundingMode.HALF_UP),
                    profitAmount,
                    profitRate,
                    trade.getCreatedAt()
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
