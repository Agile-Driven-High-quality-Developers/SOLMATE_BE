package org.solmate.domain.trade.repository;

import java.util.List;
import java.util.Optional;

import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface HoldingsRepository extends JpaRepository<Holdings, Long> {

    Optional<Holdings> findByUserAndTickerCode(User user, String tickerCode);

    // 매도 취소 시 주식 수량 복원할 때 동시 접근으로 인한 수량 덮어쓰기 방지를 위한 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holdings h WHERE h.user = :user AND h.tickerCode = :tickerCode")
    Optional<Holdings> findByUserAndTickerCodeWithLock(@Param("user") User user, @Param("tickerCode") String tickerCode);

    List<Holdings> findByUser(User user);

    @Query("SELECT h FROM Holdings h JOIN FETCH h.stock WHERE h.user.id = :userId")
    List<Holdings> findByUserId(@Param("userId") Long userId);

    @Query("SELECT h FROM Holdings h WHERE h.user.id = :userId AND h.tickerCode = :tickerCode")
    Optional<Holdings> findByUserIdAndTickerCode(@Param("userId") Long userId, @Param("tickerCode") String tickerCode);
}
