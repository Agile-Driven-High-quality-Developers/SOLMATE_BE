package org.solmate.domain.social.dto.response;

import java.time.LocalDateTime;

import org.solmate.domain.social.entity.Comment;

public record CommentResponse(
    Long commentId,
    Long userId,
    String nickname,
    boolean isMentor,       // 댓글 작성자가 일지 주인의 멘토인지
    String content,
    LocalDateTime createdAt
) {
    public static CommentResponse of(Comment comment, boolean isMentor) {
        boolean withdrawn = comment.getUser().isWithdrawn();
        String nickname = withdrawn ? "탈퇴한 사용자" : comment.getUser().getNickname();
        Long userId = withdrawn ? null : comment.getUser().getId();
        return new CommentResponse(
            comment.getId(),
            userId,
            nickname,
            isMentor,
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}
