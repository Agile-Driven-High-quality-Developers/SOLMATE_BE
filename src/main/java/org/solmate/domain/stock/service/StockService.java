package org.solmate.domain.stock.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.solmate.common.s3.S3Service;
import org.solmate.domain.stock.dto.response.StockListResponse;
import org.solmate.domain.stock.dto.response.StockQuoteResponse;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.enums.SectorType;
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

    @Transactional(readOnly = true)
    public List<StockListResponse> getStockList() {
        List<Stock> stocks = stockRepository.findAll();
        List<String> tickerCodes = stocks.stream().map(Stock::getTickerCode).toList();
        List<Map<Object, Object>> redisInfoList = stockInfoService.getStockInfoBulk(tickerCodes);

        // Redis 데이터 없는 종목 코드 수집
        List<String> missingCodes = new ArrayList<>();
        for (int i = 0; i < stocks.size(); i++) {
            if (redisInfoList.get(i).isEmpty()) {
                missingCodes.add(stocks.get(i).getTickerCode());
            }
        }

        // DB N+1 제거: 최근 일봉을 한 번에 조회
        Map<String, Long> closePriceMap = missingCodes.isEmpty() ? Map.of() :
                dailyCandleRepository.findLatestByStockCodes(missingCodes).stream()
                        .collect(Collectors.toMap(DailyCandle::getStockCode, DailyCandle::getClosePrice));

        List<StockListResponse> responses = new ArrayList<>();
        for (int i = 0; i < stocks.size(); i++) {
            Stock stock = stocks.get(i);
            String logoUrl = stock.getStockLogo() != null ? s3Service.buildFileUrl(stock.getStockLogo()) : null;
            Map<Object, Object> redisInfo = redisInfoList.get(i);
            if (redisInfo.isEmpty()) {
                long closePrice = closePriceMap.getOrDefault(stock.getTickerCode(), 0L);
                responses.add(StockListResponse.ofWithClosePrice(stock, closePrice, logoUrl));
            } else {
                responses.add(StockListResponse.of(stock, redisInfo, logoUrl));
            }
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public StockQuoteResponse getQuote(String stockCode) {
        LsQuoteResponse response = lsApiClient.getQuote(stockCode);
        Stock stock = stockRepository.findByTickerCode(stockCode).orElse(null);
        String stockLogo = stock != null && stock.getStockLogo() != null ? s3Service.buildFileUrl(stock.getStockLogo()) : null;
        SectorType sectorType = stock != null ? stock.getSectorType() : null;
        return StockQuoteResponse.from(response, stockLogo, sectorType);
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
