package org.solmate.domain.trade.service;

import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.social.repository.CommentRepository;
import org.solmate.domain.social.repository.MentoringRepository;
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
            .map(diary -> TradeDiaryListResponse.of(diary, commentRepository))
            .toList();
    }

    @Transactional(readOnly = true)
    public TradeDiaryDetailResponse getDiaryDetail(Long diaryId, Long currentUserId) {
        var diary = tradeDiaryRepository.findById(diaryId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.TRADE_DIARY_NOT_FOUND));

        var comments = commentRepository.findAllByTradeDiaryIdAndIsDeletedFalseOrderByCreatedAtAsc(diaryId);

        return TradeDiaryDetailResponse.of(diary, comments, mentoringRepository, currentUserId);
    }
}
