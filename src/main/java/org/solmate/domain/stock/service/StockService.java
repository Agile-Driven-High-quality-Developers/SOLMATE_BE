package org.solmate.domain.stock.service;

import org.solmate.domain.stock.dto.response.StockListResponse;
import org.solmate.domain.stock.dto.response.StockQuoteResponse;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.external.ls.client.LsApiClient;
import org.solmate.external.ls.dto.response.LsQuoteResponse;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final LsApiClient lsApiClient;
    private final StockRepository stockRepository;
    private final StockInfoService stockInfoService;
    private final DailyCandleRepository dailyCandleRepository;

    public List<StockListResponse> getStockList() {
        return stockRepository.findAll().stream()
                .map(stock -> {
                    Map<Object, Object> redisInfo = stockInfoService.getStockInfo(stock.getTickerCode());
                    if (redisInfo.isEmpty()) {
                        long closePrice = dailyCandleRepository
                                .findTopByStockCodeOrderByCandleTimeDesc(stock.getTickerCode())
                                .map(DailyCandle::getClosePrice)
                                .orElse(0L);
                        return StockListResponse.ofWithClosePrice(stock, closePrice);
                    }
                    return StockListResponse.of(stock, redisInfo);
                })
                .toList();
    }

    public StockQuoteResponse getQuote(String stockCode) {
        LsQuoteResponse response = lsApiClient.getQuote(stockCode);
        return StockQuoteResponse.from(response);
    }
}
