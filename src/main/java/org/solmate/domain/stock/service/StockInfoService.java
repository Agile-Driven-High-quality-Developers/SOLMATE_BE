package org.solmate.domain.stock.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.solmate.external.ls.dto.websocket.LsWsStockResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInfoService {

    private static final String INFO_KEY_PREFIX = "stock:info:";

    private final StringRedisTemplate redisTemplate;

    public Map<Object, Object> getStockInfo(String stockCode) {
        return redisTemplate.opsForHash().entries(INFO_KEY_PREFIX + stockCode);
    }

    @SuppressWarnings("unchecked")
    public List<Map<Object, Object>> getStockInfoBulk(List<String> stockCodes) {
        List<Object> results = redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (String code : stockCodes) {
                byte[] key = (INFO_KEY_PREFIX + code).getBytes();
                connection.hashCommands().hGetAll(key);
            }
            return null;
        });

        List<Map<Object, Object>> mapped = new ArrayList<>(results.size());
        for (Object result : results) {
            mapped.add(result instanceof Map ? (Map<Object, Object>) result : Map.of());
        }
        return mapped;
    }

    public Map<String, BigDecimal> getCurrentPriceBulk(List<String> stockCodes) {
        List<Object> results = redisTemplate.executePipelined(
                (org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    for (String code : stockCodes) {
                        connection.hashCommands().hGet(
                                (INFO_KEY_PREFIX + code).getBytes(),
                                "cur".getBytes()
                        );
                    }
                    return null;
                });

        Map<String, BigDecimal> priceMap = new HashMap<>();
        for (int i = 0; i < stockCodes.size(); i++) {
            Object val = results.get(i);
            if (val != null) {
                String priceStr = val instanceof byte[] ? new String((byte[]) val) : val.toString();
                priceMap.put(stockCodes.get(i), new BigDecimal(priceStr));
            }
        }
        return priceMap;
    }

    public void updateTotal(String stockCode, long total) {
        redisTemplate.opsForHash().put(INFO_KEY_PREFIX + stockCode, "total", String.valueOf(total));
    }

    public void syncFromQuote(String stockCode, long cur, long open, long high, long low, long vol, double chgRate, long total) {
        redisTemplate.opsForHash().putAll(INFO_KEY_PREFIX + stockCode, Map.of(
                "cur", String.valueOf(cur),
                "open", String.valueOf(open),
                "high", String.valueOf(high),
                "low", String.valueOf(low),
                "vol", String.valueOf(vol),
                "chgRate", String.valueOf(chgRate),
                "total", String.valueOf(total)
        ));
    }

    public void update(LsWsStockResponse.Body body) {
        redisTemplate.opsForHash().putAll(INFO_KEY_PREFIX + body.shcode(), Map.of(
                "cur", body.price() == null ? "0" : body.price().trim(),
                "open", body.open() == null ? "0" : body.open().trim(),
                "high", body.high() == null ? "0" : body.high().trim(),
                "low", body.low() == null ? "0" : body.low().trim(),
                "vol", body.volume() == null ? "0" : body.volume().trim(),
                "chgRate", body.drate() == null ? "0" : body.drate().trim()
        ));
    }
}
