package org.solmate.domain.stock.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.FiveMinuteCandleRepository;
import org.solmate.domain.stock.repository.MinuteCandleRepository;
import org.solmate.domain.stock.repository.MonthlyCandleRepository;
import org.solmate.domain.stock.repository.ThirtyMinuteCandleRepository;
import org.solmate.domain.stock.repository.WeeklyCandleRepository;
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

    private static final String KEY_1MIN   = "candle:1min:";
    private static final String KEY_5MIN   = "candle:5min:";
    private static final String KEY_30MIN  = "candle:30min:";
    private static final String KEY_60MIN  = "candle:60min:";
    private static final String KEY_1DAY   = "candle:1day:";
    private static final String KEY_1WEEK  = "candle:1week:";
    private static final String KEY_1MONTH = "candle:1month:";

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private static final DefaultRedisScript<Void> ACCUMULATE_SCRIPT;
    static {
        ACCUMULATE_SCRIPT = new DefaultRedisScript<>();
        ACCUMULATE_SCRIPT.setLocation(new ClassPathResource("scripts/accumulate-candle.lua"));
        ACCUMULATE_SCRIPT.setResultType(Void.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final MinuteCandleRepository minuteCandleRepository;
    private final DailyCandleRepository dailyCandleRepository;
    private final FiveMinuteCandleRepository fiveMinuteCandleRepository;
    private final ThirtyMinuteCandleRepository thirtyMinuteCandleRepository;
    private final WeeklyCandleRepository weeklyCandleRepository;
    private final MonthlyCandleRepository monthlyCandleRepository;

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

        accumulateKey(KEY_1MIN   + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_5MIN   + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_30MIN  + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_60MIN  + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_1DAY   + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_1WEEK  + stockCode, price, cvolume, startTime);
        accumulateKey(KEY_1MONTH + stockCode, price, cvolume, startTime);
    }

    private void accumulateKey(String key, String price, String cvolume, String startTime) {
        redisTemplate.execute(ACCUMULATE_SCRIPT, List.of(key), price, cvolume, startTime);
    }

    // 1분봉: Redis → DB 저장
    public void flushMinuteCandle(String stockCode) {
        flushToDb(stockCode, KEY_1MIN, 1, (data, candleTime) ->
            minuteCandleRepository.insertIgnoreDuplicate(
                stockCode,
                Long.parseLong((String) data.get("open")),
                Long.parseLong((String) data.get("high")),
                Long.parseLong((String) data.get("low")),
                Long.parseLong((String) data.get("close")),
                Long.parseLong((String) data.get("volume")),
                candleTime),
            "1분봉");
    }

    // 5분봉: Redis → DB 저장 (startTime을 5분 버킷 시작으로 내림 ex. 09:03 → 09:00)
    public void flush5MinCandle(String stockCode) {
        flushToDb(stockCode, KEY_5MIN, 5, (data, candleTime) ->
            fiveMinuteCandleRepository.insertIgnoreDuplicate(
                stockCode,
                Long.parseLong((String) data.get("open")),
                Long.parseLong((String) data.get("high")),
                Long.parseLong((String) data.get("low")),
                Long.parseLong((String) data.get("close")),
                Long.parseLong((String) data.get("volume")),
                candleTime),
            "5분봉");
    }

    // 30분봉: Redis → DB 저장 (startTime을 30분 버킷 시작으로 내림 ex. 09:17 → 09:00)
    public void flush30MinCandle(String stockCode) {
        flushToDb(stockCode, KEY_30MIN, 30, (data, candleTime) ->
            thirtyMinuteCandleRepository.insertIgnoreDuplicate(
                stockCode,
                Long.parseLong((String) data.get("open")),
                Long.parseLong((String) data.get("high")),
                Long.parseLong((String) data.get("low")),
                Long.parseLong((String) data.get("close")),
                Long.parseLong((String) data.get("volume")),
                candleTime),
            "30분봉");
    }

    // 60분봉: Redis 초기화만 (전용 테이블 없음, 30분봉 2개 집계로 조회)
    public void flush60MinCandle(String stockCode) {
        redisTemplate.delete(KEY_60MIN + stockCode);
    }

    // 일봉: Redis → DB 저장
    public void flushDailyCandle(String stockCode) {
        LocalDateTime candleTime = LocalDate.now(KST).atStartOfDay();
        flushToDbFixed(stockCode, KEY_1DAY, candleTime, (data, ct) ->
            dailyCandleRepository.insertIgnoreDuplicate(
                stockCode,
                Long.parseLong((String) data.get("open")),
                Long.parseLong((String) data.get("high")),
                Long.parseLong((String) data.get("low")),
                Long.parseLong((String) data.get("close")),
                Long.parseLong((String) data.get("volume")),
                ct),
            "일봉");
    }

    // 주봉: Redis → DB 저장 (이번 주 월요일 기준 시각으로 저장)
    public void flushWeeklyCandle(String stockCode) {
        LocalDateTime candleTime = LocalDate.now(KST).with(DayOfWeek.MONDAY).atStartOfDay();
        flushToDbFixed(stockCode, KEY_1WEEK, candleTime, (data, ct) ->
            weeklyCandleRepository.insertIgnoreDuplicate(
                stockCode,
                Long.parseLong((String) data.get("open")),
                Long.parseLong((String) data.get("high")),
                Long.parseLong((String) data.get("low")),
                Long.parseLong((String) data.get("close")),
                Long.parseLong((String) data.get("volume")),
                ct),
            "주봉");
    }

    // 월봉: Redis → DB 저장 (이번 달 1일 기준 시각으로 저장)
    public void flushMonthlyCandle(String stockCode) {
        LocalDateTime candleTime = LocalDate.now(KST).withDayOfMonth(1).atStartOfDay();
        flushToDbFixed(stockCode, KEY_1MONTH, candleTime, (data, ct) ->
            monthlyCandleRepository.insertIgnoreDuplicate(
                stockCode,
                Long.parseLong((String) data.get("open")),
                Long.parseLong((String) data.get("high")),
                Long.parseLong((String) data.get("low")),
                Long.parseLong((String) data.get("close")),
                Long.parseLong((String) data.get("volume")),
                ct),
            "월봉");
    }

    // startTime을 Redis에서 파싱 후 bucketUnit 분 단위로 내림해서 candleTime으로 사용
    // bucketUnit=1: 1분봉(그대로), bucketUnit=5: 5분봉, bucketUnit=30: 30분봉
    private void flushToDb(String stockCode, String keyPrefix, int bucketUnit, CandleSaveAction action, String label) {
        String key     = keyPrefix + stockCode;
        String snapKey = key + ":snap";

        try {
            redisTemplate.rename(key, snapKey);
        } catch (Exception e) {
            return;
        }

        Map<Object, Object> data = redisTemplate.opsForHash().entries(snapKey);
        if (data.isEmpty() || !isValidCandleData(data)) {
            log.warn("{} 불완전 데이터 삭제: {}", label, stockCode);
            redisTemplate.delete(snapKey);
            return;
        }

        try {
            String startTime = (String) data.get("startTime");
            LocalDateTime rawTime = (startTime != null)
                    ? LocalDateTime.parse(startTime, TIME_FORMATTER)
                    : LocalDateTime.now(KST).withSecond(0).withNano(0);

            // 버킷 시작 시각으로 내림 (ex. 09:03 → 09:00 for bucketUnit=5)
            int flooredMinute = (rawTime.getMinute() / bucketUnit) * bucketUnit;
            LocalDateTime candleTime = rawTime.toLocalDate().atTime(rawTime.getHour(), flooredMinute);

            action.save(data, candleTime);
            log.debug("{} 저장 완료: {} {}", label, stockCode, candleTime);
        } catch (Exception e) {
            log.error("{} 저장 실패: {}", label, stockCode, e);
        } finally {
            redisTemplate.delete(snapKey);
        }
    }

    // candleTime이 고정된 경우 (일봉/주봉/월봉 - 버킷 시작 시각)
    private void flushToDbFixed(String stockCode, String keyPrefix, LocalDateTime candleTime,
                                 CandleSaveAction action, String label) {
        String key     = keyPrefix + stockCode;
        String snapKey = key + ":snap";

        try {
            redisTemplate.rename(key, snapKey);
        } catch (Exception e) {
            return;
        }

        Map<Object, Object> data = redisTemplate.opsForHash().entries(snapKey);
        if (data.isEmpty() || !isValidCandleData(data)) {
            log.warn("{} 불완전 데이터 삭제: {}", label, stockCode);
            redisTemplate.delete(snapKey);
            return;
        }

        try {
            action.save(data, candleTime);
            log.info("{} 저장 완료: {}", label, stockCode);
        } catch (Exception e) {
            log.error("{} 저장 실패: {}", label, stockCode, e);
        } finally {
            redisTemplate.delete(snapKey);
        }
    }

    public Map<Object, Object> getCurrentCandle(String stockCode, String prefix) {
        return redisTemplate.opsForHash().entries(prefix + stockCode);
    }

    private boolean isValidCandleData(Map<Object, Object> data) {
        return data.get("open") != null
                && data.get("high") != null
                && data.get("low") != null
                && data.get("close") != null
                && data.get("volume") != null;
    }

    @FunctionalInterface
    private interface CandleSaveAction {
        void save(Map<Object, Object> data, LocalDateTime candleTime);
    }
}
