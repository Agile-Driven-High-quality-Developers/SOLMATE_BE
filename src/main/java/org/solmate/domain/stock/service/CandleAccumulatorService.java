package org.solmate.domain.stock.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.MinuteCandleRepository;
import org.solmate.external.ls.dto.websocket.LsWsStockResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleAccumulatorService {

    private static final LocalTime PRE_MARKET_OPEN    = LocalTime.of(8, 0);
    private static final LocalTime PRE_MARKET_CLOSE   = LocalTime.of(9, 0);
    private static final LocalTime MARKET_OPEN        = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE       = LocalTime.of(15, 40);
    private static final LocalTime AFTER_MARKET_OPEN  = LocalTime.of(15, 40);
    private static final LocalTime AFTER_MARKET_CLOSE = LocalTime.of(20, 0);

    private static final String KEY_1MIN  = "candle:1min:";
    private static final String KEY_5MIN  = "candle:5min:";
    private static final String KEY_30MIN = "candle:30min:";
    private static final String KEY_60MIN = "candle:60min:";
    private static final String KEY_1DAY  = "candle:1day:";

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    // Lua 스크립트: OHLCV 원자적 누적
    private static final DefaultRedisScript<Void> ACCUMULATE_SCRIPT;
    static {
        ACCUMULATE_SCRIPT = new DefaultRedisScript<>();
        ACCUMULATE_SCRIPT.setLocation(new ClassPathResource("scripts/accumulate-candle.lua"));
        ACCUMULATE_SCRIPT.setResultType(Void.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final MinuteCandleRepository minuteCandleRepository;
    private final DailyCandleRepository dailyCandleRepository;

    // 장 시간 내 체결 수신 시 모든 주기 Redis 키에 OHLCV 누적
    public void accumulate(LsWsStockResponse.Body body) {
        LocalTime now = LocalTime.now(KST);

        boolean isPreMarket     = !now.isBefore(PRE_MARKET_OPEN)   && !now.isAfter(PRE_MARKET_CLOSE);
        boolean isRegularMarket = !now.isBefore(MARKET_OPEN)       && !now.isAfter(MARKET_CLOSE);
        boolean isAfterMarket   = !now.isBefore(AFTER_MARKET_OPEN) && !now.isAfter(AFTER_MARKET_CLOSE);

        if (!isPreMarket && !isRegularMarket && !isAfterMarket) return;

        String stockCode = body.shcode();
        String price     = body.price().trim();
        String cvolume   = body.cvolume().trim();
        String startTime = LocalDateTime.now(KST).format(TIME_FORMATTER);

        accumulateKey(KEY_1MIN  + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_5MIN  + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_30MIN + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_60MIN + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_1DAY  + stockCode, price, cvolume, startTime);
    }

    private void accumulateKey(String key, String price, String cvolume, String startTime) {
        redisTemplate.execute(ACCUMULATE_SCRIPT, List.of(key), price, cvolume, startTime);
    }

    public void flushMinuteCandle(String stockCode) {
        flushToMinuteDb(stockCode);
    }

    public void flush5MinCandle(String stockCode) {
        redisTemplate.delete(KEY_5MIN + stockCode);
    }

    public void flush30MinCandle(String stockCode) {
        redisTemplate.delete(KEY_30MIN + stockCode);
    }

    public void flush60MinCandle(String stockCode) {
        redisTemplate.delete(KEY_60MIN + stockCode);
    }

    public void flushDailyCandle(String stockCode) {
        String key     = KEY_1DAY + stockCode;
        String snapKey = key + ":snap";

        try {
            redisTemplate.rename(key, snapKey);
        } catch (Exception e) {
            return;
        }

        Map<Object, Object> data = redisTemplate.opsForHash().entries(snapKey);
        if (data.isEmpty() || !isValidCandleData(data)) {
            log.warn("일봉 불완전 데이터 삭제: {}", stockCode);
            redisTemplate.delete(snapKey);
            return;
        }

        try {
            LocalDateTime candleTime = LocalDate.now(KST).atStartOfDay();

            dailyCandleRepository.insertIgnoreDuplicate(
                    stockCode,
                    Long.parseLong((String) data.get("open")),
                    Long.parseLong((String) data.get("high")),
                    Long.parseLong((String) data.get("low")),
                    Long.parseLong((String) data.get("close")),
                    Long.parseLong((String) data.get("volume")),
                    candleTime);
            log.info("일봉 저장 완료: {}", stockCode);
        } catch (Exception e) {
            log.error("일봉 저장 실패: {}", stockCode, e);
        } finally {
            redisTemplate.delete(snapKey);
        }
    }

    public Map<Object, Object> getCurrentCandle(String stockCode, String prefix) {
        return redisTemplate.opsForHash().entries(prefix + stockCode);
    }

    // RENAME으로 스냅샷 이동 후 처리 (flush 중 새 체결 유실 방지)
    private void flushToMinuteDb(String stockCode) {
        String key     = KEY_1MIN + stockCode;
        String snapKey = key + ":snap";

        try {
            redisTemplate.rename(key, snapKey); // 원자적 이동, key 없으면 예외
        } catch (Exception e) {
            return; // 쌓인 데이터 없음
        }

        Map<Object, Object> data = redisTemplate.opsForHash().entries(snapKey);
        if (data.isEmpty() || !isValidCandleData(data)) {
            log.warn("1분봉 불완전 데이터 삭제: {}", stockCode);
            redisTemplate.delete(snapKey);
            return;
        }

        try {
            String startTime = (String) data.get("startTime");
            LocalDateTime candleTime = (startTime != null)
                    ? LocalDateTime.parse(startTime, TIME_FORMATTER)
                    : LocalDateTime.now(KST).withSecond(0).withNano(0);

            minuteCandleRepository.insertIgnoreDuplicate(
                    stockCode,
                    Long.parseLong((String) data.get("open")),
                    Long.parseLong((String) data.get("high")),
                    Long.parseLong((String) data.get("low")),
                    Long.parseLong((String) data.get("close")),
                    Long.parseLong((String) data.get("volume")),
                    candleTime);
            log.debug("1분봉 저장 완료: {} {}", stockCode, candleTime);
        } catch (Exception e) {
            log.error("1분봉 저장 실패: {}", stockCode, e);
        } finally {
            redisTemplate.delete(snapKey);
        }
    }

    private boolean isValidCandleData(Map<Object, Object> data) {
        return data.get("open") != null
                && data.get("high") != null
                && data.get("low") != null
                && data.get("close") != null
                && data.get("volume") != null;
    }
}
