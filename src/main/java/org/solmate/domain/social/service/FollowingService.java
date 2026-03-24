package org.solmate.domain.social.service;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.social.entity.Following;
import org.solmate.domain.social.repository.FollowingRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowingService {

    private final FollowingRepository followingRepository;
    private final UserRepository userRepository;

    /**
     * 팔로우 신청
     * - 자기 자신은 팔로우 불가
     * - 이미 팔로우한 유저에게 중복 팔로우 불가
     */
    @Transactional
    public void follow(Long followerId, Long followingId) {
        // 자기 자신 팔로우 방지 (프론트에서도 막지만 백엔드에서도 방어)
        if (followerId.equals(followingId)) {
            throw new GeneralException(ErrorStatus.FOLLOW_SELF_REQUEST);
        }

        User follower = userRepository.findByIdAndDeletedAtIsNull(followerId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        User following = userRepository.findByIdAndDeletedAtIsNull(followingId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 이미 팔로우 중인 경우 중복 신청 불가
        if (followingRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new GeneralException(ErrorStatus.FOLLOW_ALREADY_FOLLOWED);
        }

        followingRepository.save(Following.builder()
                .follower(follower)
                .following(following)
                .build());
    }

    /**
     * 팔로우 취소
     * - 팔로우 관계가 존재하지 않으면 예외 발생
     */
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        User follower = userRepository.findByIdAndDeletedAtIsNull(followerId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        User following = userRepository.findByIdAndDeletedAtIsNull(followingId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 팔로우 관계 조회 → 없으면 예외
        Following followRelation = followingRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new GeneralException(ErrorStatus.FOLLOW_NOT_FOUND));

        followingRepository.delete(followRelation);
    }
}
