package org.solmate.domain.auth.repository;

import org.solmate.domain.auth.entity.LoginType;
import org.solmate.domain.auth.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {

    Optional<Token> findByLoginType(LoginType loginType);
}
