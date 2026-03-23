package org.solmate.domain.social.repository;

import java.util.List;

import org.solmate.domain.social.entity.MentoringRelation;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
