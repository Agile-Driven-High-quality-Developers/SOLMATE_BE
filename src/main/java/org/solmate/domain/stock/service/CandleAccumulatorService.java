package org.solmate.domain.stock.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.domain.stock.repository.DailyCandleRepository;
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

    private static final LocalTime MARKET_OPEN  = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private static final String KEY_1MIN  = "candle:1min:";
    private static final String KEY_5MIN  = "candle:5min:";
    private static final String KEY_30MIN = "candle:30min:";
    private static final String KEY_60MIN = "candle:60min:";
    private static final String KEY_1DAY  = "candle:1day:";

    private static final List<String> ALL_PREFIXES = List.of(
            KEY_1MIN, KEY_5MIN, KEY_30MIN, KEY_60MIN, KEY_1DAY
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final StringRedisTemplate redisTemplate;
    private final MinuteCandleRepository minuteCandleRepository;
    private final DailyCandleRepository dailyCandleRepository;

    // 체결 수신 시 정규장 여부 확인 후 모든 봉 Redis 키 동시 업데이트
    public void accumulate(LsWsStockResponse.Body body) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(MARKET_OPEN) || now.isAfter(MARKET_CLOSE)) return;

        String stockCode = body.shcode();
        long price   = Long.parseLong(body.price().trim());
        long cvolume = Long.parseLong(body.cvolume().trim());

        accumulateKey(KEY_1MIN  + stockCode, price, cvolume);
        accumulateKey(KEY_5MIN  + stockCode, price, cvolume);
        accumulateKey(KEY_30MIN + stockCode, price, cvolume);
        accumulateKey(KEY_60MIN + stockCode, price, cvolume);
        accumulateKey(KEY_1DAY  + stockCode, price, cvolume);
    }

    // Redis 키에 체결가/거래량 누적 (첫 체결이면 open 세팅, 이후엔 high/low/close/volume 갱신)
    private void accumulateKey(String key, long price, long cvolume) {
        Map<Object, Object> current = redisTemplate.opsForHash().entries(key);

        if (current.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, Map.of(
                    "open",      String.valueOf(price),
                    "high",      String.valueOf(price),
                    "low",       String.valueOf(price),
                    "close",     String.valueOf(price),
                    "volume",    String.valueOf(cvolume),
                    "startTime", LocalDateTime.now().format(TIME_FORMATTER)
            ));
        } else {
            long high   = Math.max(price, Long.parseLong((String) current.get("high")));
            long low    = Math.min(price, Long.parseLong((String) current.get("low")));
            long volume = Long.parseLong((String) current.get("volume")) + cvolume;

            redisTemplate.opsForHash().putAll(key, Map.of(
                    "high",   String.valueOf(high),
                    "low",    String.valueOf(low),
                    "close",  String.valueOf(price),
                    "volume", String.valueOf(volume)
            ));
        }
    }

    // 1분봉 DB 저장 (매 분 00초)
    public void flushMinuteCandle(String stockCode) {
        flushToMinuteDb(stockCode);
    }

    // 5분봉 Redis 초기화 (매 5분)
    public void flush5MinCandle(String stockCode) {
        redisTemplate.delete(KEY_5MIN + stockCode);
        log.info("5분봉 초기화: {}", stockCode);
    }

    // 30분봉 Redis 초기화 (매 30분)
    public void flush30MinCandle(String stockCode) {
        redisTemplate.delete(KEY_30MIN + stockCode);
        log.info("30분봉 초기화: {}", stockCode);
    }

    // 60분봉 Redis 초기화 (매 60분)
    public void flush60MinCandle(String stockCode) {
        redisTemplate.delete(KEY_60MIN + stockCode);
        log.info("60분봉 초기화: {}", stockCode);
    }

    // 일봉 DB 저장 (장 마감 15:30)
    public void flushDailyCandle(String stockCode) {
        String key = KEY_1DAY + stockCode;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) return;

        try {
            LocalDateTime candleTime = LocalDate.now().atStartOfDay();

            DailyCandle candle = DailyCandle.builder()
                    .stockCode(stockCode)
                    .openPrice(Long.parseLong((String) data.get("open")))
                    .highPrice(Long.parseLong((String) data.get("high")))
                    .lowPrice(Long.parseLong((String) data.get("low")))
                    .closePrice(Long.parseLong((String) data.get("close")))
                    .volume(Long.parseLong((String) data.get("volume")))
                    .candleTime(candleTime)
                    .build();

            dailyCandleRepository.save(candle);
            redisTemplate.delete(key);
            log.info("일봉 저장 완료: {}", stockCode);
        } catch (Exception e) {
            log.error("일봉 저장 실패: {}", stockCode, e);
        }
    }

    // 현재 진행 중인 봉 Redis 데이터 조회 (API 응답용)
    public Map<Object, Object> getCurrentCandle(String stockCode, String prefix) {
        return redisTemplate.opsForHash().entries(prefix + stockCode);
    }

    // 1분봉 Redis 데이터를 DB에 저장하고 키 삭제
    private void flushToMinuteDb(String stockCode) {
        String key = KEY_1MIN + stockCode;
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
            log.debug("1분봉 저장 완료: {} {}", stockCode, candleTime);
        } catch (Exception e) {
            log.error("1분봉 저장 실패: {}", stockCode, e);
        }
    }
}
