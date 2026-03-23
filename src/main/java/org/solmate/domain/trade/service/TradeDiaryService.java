package org.solmate.domain.trade.service;

import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.social.repository.CommentRepository;
import org.solmate.domain.social.repository.MentoringRepository;
import org.solmate.domain.trade.dto.request.UpdateTradeDiaryRequest;
import org.solmate.domain.trade.dto.response.TradeDiaryDetailResponse;
import org.solmate.domain.trade.dto.response.TradeDiaryListResponse;
import org.solmate.domain.trade.repository.TradeDiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeDiaryService {

    private final TradeDiaryRepository tradeDiaryRepository;
    private final CommentRepository commentRepository;
    private final MentoringRepository mentoringRepository;

    @Transactional(readOnly = true)
    public List<TradeDiaryListResponse> getMyDiaries(Long userId, String stockName) {
        var diaries = (stockName != null && !stockName.isBlank())
            ? tradeDiaryRepository.findAllByUserIdAndStockNameContaining(userId, stockName)
            : tradeDiaryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return diaries.stream()
            .map(diary -> TradeDiaryListResponse.of(diary, commentRepository, true))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TradeDiaryListResponse> getUserDiaries(Long targetUserId, Long currentUserId) {
        // 멘토/멘티 관계이면 댓글 수 포함, 아니면 null
        boolean isMentoringRelation =
            mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(currentUserId, targetUserId, MentoringStatus.ACCEPTED)
            || mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(targetUserId, currentUserId, MentoringStatus.ACCEPTED);

        return tradeDiaryRepository.findAllByUserIdOrderByCreatedAtDesc(targetUserId)
            .stream()
            .map(diary -> TradeDiaryListResponse.of(diary, commentRepository, isMentoringRelation))
            .toList();
    }

    @Transactional(readOnly = true)
    public TradeDiaryDetailResponse getDiaryDetail(Long diaryId, Long currentUserId) {
        var diary = tradeDiaryRepository.findById(diaryId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.TRADE_DIARY_NOT_FOUND));

        Long diaryOwnerId = diary.getUser().getId();

        // 내 매매일지가 아닌 경우 멘토/멘티 관계 확인
        if (!diaryOwnerId.equals(currentUserId)) {
            boolean isMentoringRelation =
                // 내가 일지 주인의 멘토인 경우
                mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(currentUserId, diaryOwnerId, MentoringStatus.ACCEPTED)
                // 내가 일지 주인의 멘티인 경우
                || mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(diaryOwnerId, currentUserId, MentoringStatus.ACCEPTED);

            if (!isMentoringRelation) {
                throw new GeneralException(ErrorStatus.FORBIDDEN);
            }
        }

        var comments = commentRepository.findAllByTradeDiaryIdAndIsDeletedFalseOrderByCreatedAtAsc(diaryId);

        return TradeDiaryDetailResponse.of(diary, comments, mentoringRepository, currentUserId);
    }

    @Transactional
    public TradeDiaryDetailResponse updateDiary(Long diaryId, Long currentUserId, UpdateTradeDiaryRequest request) {
        var diary = tradeDiaryRepository.findById(diaryId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.TRADE_DIARY_NOT_FOUND));

        // 본인 매매일지만 수정 가능
        if (!diary.getUser().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        diary.updateContent(request.content());

        var comments = commentRepository.findAllByTradeDiaryIdAndIsDeletedFalseOrderByCreatedAtAsc(diaryId);
        return TradeDiaryDetailResponse.of(diary, comments, mentoringRepository, currentUserId);
    }
}
