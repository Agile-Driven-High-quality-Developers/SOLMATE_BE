package org.solmate.domain.stock.repository;

import java.util.Optional;

import org.solmate.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByTickerCode(String tickerCode);
}
