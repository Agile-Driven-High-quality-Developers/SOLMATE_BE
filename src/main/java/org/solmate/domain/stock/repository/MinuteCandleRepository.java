package org.solmate.domain.stock.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.solmate.domain.stock.entity.MinuteCandle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinuteCandleRepository extends JpaRepository<MinuteCandle, Long> {

    // 특정 종목의 시간 범위 내 1분봉 조회 (오름차순)
    List<MinuteCandle> findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(
            String stockCode, LocalDateTime from, LocalDateTime to);
}
