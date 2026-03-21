package org.solmate.domain.stock.repository;

import java.util.List;
import java.util.Optional;

import org.solmate.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByTickerCode(String tickerCode);

    @Query("SELECT s.tickerCode FROM Stock s")
    List<String> findAllTickerCodes();
}
