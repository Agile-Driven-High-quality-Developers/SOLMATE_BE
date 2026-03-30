package org.solmate.domain.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.solmate.domain.stock.entity.DailyCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyCandleRepository extends JpaRepository<DailyCandle, Long> {

    List<DailyCandle> findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(
            String stockCode, LocalDateTime from, LocalDateTime to);

    // 특정 종목의 가장 최근 일봉 조회
    Optional<DailyCandle> findTopByStockCodeOrderByCandleTimeDesc(String stockCode);

    // 여러 종목의 가장 최근 일봉 한 번에 조회
    @Query("SELECT d FROM DailyCandle d WHERE d.stockCode IN :tickerCodes AND d.candleTime = (SELECT MAX(d2.candleTime) FROM DailyCandle d2 WHERE d2.stockCode = d.stockCode)")
    List<DailyCandle> findLatestByStockCodes(@Param("tickerCodes") List<String> tickerCodes);
}
