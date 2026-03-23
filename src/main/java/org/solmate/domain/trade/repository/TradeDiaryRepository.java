package org.solmate.domain.trade.repository;

import java.util.Optional;

import org.solmate.domain.trade.entity.TradeDiary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeDiaryRepository extends JpaRepository<TradeDiary, Long> {

    void deleteByTradeHistoryId(Long tradeHistoryId);

    Optional<TradeDiary> findByTradeHistoryId(Long tradeHistoryId);
}
