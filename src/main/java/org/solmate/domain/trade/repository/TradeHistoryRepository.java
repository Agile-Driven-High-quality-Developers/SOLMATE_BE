package org.solmate.domain.trade.repository;

import java.util.List;
import java.util.Optional;

import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {

    List<TradeHistory> findByUserAndTradeStatus(User user, TradeStatus tradeStatus);

    // 주문 취소 시 중복 취소 방지를 위한 비관적 락 (SELECT FOR UPDATE)
    // 동시에 같은 주문에 취소 요청이 들어오면 DB에서 row를 잠가 순서대로 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TradeHistory t WHERE t.id = :id")
    Optional<TradeHistory> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT t FROM TradeHistory t JOIN FETCH t.stock WHERE t.user.id = :userId AND t.stock.tickerCode = :tickerCode ORDER BY t.createdAt DESC")
    List<TradeHistory> findByUserIdAndTickerCode(@Param("userId") Long userId, @Param("tickerCode") String tickerCode);

    @Query("SELECT t FROM TradeHistory t JOIN FETCH t.stock WHERE t.user.id = :userId AND t.tradeStatus = 'FILLED' ORDER BY t.createdAt DESC")
    List<TradeHistory> findFilledByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM TradeHistory t WHERE t.user.id = :userId AND t.stock.tickerCode = :tickerCode AND t.tradeStatus = 'PENDING' AND t.tradeType = :tradeType")
    List<TradeHistory> findPendingByUserIdAndTickerCodeAndTradeType(
            @Param("userId") Long userId,
            @Param("tickerCode") String tickerCode,
            @Param("tradeType") org.solmate.domain.trade.enums.TradeType tradeType);

    @Query("SELECT t FROM TradeHistory t WHERE t.user.id = :userId AND t.tradeStatus = 'PENDING' AND t.tradeType = :tradeType")
    List<TradeHistory> findPendingByUserIdAndTradeType(
            @Param("userId") Long userId,
            @Param("tradeType") org.solmate.domain.trade.enums.TradeType tradeType);
}
