package org.solmate.domain.account.repository;

import java.util.Optional;

import org.solmate.domain.account.entity.Account;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUser(User user);
}
