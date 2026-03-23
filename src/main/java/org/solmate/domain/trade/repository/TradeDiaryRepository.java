package org.solmate.domain.trade.repository;

import java.util.List;
import java.util.Optional;

import org.solmate.domain.trade.entity.TradeDiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeDiaryRepository extends JpaRepository<TradeDiary, Long> {

    void deleteByTradeHistoryId(Long tradeHistoryId);

    Optional<TradeDiary> findByTradeHistoryId(Long tradeHistoryId);

    // 내 매매일지 목록 조회 (최신순, 체결된 것만)
    @Query("SELECT td FROM TradeDiary td JOIN FETCH td.tradeHistory th JOIN FETCH th.stock WHERE td.user.id = :userId AND td.status = 'FILLED' ORDER BY td.createdAt DESC")
    List<TradeDiary> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // 종목명 검색 (최신순, 체결된 것만)
    @Query("SELECT td FROM TradeDiary td JOIN FETCH td.tradeHistory th JOIN FETCH th.stock s WHERE td.user.id = :userId AND td.status = 'FILLED' AND s.stockName LIKE %:stockName% ORDER BY td.createdAt DESC")
    List<TradeDiary> findAllByUserIdAndStockNameContaining(@Param("userId") Long userId, @Param("stockName") String stockName);
}
