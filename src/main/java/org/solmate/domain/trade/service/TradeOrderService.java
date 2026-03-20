package org.solmate.domain.trade.service;

import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.trade.dto.response.TradeOrderResponse;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.repository.TradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeOrderService {

    private final TradeHistoryRepository tradeHistoryRepository;

    public List<TradeOrderResponse> getOrders(Long userId, String stockCode) {
        return tradeHistoryRepository.findByUserIdAndStockTickerCode(userId, stockCode)
                .stream()
                .map(TradeOrderResponse::from)
                .toList();
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        TradeHistory tradeHistory = tradeHistoryRepository.findById(orderId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        if (!tradeHistory.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        if (tradeHistory.getTradeStatus() != TradeStatus.PENDING) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        tradeHistory.updateStatus(TradeStatus.CANCELLED);
    }
}
