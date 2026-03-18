package org.solmate.domain.auth.repository;

import org.solmate.domain.auth.entity.LoginType;
import org.solmate.domain.auth.enums.OAuthProvider;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginTypeRepository extends JpaRepository<LoginType, String> {

    Optional<LoginType> findByUserAndLoginType(User user, OAuthProvider loginType);

    boolean existsByUserAndLoginType(User user, OAuthProvider loginType);
}
