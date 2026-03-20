package org.solmate.domain.trade.entity;

import java.math.BigDecimal;

import org.solmate.common.base.BaseEntity;
import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "holdings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holdings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "ticker_code", nullable = false, length = 10)
    private String tickerCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "avg_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal avgPrice;

    @Column(name = "return_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal returnRate;

    @Builder
    public Holdings(User user, Stock stock, String tickerCode, BigDecimal quantity,
                    BigDecimal avgPrice, BigDecimal returnRate) {
        this.user = user;
        this.stock = stock;
        this.tickerCode = tickerCode;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.returnRate = returnRate;
    }

    public void updateQuantityAndAvgPrice(BigDecimal quantity, BigDecimal avgPrice) {
        this.quantity = quantity;
        this.avgPrice = avgPrice;
    }

    public void updateReturnRate(BigDecimal returnRate) {
        this.returnRate = returnRate;
    }

    public void subtractQuantity(BigDecimal quantity) {
        this.quantity = this.quantity.subtract(quantity);
    }
}
