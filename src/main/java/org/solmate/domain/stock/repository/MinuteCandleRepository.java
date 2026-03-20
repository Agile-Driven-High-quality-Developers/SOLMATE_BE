package org.solmate.domain.stock.repository;

import org.solmate.domain.stock.entity.MinuteCandle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinuteCandleRepository extends JpaRepository<MinuteCandle, Long> {
}
