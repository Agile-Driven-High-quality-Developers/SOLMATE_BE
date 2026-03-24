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
        return new CommentResponse(
            comment.getId(),
            comment.getUser().getNickname(),
            isMentor,
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}
