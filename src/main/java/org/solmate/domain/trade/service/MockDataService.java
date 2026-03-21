package org.solmate.domain.trade.service;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MockDataService {

    private final StringRedisTemplate redisTemplate;

    public void initMockStockData() {
        initStockInfo("005930", Map.of(
                "cur", "73400",
                "open", "72500",
                "high", "73900",
                "low", "72300",
                "name", "삼성전자"
        ));
        initStockInfo("000660", Map.of(
                "cur", "192500",
                "open", "190000",
                "high", "193000",
                "low", "189500",
                "name", "SK하이닉스"
        ));
        initStockInfo("035720", Map.of(
                "cur", "44890",
                "open", "44500",
                "high", "45100",
                "low", "44300",
                "name", "카카오"
        ));

        initMarketIndicator("KOSPI", Map.of(
                "cur", "2648.73",
                "change", "+18.43",
                "rate", "+0.70",
                "sign", "1",
                "high", "2651.00",
                "low", "2629.00",
                "asOf", "1710123456"
        ));
        initMarketIndicator("KOSDAQ", Map.of(
                "cur", "871.26",
                "change", "-4.12",
                "rate", "-0.47",
                "sign", "5",
                "high", "876.00",
                "low", "869.00",
                "asOf", "1710123456"
        ));
        initMarketIndicator("USD_KRW", Map.of(
                "cur", "1342.50",
                "change", "-2.50",
                "rate", "-0.19",
                "sign", "5",
                "high", "1346.00",
                "low", "1340.00",
                "asOf", "1710123456"
        ));
    }

    private void initStockInfo(String ticker, Map<String, String> data) {
        String key = "stock:info:" + ticker;
        redisTemplate.delete(key);
        redisTemplate.opsForHash().putAll(key, data);
    }

    private void initMarketIndicator(String market, Map<String, String> data) {
        String key = "market:indicator:" + market;
        redisTemplate.delete(key);
        redisTemplate.opsForHash().putAll(key, data);
    }

    public String getCurrentPrice(String ticker) {
        Object value = redisTemplate.opsForHash().get("stock:info:" + ticker, "cur");
        return value != null ? value.toString() : null;
    }
}
