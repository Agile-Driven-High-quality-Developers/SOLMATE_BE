package org.solmate.domain.user.repository;

import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 탈퇴 유저 제외 조회 (로그인, 프로필 수정 등 실제 유저만 필요한 경우)
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    // 탈퇴 유저 포함 조회 (댓글, 매매일지 작성자 표시를 위한..)
    Optional<User> findByEmail(String email);
}
