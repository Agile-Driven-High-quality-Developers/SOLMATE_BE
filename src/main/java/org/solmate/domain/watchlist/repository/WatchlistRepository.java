package org.solmate.domain.watchlist.repository;

import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    boolean existsByUserAndStock(User user, Stock stock);

    Optional<Watchlist> findByUserAndStock(User user, Stock stock);

    @Query("SELECT w.stock.tickerCode FROM Watchlist w WHERE w.user.id = :userId")
    List<String> findTickerCodesByUserId(@Param("userId") Long userId);
}
