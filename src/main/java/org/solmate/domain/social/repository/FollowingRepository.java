package org.solmate.domain.social.repository;

import java.util.Optional;

import org.solmate.domain.social.entity.Following;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowingRepository extends JpaRepository<Following, Long> {

    // 특정 팔로우 관계가 존재하는지 확인 (중복 팔로우 방지)
    boolean existsByFollowerAndFollowing(User follower, User following);

    // 특정 팔로우 관계 조회 (팔로우 취소 시 사용)
    Optional<Following> findByFollowerAndFollowing(User follower, User following);
}
