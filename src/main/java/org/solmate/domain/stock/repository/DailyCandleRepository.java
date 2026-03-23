package org.solmate.domain.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.solmate.domain.stock.entity.DailyCandle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCandleRepository extends JpaRepository<DailyCandle, Long> {

    List<DailyCandle> findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(
            String stockCode, LocalDateTime from, LocalDateTime to);

    // 특정 종목의 가장 최근 일봉 조회
    Optional<DailyCandle> findTopByStockCodeOrderByCandleTimeDesc(String stockCode);
}
