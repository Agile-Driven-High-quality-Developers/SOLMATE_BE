package org.solmate.domain.trade.repository;

import java.util.List;

import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {

    List<TradeHistory> findByUserAndTradeStatus(User user, TradeStatus tradeStatus);

    @Query("SELECT t FROM TradeHistory t JOIN FETCH t.stock WHERE t.user.id = :userId AND t.stock.tickerCode = :tickerCode ORDER BY t.createdAt DESC")
    List<TradeHistory> findByUserIdAndTickerCode(@Param("userId") Long userId, @Param("tickerCode") String tickerCode);
}
