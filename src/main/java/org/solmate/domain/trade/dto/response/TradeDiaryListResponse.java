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
    long commentCount,
    LocalDateTime createdAt
) {
    public static TradeDiaryListResponse of(TradeDiary diary, CommentRepository commentRepository) {
        var tradeHistory = diary.getTradeHistory();
        boolean isSell = tradeHistory.getTradeType() == TradeType.SELL;

        return new TradeDiaryListResponse(
            diary.getId(),
            tradeHistory.getTradeType().name(),
            tradeHistory.getStock().getStockName(),
            tradeHistory.getPrice(),
            tradeHistory.getQuantity(),
            isSell ? tradeHistory.getProfit() : null,
            diary.getContent(),
            commentRepository.countByTradeDiaryIdAndIsDeletedFalse(diary.getId()),
            diary.getCreatedAt()
        );
    }
}
