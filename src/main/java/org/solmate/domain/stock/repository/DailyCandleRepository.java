package org.solmate.domain.stock.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.solmate.domain.stock.entity.DailyCandle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCandleRepository extends JpaRepository<DailyCandle, Long> {

    List<DailyCandle> findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(
            String stockCode, LocalDateTime from, LocalDateTime to);
}
