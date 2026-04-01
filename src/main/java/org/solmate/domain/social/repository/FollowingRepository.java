package org.solmate.domain.social.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.solmate.domain.social.entity.Following;
import org.solmate.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowingRepository extends JpaRepository<Following, Long> {

    // 특정 팔로우 관계가 존재하는지 확인 (중복 팔로우 방지)
    boolean existsByFollowerAndFollowing(User follower, User following);

    // 특정 팔로우 관계 조회 (팔로우 취소 시 사용)
    Optional<Following> findByFollowerAndFollowing(User follower, User following);

    // 유저 ID 목록에 대한 팔로워 수 bulk 집계 (N+1 방지)
    // 반환: [userId, count] 형태의 Object[] 리스트
    // 팔로워가 없는 유저는 결과에 포함되지 않으므로 서비스에서 getOrDefault(0L) 처리 필요
    @Query("SELECT f.following.id, COUNT(f) FROM Following f WHERE f.following.id IN :userIds GROUP BY f.following.id")
    List<Object[]> countFollowersByUserIds(@Param("userIds") List<Long> userIds);

    // 유저 ID 목록에 대한 팔로잉 수 bulk 집계 (N+1 방지)
    // 반환: [userId, count] 형태의 Object[] 리스트
    // 팔로잉이 없는 유저는 결과에 포함되지 않으므로 서비스에서 getOrDefault(0L) 처리 필요
    @Query("SELECT f.follower.id, COUNT(f) FROM Following f WHERE f.follower.id IN :userIds GROUP BY f.follower.id")
    List<Object[]> countFollowingByUserIds(@Param("userIds") List<Long> userIds);

    // 현재 유저가 팔로우하는 유저 ID Set 조회
    // 유저 목록에서 isFollowing 여부를 Set.contains() O(1)로 일괄 확인하기 위해 사용
    @Query("SELECT f.following.id FROM Following f WHERE f.follower.id = :followerId")
    Set<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);

    // 나를 팔로우하는 유저 목록 조회 (체결 알림 전송용)
    @Query("SELECT f.follower FROM Following f WHERE f.following.id = :followingId")
    List<User> findAllFollowersByFollowingId(@Param("followingId") Long followingId);

    // 탈퇴 시 팔로우 관계 전체 삭제
    void deleteAllByFollowerId(Long followerId);
    void deleteAllByFollowingId(Long followingId);
    // 팔로워 목록 조회 - userId를 팔로우하는 사람들 (첫 페이지)
    @Query("SELECT f.follower FROM Following f WHERE f.following.id = :userId AND f.follower.deletedAt IS NULL ORDER BY f.follower.id ASC")
    List<User> findFollowersFirstPage(@Param("userId") Long userId, Pageable pageable);

    // 팔로워 목록 조회 - userId를 팔로우하는 사람들 (다음 페이지, 커서 기반)
    @Query("SELECT f.follower FROM Following f WHERE f.following.id = :userId AND f.follower.id > :cursor AND f.follower.deletedAt IS NULL ORDER BY f.follower.id ASC")
    List<User> findFollowersWithCursor(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    // 팔로잉 목록 조회 - userId가 팔로우하는 사람들 (첫 페이지)
    @Query("SELECT f.following FROM Following f WHERE f.follower.id = :userId AND f.following.deletedAt IS NULL ORDER BY f.following.id ASC")
    List<User> findFollowingFirstPage(@Param("userId") Long userId, Pageable pageable);

    // 팔로잉 목록 조회 - userId가 팔로우하는 사람들 (다음 페이지, 커서 기반)
    @Query("SELECT f.following FROM Following f WHERE f.follower.id = :userId AND f.following.id > :cursor AND f.following.deletedAt IS NULL ORDER BY f.following.id ASC")
    List<User> findFollowingWithCursor(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);
}
