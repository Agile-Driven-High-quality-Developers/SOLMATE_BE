package org.solmate.domain.trade.service;

import java.util.List;

import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.domain.trade.dto.response.TradeHistoryResponse;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.repository.TradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final StockRepository stockRepository;

    public TradeHistoryResponse getTradeHistories(Long userId, String tickerCode) {
        List<TradeHistory> trades = tradeHistoryRepository.findByUserIdAndTickerCode(userId, tickerCode);

        String stockName = trades.isEmpty()
                ? stockRepository.findByTickerCode(tickerCode)
                        .map(Stock::getStockName)
                        .orElse("")
                : trades.get(0).getStock().getStockName();

        return TradeHistoryResponse.of(tickerCode, stockName, trades);
    }
}
