package org.solmate.domain.stock.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.solmate.domain.stock.entity.ThirtyMinuteCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ThirtyMinuteCandleRepository extends JpaRepository<ThirtyMinuteCandle, Long> {

    List<ThirtyMinuteCandle> findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(
            String stockCode, LocalDateTime from, LocalDateTime to);

    @Modifying
    @Transactional
    @Query(value =
        "INSERT INTO thirty_minute_candle " +
        "(stock_code, open_price, high_price, low_price, close_price, volume, candle_time) " +
        "VALUES (:stockCode, :openPrice, :highPrice, :lowPrice, :closePrice, :volume, :candleTime) " +
        "ON CONFLICT DO NOTHING",
        nativeQuery = true)
    int insertIgnoreDuplicate(
        @Param("stockCode")  String stockCode,
        @Param("openPrice")  Long openPrice,
        @Param("highPrice")  Long highPrice,
        @Param("lowPrice")   Long lowPrice,
        @Param("closePrice") Long closePrice,
        @Param("volume")     Long volume,
        @Param("candleTime") LocalDateTime candleTime
    );
}
