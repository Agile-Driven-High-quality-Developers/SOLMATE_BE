package org.solmate.domain.stock.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.solmate.domain.stock.dto.response.CandleResponse;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.MinuteCandleRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final MinuteCandleRepository minuteCandleRepository;
    private final DailyCandleRepository dailyCandleRepository;
    private final CandleAccumulatorService candleAccumulatorService;

    /**
     * 분봉 조회
     * unit=1  → DB minute_candle 직접 조회 + Redis 현재 1분봉
     * unit=5|30|60 → DB 1분봉 집계 + Redis 현재 N분봉
     */
    public List<CandleResponse> getMinuteCandles(String stockCode, int unit, int days) {
        int effectiveDays = days > 0 ? days : defaultDaysForUnit(unit);
        if (unit == 1) {
            return getOneMinuteCandles(stockCode, effectiveDays);
        }
        return getAggregatedMinuteCandles(stockCode, unit, effectiveDays);
    }

    // unit별 기본 조회 일수 (days=0으로 요청 시 적용)
    private int defaultDaysForUnit(int unit) {
        return switch (unit) {
            case 1  -> 5;   // 1분봉:  5 영업일  (~1,950개)
            case 5  -> 20;  // 5분봉:  20 영업일 (~1,560개)
            case 30 -> 60;  // 30분봉: 60 영업일 (~780개)
            case 60 -> 90;  // 60분봉: 90 영업일 (~630개)
            default -> 5;
        };
    }

    // unit=1: DB minute_candle 직접 조회 + Redis 현재 봉
    private List<CandleResponse> getOneMinuteCandles(String stockCode, int days) {
        LocalDateTime from = LocalDate.now(KST).minusDays(days - 1).atStartOfDay();
        LocalDateTime to   = LocalDate.now(KST).atTime(23, 59, 59);

        List<MinuteCandle> candles = minuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream()
                        .map(c -> CandleResponse.from(c, c.getCandleTime()))
                        .toList()
        );

        appendCurrentCandle(result, stockCode, "candle:1min:");
        return result;
    }

    // unit=5|30|60: DB 1분봉을 N분 단위로 집계 + Redis 현재 N분봉
    private List<CandleResponse> getAggregatedMinuteCandles(String stockCode, int unit, int days) {
        LocalDateTime from = LocalDate.now(KST).minusDays(days - 1).atStartOfDay();
        LocalDateTime to   = LocalDate.now(KST).atTime(23, 59, 59);

        List<MinuteCandle> oneMinCandles = minuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        // N분 단위 버킷으로 그룹핑 (버킷 시작 시각 기준)
        TreeMap<LocalDateTime, List<MinuteCandle>> buckets = new TreeMap<>();
        for (MinuteCandle c : oneMinCandles) {
            LocalDateTime bucketStart = toBucketStart(c.getCandleTime(), unit);
            buckets.computeIfAbsent(bucketStart, k -> new ArrayList<>()).add(c);
        }

        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry : buckets.entrySet()) {
            result.add(aggregateMinuteCandles(entry.getValue(), entry.getKey()));
        }

        String redisPrefix = "candle:" + unit + "min:";
        appendCurrentCandle(result, stockCode, redisPrefix);
        return result;
    }

    // 일봉 조회 (DB + 오늘 진행 중인 봉 포함)
    public List<CandleResponse> getDailyCandles(String stockCode, int days) {
        LocalDateTime from = LocalDate.now(KST).minusDays(days).atStartOfDay();
        LocalDateTime to   = LocalDate.now(KST).plusDays(1).atStartOfDay();

        List<DailyCandle> candles = dailyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream()
                        .map(c -> CandleResponse.from(c, c.getCandleTime()))
                        .toList()
        );

        appendCurrentCandle(result, stockCode, "candle:1day:");
        return result;
    }

    // 주봉 조회 (DB 일봉 집계 + 오늘 진행 중인 봉 포함)
    public List<CandleResponse> getWeeklyCandles(String stockCode, int weeks) {
        LocalDateTime from = LocalDate.now(KST).minusWeeks(weeks).atStartOfDay();
        LocalDateTime to   = LocalDate.now(KST).plusDays(1).atStartOfDay();
        return aggregateDailyCandles(stockCode, from, to, this::toWeekBucketStart);
    }

    // 월봉 조회 (DB 일봉 집계 + 오늘 진행 중인 봉 포함)
    public List<CandleResponse> getMonthlyCandles(String stockCode, int months) {
        LocalDateTime from = LocalDate.now(KST).minusMonths(months).atStartOfDay();
        LocalDateTime to   = LocalDate.now(KST).plusDays(1).atStartOfDay();
        return aggregateDailyCandles(stockCode, from, to, this::toMonthBucketStart);
    }

    // 년봉 조회 (DB 일봉 집계 + 오늘 진행 중인 봉 포함)
    public List<CandleResponse> getYearlyCandles(String stockCode, int years) {
        LocalDateTime from = LocalDate.now(KST).minusYears(years).atStartOfDay();
        LocalDateTime to   = LocalDate.now(KST).plusDays(1).atStartOfDay();
        return aggregateDailyCandles(stockCode, from, to, this::toYearBucketStart);
    }

    // DB 일봉을 주어진 버킷 함수로 그룹핑해 집계 + Redis 현재 일봉을 버킷에 merge
    private List<CandleResponse> aggregateDailyCandles(
            String stockCode,
            LocalDateTime from,
            LocalDateTime to,
            java.util.function.Function<LocalDate, LocalDate> bucketFn) {

        List<DailyCandle> dailyCandles = dailyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        TreeMap<LocalDate, List<DailyCandle>> buckets = new TreeMap<>();
        for (DailyCandle c : dailyCandles) {
            LocalDate bucketStart = bucketFn.apply(c.getCandleTime().toLocalDate());
            buckets.computeIfAbsent(bucketStart, k -> new ArrayList<>()).add(c);
        }

        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<DailyCandle>> entry : buckets.entrySet()) {
            result.add(aggregateDailyCandleList(entry.getValue(), entry.getKey().atStartOfDay()));
        }

        appendCurrentDayCandle(result, stockCode, bucketFn);
        return result;
    }

    // Redis 현재 일봉을 버킷 기준으로 변환해 마지막 봉에 merge (주/월/년봉용)
    private void appendCurrentDayCandle(
            List<CandleResponse> result,
            String stockCode,
            java.util.function.Function<LocalDate, LocalDate> bucketFn) {

        Map<Object, Object> current = candleAccumulatorService.getCurrentCandle(stockCode, "candle:1day:");
        if (current.isEmpty()) return;

        String startTime = (String) current.get("startTime");
        if (startTime == null || current.get("open") == null || current.get("close") == null
                || current.get("high") == null || current.get("low") == null || current.get("volume") == null) return;

        LocalDateTime candleTime = LocalDateTime.parse(startTime, TIME_FORMATTER);

        // Redis 봉의 날짜를 버킷 시작으로 변환 (월요일 / 1일 / 1월1일)
        LocalDate bucketDate = bucketFn.apply(candleTime.toLocalDate());
        LocalDateTime bucketStart = bucketDate.atStartOfDay();
        long bucketEpoch = bucketStart.toEpochSecond(ZoneOffset.ofHours(9));

        long redisOpen   = Long.parseLong((String) current.get("open"));
        long redisHigh   = Long.parseLong((String) current.get("high"));
        long redisLow    = Long.parseLong((String) current.get("low"));
        long redisClose  = Long.parseLong((String) current.get("close"));
        long redisVolume = Long.parseLong((String) current.get("volume"));

        // 같은 버킷이 이미 집계 결과에 있으면 close/high/low/volume 갱신
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).time() == bucketEpoch) {
                CandleResponse existing = result.get(i);
                result.set(i, new CandleResponse(
                        bucketEpoch,
                        existing.open(),
                        Math.max(existing.high(), redisHigh),
                        Math.min(existing.low(),  redisLow),
                        redisClose,
                        existing.volume() + redisVolume
                ));
                return;
            }
        }

        // 버킷이 없으면 새 봉으로 추가 (조회 범위 안에 오늘이 포함된 첫 거래일)
        result.add(new CandleResponse(bucketEpoch, redisOpen, redisHigh, redisLow, redisClose, redisVolume));
    }

    // 일봉 리스트를 하나의 집계 봉으로 변환
    private CandleResponse aggregateDailyCandleList(List<DailyCandle> candles, LocalDateTime bucketStart) {
        long open   = candles.get(0).getOpenPrice();
        long close  = candles.get(candles.size() - 1).getClosePrice();
        long high   = candles.stream().mapToLong(DailyCandle::getHighPrice).max().orElse(0);
        long low    = candles.stream().mapToLong(DailyCandle::getLowPrice).min().orElse(0);
        long volume = candles.stream().mapToLong(DailyCandle::getVolume).sum();
        return CandleResponse.ofAggregated(bucketStart, open, high, low, close, volume);
    }

    // 주봉 버킷: 해당 날짜가 속한 주의 월요일
    private LocalDate toWeekBucketStart(LocalDate date) {
        return date.with(java.time.DayOfWeek.MONDAY);
    }

    // 월봉 버킷: 해당 날짜가 속한 달의 1일
    private LocalDate toMonthBucketStart(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    // 년봉 버킷: 해당 날짜가 속한 해의 1월 1일
    private LocalDate toYearBucketStart(LocalDate date) {
        return date.withDayOfYear(1);
    }

    // 절대 분 단위 버킷 시작 시각 계산 (프리/에프터 마켓 포함)
    private LocalDateTime toBucketStart(LocalDateTime candleTime, int unit) {
        int minuteOfDay = candleTime.getHour() * 60 + candleTime.getMinute();
        int bucketMinute = Math.floorDiv(minuteOfDay, unit) * unit;
        return candleTime.toLocalDate().atTime(bucketMinute / 60, bucketMinute % 60);
    }

    // 1분봉 리스트를 하나의 N분봉으로 집계
    private CandleResponse aggregateMinuteCandles(List<MinuteCandle> candles, LocalDateTime bucketStart) {
        long open   = candles.get(0).getOpenPrice();
        long close  = candles.get(candles.size() - 1).getClosePrice();
        long high   = candles.stream().mapToLong(MinuteCandle::getHighPrice).max().orElse(0);
        long low    = candles.stream().mapToLong(MinuteCandle::getLowPrice).min().orElse(0);
        long volume = candles.stream().mapToLong(MinuteCandle::getVolume).sum();
        return CandleResponse.ofAggregated(bucketStart, open, high, low, close, volume);
    }

    // Redis에서 현재 진행 중인 봉을 꺼내 리스트 끝에 추가
    private void appendCurrentCandle(List<CandleResponse> result, String stockCode, String prefix) {
        Map<Object, Object> current = candleAccumulatorService.getCurrentCandle(stockCode, prefix);
        if (current.isEmpty()) return;

        String startTime = (String) current.get("startTime");
        if (startTime == null || current.get("open") == null || current.get("close") == null
                || current.get("high") == null || current.get("low") == null || current.get("volume") == null) return;

        LocalDateTime candleTime = LocalDateTime.parse(startTime, TIME_FORMATTER);
        long epochTime = candleTime.toEpochSecond(ZoneOffset.ofHours(9));

        // 이미 같은 시각의 봉이 집계 결과에 있으면 교체 (Redis 봉이 더 최신)
        result.removeIf(c -> c.time() == epochTime);
        result.add(CandleResponse.fromRedis(current, candleTime));
    }
}
