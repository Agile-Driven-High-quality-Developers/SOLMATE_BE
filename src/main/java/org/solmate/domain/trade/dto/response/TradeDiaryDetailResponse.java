package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.solmate.domain.social.entity.Comment;
import org.solmate.domain.social.enums.MentoringStatus;
import org.solmate.domain.social.repository.MentoringRepository;
import org.solmate.domain.trade.entity.TradeDiary;
import org.solmate.domain.trade.enums.TradeType;

public record TradeDiaryDetailResponse(
    Long diaryId,
    String tradeType,
    String stockName,
    String tickerCode,
    BigDecimal filledPrice,     // 체결가
    BigDecimal quantity,        // 수량
    BigDecimal profit,          // 수익금 (매도일 때만, 매수는 null)
    BigDecimal profitRate,      // 수익률 % (매도일 때만, 매수는 null)
    String content,             // 매매일지 내용
    LocalDateTime createdAt,
    List<CommentInfo> comments
) {
    public record CommentInfo(
        Long commentId,
        String nickname,
        boolean isMentor,       // 댓글 작성자가 나의 멘토인지
        String content
    ) {}

    public static TradeDiaryDetailResponse of(TradeDiary diary, List<Comment> comments,
                                               MentoringRepository mentoringRepository, Long currentUserId) {
        var tradeHistory = diary.getTradeHistory();
        boolean isSell = tradeHistory.getTradeType() == TradeType.SELL;

        // 수익금 = (체결가 - avgPriceSnapshot) * 수량 (매도일 때만)
        BigDecimal profit = null;
        BigDecimal profitRate = null;
        if (isSell && tradeHistory.getAvgPriceSnapshot() != null) {
            profit = tradeHistory.getPrice()
                .subtract(tradeHistory.getAvgPriceSnapshot())
                .multiply(tradeHistory.getQuantity());

            // 수익률 = (체결가 - avgPriceSnapshot) / avgPriceSnapshot * 100
            profitRate = tradeHistory.getPrice()
                .subtract(tradeHistory.getAvgPriceSnapshot())
                .divide(tradeHistory.getAvgPriceSnapshot(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        // 댓글 목록 변환 (댓글 작성자가 나의 멘토인지 확인)
        List<CommentInfo> commentInfos = comments.stream()
            .map(comment -> new CommentInfo(
                comment.getId(),
                comment.getUser().isWithdrawn() ? "탈퇴한 사용자" : comment.getUser().getNickname(),
                mentoringRepository.existsByMentorIdAndMenteeIdAndStatus(
                    comment.getUser().getId(), currentUserId, MentoringStatus.ACCEPTED),
                comment.getContent()
            ))
            .toList();

        return new TradeDiaryDetailResponse(
            diary.getId(),
            tradeHistory.getTradeType().name(),
            tradeHistory.getStock().getStockName(),
            tradeHistory.getStock().getTickerCode(),
            tradeHistory.getPrice(),
            tradeHistory.getQuantity(),
            profit,
            profitRate,
            diary.getContent(),
            diary.getCreatedAt(),
            commentInfos
        );
    }
}
