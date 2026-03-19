package org.solmate.domain.stock.entity;

import org.solmate.domain.stock.enums.SectorType;
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

    @Column(nullable = false, length = 20)
    private String stockName;

    @Column(nullable = false, length = 1024)
    private String stockLogo;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SectorType sectorType;



    @Builder
    public Stock(String tickerCode, String stockName, String stockLogo, SectorType sectorType) {
        this.tickerCode = tickerCode;
        this.stockName = stockName;
        this.stockLogo = stockLogo;
        this.sectorType = sectorType;
    }
}
