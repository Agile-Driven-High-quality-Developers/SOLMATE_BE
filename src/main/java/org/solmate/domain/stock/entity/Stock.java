package org.solmate.domain.stock.entity;

import org.solmate.domain.stock.enums.MarketType;
import org.solmate.domain.stock.enums.StockStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker_code", nullable = false, length = 20)
    private String tickerCode;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false)
    private MarketType marketType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus status;

    @Builder
    public Stock(String tickerCode, String symbol, String description, MarketType marketType, StockStatus status) {
        this.tickerCode = tickerCode;
        this.symbol = symbol;
        this.description = description;
        this.marketType = marketType;
        this.status = status;
    }
}
