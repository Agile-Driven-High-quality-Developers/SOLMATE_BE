package org.solmate.domain.stock.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.solmate.common.s3.S3Service;
import org.solmate.domain.stock.dto.response.StockListResponse;
import org.solmate.domain.stock.dto.response.StockQuoteResponse;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.external.ls.client.LsApiClient;
import org.solmate.external.ls.dto.response.LsQuoteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final LsApiClient lsApiClient;
    private final StockRepository stockRepository;
    private final StockInfoService stockInfoService;
    private final DailyCandleRepository dailyCandleRepository;
    private final S3Service s3Service;

    public List<StockListResponse> getStockList() {
        return stockRepository.findAll().stream()
                .map(stock -> {
                    String logoUrl = stock.getStockLogo() != null ? s3Service.buildFileUrl(stock.getStockLogo()) : null;
                    Map<Object, Object> redisInfo = stockInfoService.getStockInfo(stock.getTickerCode());
                    if (redisInfo.isEmpty()) {
                        long closePrice = dailyCandleRepository
                                .findTopByStockCodeOrderByCandleTimeDesc(stock.getTickerCode())
                                .map(DailyCandle::getClosePrice)
                                .orElse(0L);
                        return StockListResponse.ofWithClosePrice(stock, closePrice, logoUrl);
                    }
                    return StockListResponse.of(stock, redisInfo, logoUrl);
                })
                .toList();
    }

    public StockQuoteResponse getQuote(String stockCode) {
        LsQuoteResponse response = lsApiClient.getQuote(stockCode);
        String stockLogo = stockRepository.findByTickerCode(stockCode)
                .map(stock -> stock.getStockLogo() != null ? s3Service.buildFileUrl(stock.getStockLogo()) : null)
                .orElse(null);
        return StockQuoteResponse.from(response, stockLogo);
    }

    @Transactional
    public void updateAllMarketCap() {
        List<Stock> stocks = stockRepository.findAll();
        for (Stock stock : stocks) {
            try {
                LsQuoteResponse response = lsApiClient.getQuote(stock.getTickerCode());
                BigDecimal total = BigDecimal.valueOf(response.t1102OutBlock().total());
                stock.updateTotal(total);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("시가총액 업데이트 중단 - 종목: {}", stock.getTickerCode());
                break;
            } catch (Exception e) {
                log.warn("시가총액 업데이트 실패 - 종목: {}, 사유: {}", stock.getTickerCode(), e.getMessage());
            }
        }
    }
}
