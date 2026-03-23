package org.solmate.domain.social.repository;

import org.solmate.domain.social.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByTradeDiaryIdAndIsDeletedFalse(Long tradeDiaryId);
}
