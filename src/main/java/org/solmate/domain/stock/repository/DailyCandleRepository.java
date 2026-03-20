package org.solmate.domain.stock.repository;

import org.solmate.domain.stock.entity.DailyCandle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCandleRepository extends JpaRepository<DailyCandle, Long> {
}
