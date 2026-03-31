package org.solmate.domain.stock.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.solmate.domain.stock.entity.MinuteCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MinuteCandleRepository extends JpaRepository<MinuteCandle, Long> {

    // 특정 종목의 시간 범위 내 1분봉 조회 (오름차순)
    List<MinuteCandle> findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(
            String stockCode, LocalDateTime from, LocalDateTime to);

    @Modifying
    @Transactional
    @Query(value =
        "INSERT INTO minute_candle " +
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
