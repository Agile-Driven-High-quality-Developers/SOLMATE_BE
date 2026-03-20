package org.solmate.domain.trade.repository;

import java.util.List;

import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {

    List<TradeHistory> findByUserAndTradeStatus(User user, TradeStatus tradeStatus);
}
