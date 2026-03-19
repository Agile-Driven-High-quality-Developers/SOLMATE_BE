package org.solmate.domain.stock.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.domain.stock.repository.MinuteCandleRepository;
import org.solmate.external.ls.dto.websocket.LsWsStockResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleAccumulatorService {

    private static final String CANDLE_KEY_PREFIX = "candle:1min:";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final StringRedisTemplate redisTemplate;
    private final MinuteCandleRepository minuteCandleRepository;

    public void accumulate(LsWsStockResponse.Body body) {
        String stockCode = body.shcode();
        long price = Long.parseLong(body.price().trim());
        long cvolume = Long.parseLong(body.cvolume().trim());
        String key = CANDLE_KEY_PREFIX + stockCode;

        Map<Object, Object> current = redisTemplate.opsForHash().entries(key);

        if (current.isEmpty()) {

            redisTemplate.opsForHash().putAll(key, Map.of(
                    "open", String.valueOf(price),
                    "high", String.valueOf(price),
                    "low", String.valueOf(price),
                    "close", String.valueOf(price),
                    "volume", String.valueOf(cvolume),
                    "startTime", LocalDateTime.now().format(TIME_FORMATTER)
            ));
        } else {
            long high = Math.max(price, Long.parseLong((String) current.get("high")));
            long low = Math.min(price, Long.parseLong((String) current.get("low")));
            long volume = Long.parseLong((String) current.get("volume")) + cvolume;

            redisTemplate.opsForHash().putAll(key, Map.of(
                    "high", String.valueOf(high),
                    "low", String.valueOf(low),
                    "close", String.valueOf(price),
                    "volume", String.valueOf(volume)
            ));
        }
    }

    public void flushToDb(String stockCode) {
        String key = CANDLE_KEY_PREFIX + stockCode;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) return;

        try {
            String startTime = (String) data.get("startTime");
            LocalDateTime candleTime = LocalDateTime.parse(startTime, TIME_FORMATTER);

            MinuteCandle candle = MinuteCandle.builder()
                    .stockCode(stockCode)
                    .openPrice(Long.parseLong((String) data.get("open")))
                    .highPrice(Long.parseLong((String) data.get("high")))
                    .lowPrice(Long.parseLong((String) data.get("low")))
                    .closePrice(Long.parseLong((String) data.get("close")))
                    .volume(Long.parseLong((String) data.get("volume")))
                    .candleTime(candleTime)
                    .build();

            minuteCandleRepository.save(candle);
            redisTemplate.delete(key);
            log.info("1분봉 저장 완료: {} {}", stockCode, candleTime);
        } catch (Exception e) {
            log.error("1분봉 저장 실패: {}", stockCode, e);
        }
    }
}
