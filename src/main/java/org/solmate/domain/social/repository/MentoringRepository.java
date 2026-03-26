package org.solmate.domain.social.repository;

import java.util.List;
import java.util.Optional;

import org.solmate.domain.social.entity.MentoringRelation;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentoringRepository extends JpaRepository<MentoringRelation, Long> {

    // 멘티가 이미 수락된 멘토가 있는지 확인
    boolean existsByMenteeAndStatus(User mentee, MentoringStatus status);

    // 동일한 멘토-멘티 간 이미 PENDING 요청이 있는지 확인
    boolean existsByMentorAndMenteeAndStatus(User mentor, User mentee, MentoringStatus status);

    // 멘티가 보낸 특정 상태의 멘토링 요청 목록 조회
    List<MentoringRelation> findAllByMenteeAndStatus(User mentee, MentoringStatus status);

    // 멘티의 특정 멘토링 관계를 제외한 나머지 PENDING 요청 삭제
    // (멘토 수락 시 다른 멘토에게 보낸 PENDING 요청 정리)
    void deleteAllByMenteeAndStatusAndIdNot(User mentee, MentoringStatus status, Long excludeId);

    // 댓글 작성자가 나의 멘토인지 확인 (mentor=댓글작성자, mentee=나, status=ACCEPTED)
    boolean existsByMentorIdAndMenteeIdAndStatus(Long mentorId, Long menteeId, MentoringStatus status);

    // 내가 멘티인 멘토링 관계 일괄 조회 (유저 목록/프로필 멘토 버튼 상태 계산용)
    // PENDING + ACCEPTED 상태만 조회 (REJECTED는 버튼 표시 불필요)
    // 결과로 mentorId → UserMentoringStatus 맵을 구성하여 유저별 상태를 O(1)로 확인
    @Query("SELECT m FROM MentoringRelation m WHERE m.mentee.id = :menteeId AND m.status IN :statuses")
    List<MentoringRelation> findByMenteeIdAndStatusIn(@Param("menteeId") Long menteeId, @Param("statuses") List<MentoringStatus> statuses);

    // 멘토링 취소 시 (menteeId, mentorId)로 관계 조회
    @Query("SELECT m FROM MentoringRelation m WHERE m.mentee.id = :menteeId AND m.mentor.id = :mentorId AND m.status IN :statuses")
    List<MentoringRelation> findByMenteeIdAndMentorIdAndStatusIn(@Param("menteeId") Long menteeId, @Param("mentorId") Long mentorId, @Param("statuses") List<MentoringStatus> statuses);

    // 내 멘토 조회 (내가 멘티인 ACCEPTED 관계)
    Optional<MentoringRelation> findByMenteeIdAndStatus(Long menteeId, MentoringStatus status);

    // 내 멘티 목록 조회 (내가 멘토인 ACCEPTED 관계 전체)
    List<MentoringRelation> findAllByMentorIdAndStatus(Long mentorId, MentoringStatus status);
}
