package org.solmate.domain.trade.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.solmate.domain.social.repository.CommentRepository;
import org.solmate.domain.trade.entity.TradeDiary;
import org.solmate.domain.trade.enums.TradeType;

public record TradeDiaryListResponse(
    Long diaryId,
    String tradeType,
    String stockName,
    BigDecimal filledPrice,
    BigDecimal quantity,
    BigDecimal profit,       // 수익금 (매도일 때만, 매수는 null)
    String content,
    Long commentCount,       // 멘토/멘티 관계일 때만, 아니면 null
    LocalDateTime createdAt
) {
    public static TradeDiaryListResponse of(TradeDiary diary, CommentRepository commentRepository, boolean includeCommentCount) {
        var tradeHistory = diary.getTradeHistory();
        boolean isSell = tradeHistory.getTradeType() == TradeType.SELL;

        // 수익금 = (체결가 - avgPriceSnapshot) * 수량 (매도일 때만)
        BigDecimal profit = null;
        if (isSell && tradeHistory.getAvgPriceSnapshot() != null) {
            profit = tradeHistory.getPrice()
                .subtract(tradeHistory.getAvgPriceSnapshot())
                .multiply(tradeHistory.getQuantity());
        }

        Long commentCount = includeCommentCount
            ? commentRepository.countByTradeDiaryIdAndIsDeletedFalse(diary.getId())
            : null;

        return new TradeDiaryListResponse(
            diary.getId(),
            tradeHistory.getTradeType().name(),
            tradeHistory.getStock().getStockName(),
            tradeHistory.getPrice(),
            tradeHistory.getQuantity(),
            profit,
            diary.getContent(),
            commentCount,
            diary.getCreatedAt()
        );
    }
}
