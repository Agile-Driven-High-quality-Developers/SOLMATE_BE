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

    // 내 매매일지 목록 조회 (최신순)
    @Query("SELECT td FROM TradeDiary td JOIN FETCH td.tradeHistory th JOIN FETCH th.stock WHERE td.user.id = :userId ORDER BY td.createdAt DESC")
    List<TradeDiary> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
