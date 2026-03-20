package org.solmate.domain.trade.repository;

import java.util.List;
import java.util.Optional;

import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingsRepository extends JpaRepository<Holdings, Long> {

    Optional<Holdings> findByUserAndTickerCode(User user, String tickerCode);

    List<Holdings> findByUser(User user);
}
