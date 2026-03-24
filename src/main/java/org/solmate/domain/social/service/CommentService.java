package org.solmate.domain.social.service;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.social.dto.request.CreateCommentRequest;
import org.solmate.domain.social.dto.request.UpdateCommentRequest;
import org.solmate.domain.social.dto.response.CommentResponse;
import org.solmate.domain.social.entity.Comment;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.social.repository.CommentRepository;
import org.solmate.domain.social.repository.MentoringRepository;
import org.solmate.domain.trade.entity.TradeDiary;
import org.solmate.domain.trade.repository.TradeDiaryRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TradeDiaryRepository tradeDiaryRepository;
    private final MentoringRepository mentoringRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(Long diaryId, Long currentUserId, CreateCommentRequest request) {
        TradeDiary diary = tradeDiaryRepository.findById(diaryId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.TRADE_DIARY_NOT_FOUND));

        checkMentoringRelation(currentUserId, diary.getUser().getId());

        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Comment comment = Comment.builder()
            .user(user)
            .tradeDiary(diary)
            .content(request.content())
            .build();

        commentRepository.save(comment);

        boolean isMentor = mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(
            currentUserId, diary.getUser().getId(), MentoringStatus.ACCEPTED);

        return CommentResponse.of(comment, isMentor);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long currentUserId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new GeneralException(ErrorStatus.COMMENT_NOT_FOUND);
        }

        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        comment.updateContent(request.content());

        Long diaryOwnerId = comment.getTradeDiary().getUser().getId();
        boolean isMentor = mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(
            currentUserId, diaryOwnerId, MentoringStatus.ACCEPTED);

        return CommentResponse.of(comment, isMentor);
    }

    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new GeneralException(ErrorStatus.COMMENT_NOT_FOUND);
        }

        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        comment.delete();
    }

    private void checkMentoringRelation(Long currentUserId, Long diaryOwnerId) {
        if (currentUserId.equals(diaryOwnerId)) return;

        boolean isMentoringRelation =
            mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(currentUserId, diaryOwnerId, MentoringStatus.ACCEPTED)
            || mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(diaryOwnerId, currentUserId, MentoringStatus.ACCEPTED);

        if (!isMentoringRelation) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }
    }
}
