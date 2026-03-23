package org.solmate.domain.trade.entity;

import java.math.BigDecimal;

import org.solmate.common.base.BaseEntity;
import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.trade.enums.OrderType;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.enums.TradeType;
import org.solmate.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private TradeType tradeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_status", nullable = false)
    private TradeStatus tradeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    /**
     * 매도 주문 접수 시점의 평균단가 스냅샷 (매도일 때만 저장)
     * - 매도 후 추가 매수하면 Holdings.avgPrice가 바뀌기 때문에
     *   매도 시점의 평균단가를 여기에 저장해두고 수익 계산에 사용
     * - 수익 = (price - avgPriceSnapshot) * quantity
     * - 매수 주문의 경우 null
     */
    @Column(name = "avg_price_snapshot", precision = 19, scale = 4)
    private BigDecimal avgPriceSnapshot;

    @Builder
    public TradeHistory(User user, Stock stock, BigDecimal price, BigDecimal quantity,
                        TradeType tradeType, TradeStatus tradeStatus, OrderType orderType,
                        BigDecimal avgPriceSnapshot) {
        this.user = user;
        this.stock = stock;
        this.price = price;
        this.quantity = quantity;
        this.tradeType = tradeType;
        this.tradeStatus = tradeStatus;
        this.orderType = orderType;
        this.avgPriceSnapshot = avgPriceSnapshot;
    }

    public void updateStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }
}
