package org.solmate.domain.social.dto.response;

import java.time.LocalDateTime;

import org.solmate.domain.social.entity.Comment;

public record CommentResponse(
    Long commentId,
    String nickname,
    boolean isMentor,       // 댓글 작성자가 일지 주인의 멘토인지
    String content,
    LocalDateTime createdAt
) {
    public static CommentResponse of(Comment comment, boolean isMentor) {
        String nickname = comment.getUser().isWithdrawn() ? "탈퇴한 사용자" : comment.getUser().getNickname();
        return new CommentResponse(
            comment.getId(),
            nickname,
            isMentor,
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}
