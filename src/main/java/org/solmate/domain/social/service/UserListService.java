package org.solmate.domain.social.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.social.dto.response.FollowListItemResponse;
import org.solmate.domain.social.dto.response.FollowListResponse;
import org.solmate.domain.social.dto.response.UserListItemResponse;
import org.solmate.domain.social.dto.response.UserListResponse;
import org.solmate.domain.social.dto.response.UserProfileResponse;
import org.solmate.domain.social.entity.MentoringRelation;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.social.enums.UserMentoringStatus;
import org.solmate.domain.social.repository.FollowingRepository;
import org.solmate.domain.social.repository.MentoringRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserListService {

    private final UserRepository userRepository;
    private final FollowingRepository followingRepository;
    private final MentoringRepository mentoringRepository;

    /**
     * 유저 목록 조회 (커서 기반 무한 스크롤)
     *
     * 처리 순서 (총 5번의 쿼리로 N+1 없이 처리):
     *   ① 유저 목록 조회 (cursor 기반 - 첫 페이지면 cursor=null)
     *   ② 내가 팔로우하는 유저 ID 목록 (팔로우 여부 일괄 확인)
     *   ③ 나의 멘토링 관계 일괄 조회 (PENDING + ACCEPTED)
     *   ④ 유저 ID 목록으로 팔로워 수 bulk 집계
     *   ⑤ 유저 ID 목록으로 팔로잉 수 bulk 집계
     *
     * hasNext 판별:
     *   size+1개를 조회해서 실제 size보다 많으면 다음 페이지가 있음
     *   nextCursor는 현재 페이지 마지막 유저의 userId
     *
     * hasAcceptedMentor:
     *   true이면 프론트에서 NONE 상태의 멘토신청 버튼을 모두 비활성화해야 함
     *   (이미 멘토가 있는 경우 추가 신청 불가)
     */
    public UserListResponse getUserList(Long currentUserId, Long cursor, int size) {
        // ① 유저 목록 조회 (size+1개 조회로 hasNext 판별)
        List<User> users = cursor == null
                ? userRepository.findByDeletedAtIsNullOrderByIdAsc(PageRequest.of(0, size + 1))
                : userRepository.findByIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(cursor, PageRequest.of(0, size + 1));

        // size+1개가 왔으면 다음 페이지가 존재함 → 실제 반환은 size개만
        boolean hasNext = users.size() > size;
        if (hasNext) {
            users = users.subList(0, size);
        }
        // 다음 페이지 커서 = 현재 페이지 마지막 유저의 userId (없으면 null)
        Long nextCursor = hasNext ? users.get(users.size() - 1).getId() : null;

        List<Long> userIds = users.stream().map(User::getId).toList();

        // ② 내가 팔로우하는 유저 ID 목록 (팔로우 여부 일괄 확인용)
        Set<Long> followingIds = followingRepository.findFollowingIdsByFollowerId(currentUserId);

        // ③ 나의 멘토링 관계 일괄 조회 (PENDING + ACCEPTED만 - REJECTED는 버튼 표시 불필요)
        List<MentoringRelation> myRelations = mentoringRepository.findByMenteeIdAndStatusIn(
                currentUserId, List.of(MentoringStatus.PENDING, MentoringStatus.ACCEPTED));

        // 수락된 멘토가 있는지 여부 (true이면 NONE 상태 멘토신청 버튼 비활성화)
        boolean hasAcceptedMentor = myRelations.stream()
                .anyMatch(r -> r.getStatus() == MentoringStatus.ACCEPTED);

        // mentorId → UserMentoringStatus 맵으로 변환 (유저별 멘토링 상태 빠른 조회용)
        Map<Long, UserMentoringStatus> mentoringStatusMap = myRelations.stream()
                .collect(Collectors.toMap(
                        r -> r.getMentor().getId(),
                        r -> r.getStatus() == MentoringStatus.ACCEPTED
                                ? UserMentoringStatus.ACCEPTED
                                : UserMentoringStatus.PENDING
                ));

        // ④ 유저 ID 목록으로 팔로워 수 bulk 집계 (개별 count 쿼리 N번 → 1번으로 처리)
        Map<Long, Long> followerCountMap = followingRepository.countFollowersByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],  // userId
                        arr -> (Long) arr[1]   // count
                ));

        // ⑤ 유저 ID 목록으로 팔로잉 수 bulk 집계
        Map<Long, Long> followingCountMap = followingRepository.countFollowingByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],  // userId
                        arr -> (Long) arr[1]   // count
                ));

        // 각 유저에 대해 소셜 정보를 조합하여 응답 생성
        // 팔로워/팔로잉이 없는 유저는 0으로 기본값 처리
        List<UserListItemResponse> items = users.stream()
                .map(user -> UserListItemResponse.of(
                        user,
                        followerCountMap.getOrDefault(user.getId(), 0L),
                        followingCountMap.getOrDefault(user.getId(), 0L),
                        user.getId().equals(currentUserId),
                        followingIds.contains(user.getId()),
                        mentoringStatusMap.getOrDefault(user.getId(), UserMentoringStatus.NONE)
                ))
                .toList();

        return new UserListResponse(hasAcceptedMentor, items, nextCursor, hasNext);
    }

    /**
     * 유저 프로필 단건 조회
     *
     * - 탈퇴한 유저는 조회 불가 (USER_NOT_FOUND 예외)
     * - 본인 프로필 조회 시 isMe=true, isFollowing=false, mentoringStatus=NONE
     * - 타인 프로필 조회 시 현재 로그인 유저 기준으로 팔로우 여부 및 멘토링 상태 계산
     */
    public UserProfileResponse getUserProfile(Long currentUserId, Long targetUserId) {
        User target = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        boolean isMe = currentUserId.equals(targetUserId);

        // 대상 유저의 팔로워 수 (이 유저를 팔로우하는 사람 수)
        long followerCount = followingRepository.countFollowersByUserIds(List.of(targetUserId)).stream()
                .findFirst()
                .map(arr -> (Long) arr[1])
                .orElse(0L);

        // 대상 유저의 팔로잉 수 (이 유저가 팔로우하는 사람 수)
        long followingCount = followingRepository.countFollowingByUserIds(List.of(targetUserId)).stream()
                .findFirst()
                .map(arr -> (Long) arr[1])
                .orElse(0L);

        // 현재 로그인 유저가 대상 유저를 팔로우 중인지 (본인이면 항상 false)
        boolean isFollowing = !isMe && followingIds(currentUserId).contains(targetUserId);

        // 현재 로그인 유저 기준 대상 유저와의 멘토링 상태
        // 본인 프로필이면 NONE 고정, 타인이면 PENDING/ACCEPTED/NONE 중 하나
        UserMentoringStatus mentoringStatus = UserMentoringStatus.NONE;
        if (!isMe) {
            List<MentoringRelation> relations = mentoringRepository.findByMenteeIdAndStatusIn(
                    currentUserId, List.of(MentoringStatus.PENDING, MentoringStatus.ACCEPTED));
            mentoringStatus = relations.stream()
                    .filter(r -> r.getMentor().getId().equals(targetUserId))
                    .findFirst()
                    .map(r -> r.getStatus() == MentoringStatus.ACCEPTED
                            ? UserMentoringStatus.ACCEPTED
                            : UserMentoringStatus.PENDING)
                    .orElse(UserMentoringStatus.NONE);
        }

        return UserProfileResponse.of(target, followerCount, followingCount, isMe, isFollowing, mentoringStatus);
    }

    /**
     * 팔로워 목록 조회 (커서 기반 무한 스크롤)
     *
     * - targetUserId를 팔로우하는 사람들의 목록을 반환
     * - 탈퇴한 유저는 제외
     */
    public FollowListResponse getFollowerList(Long targetUserId, Long cursor, int size) {
        userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(0, size + 1);
        List<User> followers = cursor == null
                ? followingRepository.findFollowersFirstPage(targetUserId, pageable)
                : followingRepository.findFollowersWithCursor(targetUserId, cursor, pageable);

        boolean hasNext = followers.size() > size;
        if (hasNext) {
            followers = followers.subList(0, size);
        }
        Long nextCursor = hasNext ? followers.get(followers.size() - 1).getId() : null;

        List<FollowListItemResponse> items = followers.stream()
                .map(FollowListItemResponse::of)
                .toList();

        return new FollowListResponse(items, nextCursor, hasNext);
    }

    /**
     * 팔로잉 목록 조회 (커서 기반 무한 스크롤)
     *
     * - targetUserId가 팔로우하는 사람들의 목록을 반환
     * - 탈퇴한 유저는 제외
     */
    public FollowListResponse getFollowingList(Long targetUserId, Long cursor, int size) {
        userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(0, size + 1);
        List<User> followings = cursor == null
                ? followingRepository.findFollowingFirstPage(targetUserId, pageable)
                : followingRepository.findFollowingWithCursor(targetUserId, cursor, pageable);

        boolean hasNext = followings.size() > size;
        if (hasNext) {
            followings = followings.subList(0, size);
        }
        Long nextCursor = hasNext ? followings.get(followings.size() - 1).getId() : null;

        List<FollowListItemResponse> items = followings.stream()
                .map(FollowListItemResponse::of)
                .toList();

        return new FollowListResponse(items, nextCursor, hasNext);
    }

    /**
     * 현재 유저가 팔로우하는 유저 ID Set 조회
     * - 팔로우 여부 확인 시 contains() O(1) 조회를 위해 Set으로 반환
     */
    private Set<Long> followingIds(Long userId) {
        return followingRepository.findFollowingIdsByFollowerId(userId);
    }
}
