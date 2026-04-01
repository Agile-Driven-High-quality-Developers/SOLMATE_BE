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
import java.util.function.Function;

import org.solmate.domain.stock.dto.response.CandleResponse;
import org.solmate.domain.stock.entity.FiveMinuteCandle;
import org.solmate.domain.stock.entity.MonthlyCandle;
import org.solmate.domain.stock.entity.ThirtyMinuteCandle;
import org.solmate.domain.stock.entity.WeeklyCandle;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.FiveMinuteCandleRepository;
import org.solmate.domain.stock.repository.MinuteCandleRepository;
import org.solmate.domain.stock.repository.MonthlyCandleRepository;
import org.solmate.domain.stock.repository.ThirtyMinuteCandleRepository;
import org.solmate.domain.stock.repository.WeeklyCandleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final MinuteCandleRepository minuteCandleRepository;
    private final DailyCandleRepository dailyCandleRepository;
    private final FiveMinuteCandleRepository fiveMinuteCandleRepository;
    private final ThirtyMinuteCandleRepository thirtyMinuteCandleRepository;
    private final WeeklyCandleRepository weeklyCandleRepository;
    private final MonthlyCandleRepository monthlyCandleRepository;
    private final CandleAccumulatorService candleAccumulatorService;

    /**
     * 분봉 조회
     * unit=1  → minute_candle DB 직접 조회 + Redis 현재 봉
     * unit=5  → five_minute_candle DB 직접 조회 + Redis 현재 봉
     * unit=30 → thirty_minute_candle DB 직접 조회 + Redis 현재 봉
     * unit=60 → thirty_minute_candle 2개 집계 + Redis 현재 봉
     * to 지정 시 해당 Unix epoch(초) 이전 데이터 조회 (TradingView 무한스크롤)
     */
    @Transactional(readOnly = true)
    public List<CandleResponse> getMinuteCandles(String stockCode, int unit, int days, Long to) {
        int effectiveDays = days > 0 ? days : defaultDaysForUnit(unit);
        return switch (unit) {
            case 1  -> getOneMinuteCandles(stockCode, effectiveDays, to);
            case 5  -> getFiveMinuteCandles(stockCode, effectiveDays, to);
            case 30 -> getThirtyMinuteCandles(stockCode, effectiveDays, to);
            case 60 -> getSixtyMinuteCandles(stockCode, effectiveDays, to);
            default -> getOneMinuteCandles(stockCode, effectiveDays, to);
        };
    }

    private int defaultDaysForUnit(int unit) {
        return switch (unit) {
            case 1  -> 5;
            case 5  -> 20;
            case 30 -> 60;
            case 60 -> 90;
            default -> 5;
        };
    }

    // 1분봉: minute_candle 직접 조회 + Redis 현재 봉
    private List<CandleResponse> getOneMinuteCandles(String stockCode, int days, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).atTime(23, 59, 59));
        LocalDateTime from = toDateTime.minusDays(days - 1).toLocalDate().atStartOfDay();

        List<MinuteCandle> candles = minuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList()
        );

        if (toEpoch == null) appendCurrentCandle(result, stockCode, "candle:1min:", 1);
        return result;
    }

    // 5분봉: five_minute_candle 직접 조회 + Redis 현재 봉
    private List<CandleResponse> getFiveMinuteCandles(String stockCode, int days, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).atTime(23, 59, 59));
        LocalDateTime from = toDateTime.minusDays(days - 1).toLocalDate().atStartOfDay();

        List<FiveMinuteCandle> candles = fiveMinuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList()
        );

        if (toEpoch == null) appendCurrentCandle(result, stockCode, "candle:5min:", 5);
        return result;
    }

    // 30분봉: thirty_minute_candle 직접 조회 + Redis 현재 봉
    private List<CandleResponse> getThirtyMinuteCandles(String stockCode, int days, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).atTime(23, 59, 59));
        LocalDateTime from = toDateTime.minusDays(days - 1).toLocalDate().atStartOfDay();

        List<ThirtyMinuteCandle> candles = thirtyMinuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList()
        );

        if (toEpoch == null) appendCurrentCandle(result, stockCode, "candle:30min:", 30);
        return result;
    }

    // 60분봉: thirty_minute_candle 2개씩 집계 + Redis 현재 봉
    private List<CandleResponse> getSixtyMinuteCandles(String stockCode, int days, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).atTime(23, 59, 59));
        LocalDateTime from = toDateTime.minusDays(days - 1).toLocalDate().atStartOfDay();

        List<ThirtyMinuteCandle> candles = thirtyMinuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        TreeMap<LocalDateTime, List<ThirtyMinuteCandle>> buckets = new TreeMap<>();
        for (ThirtyMinuteCandle c : candles) {
            LocalDateTime bucketStart = toBucketStart(c.getCandleTime(), 60);
            buckets.computeIfAbsent(bucketStart, k -> new ArrayList<>()).add(c);
        }

        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<ThirtyMinuteCandle>> entry : buckets.entrySet()) {
            result.add(aggregateThirtyMinuteCandles(entry.getValue(), entry.getKey()));
        }

        if (toEpoch == null) appendCurrentCandle(result, stockCode, "candle:60min:", 60);
        return result;
    }

    // 일봉: daily_candle 직접 조회 + Redis 현재 봉
    @Transactional(readOnly = true)
    public List<CandleResponse> getDailyCandles(String stockCode, int days, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).plusDays(1).atStartOfDay());
        LocalDateTime from = toDateTime.minusDays(days);

        List<DailyCandle> candles = dailyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList()
        );

        if (toEpoch == null) appendCurrentCandle(result, stockCode, "candle:1day:", 1);
        return result;
    }

    // 주봉: weekly_candle 직접 조회 + Redis 현재 봉
    @Transactional(readOnly = true)
    public List<CandleResponse> getWeeklyCandles(String stockCode, int weeks, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).plusDays(1).atStartOfDay());
        LocalDateTime from = toDateTime.minusWeeks(weeks);

        List<WeeklyCandle> candles = weeklyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList()
        );

        if (toEpoch == null) appendCurrentPeriodCandle(result, stockCode, "candle:1week:", this::toWeekBucketStart);
        return result;
    }

    // 월봉: monthly_candle 직접 조회 + Redis 현재 봉
    @Transactional(readOnly = true)
    public List<CandleResponse> getMonthlyCandles(String stockCode, int months, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).plusDays(1).atStartOfDay());
        LocalDateTime from = toDateTime.minusMonths(months);

        List<MonthlyCandle> candles = monthlyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList()
        );

        if (toEpoch == null) appendCurrentPeriodCandle(result, stockCode, "candle:1month:", this::toMonthBucketStart);
        return result;
    }

    // 5분봉 벤치마크: five_minute_candle 직접 조회
    @Transactional(readOnly = true)
    public List<CandleResponse> getFiveMinFromTable(String stockCode, int days) {
        LocalDateTime to = LocalDate.now(KST).atTime(23, 59, 59);
        LocalDateTime from = to.minusDays(days - 1).toLocalDate().atStartOfDay();
        List<FiveMinuteCandle> candles = fiveMinuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        return candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList();
    }

    // 5분봉 벤치마크: minute_candle 집계
    @Transactional(readOnly = true)
    public List<CandleResponse> getFiveMinFromAggregate(String stockCode, int days) {
        LocalDateTime to = LocalDate.now(KST).atTime(23, 59, 59);
        LocalDateTime from = to.minusDays(days - 1).toLocalDate().atStartOfDay();
        List<MinuteCandle> candles = minuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        TreeMap<LocalDateTime, List<MinuteCandle>> buckets = new TreeMap<>();
        for (MinuteCandle c : candles) {
            buckets.computeIfAbsent(toBucketStart(c.getCandleTime(), 5), k -> new ArrayList<>()).add(c);
        }
        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry : buckets.entrySet()) {
            List<MinuteCandle> bucket = entry.getValue();
            result.add(CandleResponse.ofAggregated(entry.getKey(),
                    bucket.get(0).getOpenPrice(),
                    bucket.stream().mapToLong(MinuteCandle::getHighPrice).max().orElse(0),
                    bucket.stream().mapToLong(MinuteCandle::getLowPrice).min().orElse(0),
                    bucket.get(bucket.size() - 1).getClosePrice(),
                    bucket.stream().mapToLong(MinuteCandle::getVolume).sum()));
        }
        return result;
    }

    // 60분봉 벤치마크: thirty_minute_candle 집계 (table 방식)
    @Transactional(readOnly = true)
    public List<CandleResponse> getSixtyMinFromTable(String stockCode, int days) {
        LocalDateTime to = LocalDate.now(KST).atTime(23, 59, 59);
        LocalDateTime from = to.minusDays(days - 1).toLocalDate().atStartOfDay();
        List<ThirtyMinuteCandle> candles = thirtyMinuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        TreeMap<LocalDateTime, List<ThirtyMinuteCandle>> buckets = new TreeMap<>();
        for (ThirtyMinuteCandle c : candles) {
            buckets.computeIfAbsent(toBucketStart(c.getCandleTime(), 60), k -> new ArrayList<>()).add(c);
        }
        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<ThirtyMinuteCandle>> entry : buckets.entrySet()) {
            result.add(aggregateThirtyMinuteCandles(entry.getValue(), entry.getKey()));
        }
        return result;
    }

    // 60분봉 벤치마크: minute_candle 집계
    @Transactional(readOnly = true)
    public List<CandleResponse> getSixtyMinFromAggregate(String stockCode, int days) {
        LocalDateTime to = LocalDate.now(KST).atTime(23, 59, 59);
        LocalDateTime from = to.minusDays(days - 1).toLocalDate().atStartOfDay();
        List<MinuteCandle> candles = minuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        TreeMap<LocalDateTime, List<MinuteCandle>> buckets = new TreeMap<>();
        for (MinuteCandle c : candles) {
            buckets.computeIfAbsent(toBucketStart(c.getCandleTime(), 60), k -> new ArrayList<>()).add(c);
        }
        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry : buckets.entrySet()) {
            List<MinuteCandle> bucket = entry.getValue();
            result.add(CandleResponse.ofAggregated(entry.getKey(),
                    bucket.get(0).getOpenPrice(),
                    bucket.stream().mapToLong(MinuteCandle::getHighPrice).max().orElse(0),
                    bucket.stream().mapToLong(MinuteCandle::getLowPrice).min().orElse(0),
                    bucket.get(bucket.size() - 1).getClosePrice(),
                    bucket.stream().mapToLong(MinuteCandle::getVolume).sum()));
        }
        return result;
    }

    // 주봉 벤치마크: weekly_candle 직접 조회
    @Transactional(readOnly = true)
    public List<CandleResponse> getWeeklyFromTable(String stockCode, int weeks) {
        LocalDateTime to = LocalDate.now(KST).plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusWeeks(weeks);
        List<WeeklyCandle> candles = weeklyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        return candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList();
    }

    // 주봉 벤치마크: daily_candle 집계
    @Transactional(readOnly = true)
    public List<CandleResponse> getWeeklyFromAggregate(String stockCode, int weeks) {
        LocalDateTime to = LocalDate.now(KST).plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusWeeks(weeks);
        List<DailyCandle> candles = dailyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        TreeMap<LocalDate, List<DailyCandle>> buckets = new TreeMap<>();
        for (DailyCandle c : candles) {
            LocalDate weekStart = c.getCandleTime().toLocalDate().with(java.time.DayOfWeek.MONDAY);
            buckets.computeIfAbsent(weekStart, k -> new ArrayList<>()).add(c);
        }
        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<DailyCandle>> entry : buckets.entrySet()) {
            List<DailyCandle> bucket = entry.getValue();
            long open   = bucket.get(0).getOpenPrice();
            long close  = bucket.get(bucket.size() - 1).getClosePrice();
            long high   = bucket.stream().mapToLong(DailyCandle::getHighPrice).max().orElse(0);
            long low    = bucket.stream().mapToLong(DailyCandle::getLowPrice).min().orElse(0);
            long volume = bucket.stream().mapToLong(DailyCandle::getVolume).sum();
            result.add(CandleResponse.ofAggregated(entry.getKey().atStartOfDay(), open, high, low, close, volume));
        }
        return result;
    }

    // 월봉 벤치마크: monthly_candle 직접 조회
    @Transactional(readOnly = true)
    public List<CandleResponse> getMonthlyFromTable(String stockCode, int months) {
        LocalDateTime to = LocalDate.now(KST).plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusMonths(months);
        List<MonthlyCandle> candles = monthlyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        return candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList();
    }

    // 월봉 벤치마크: daily_candle 집계
    @Transactional(readOnly = true)
    public List<CandleResponse> getMonthlyFromAggregate(String stockCode, int months) {
        LocalDateTime to = LocalDate.now(KST).plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusMonths(months);
        List<DailyCandle> candles = dailyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        TreeMap<LocalDate, List<DailyCandle>> buckets = new TreeMap<>();
        for (DailyCandle c : candles) {
            LocalDate monthStart = c.getCandleTime().toLocalDate().withDayOfMonth(1);
            buckets.computeIfAbsent(monthStart, k -> new ArrayList<>()).add(c);
        }
        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<DailyCandle>> entry : buckets.entrySet()) {
            List<DailyCandle> bucket = entry.getValue();
            long open   = bucket.get(0).getOpenPrice();
            long close  = bucket.get(bucket.size() - 1).getClosePrice();
            long high   = bucket.stream().mapToLong(DailyCandle::getHighPrice).max().orElse(0);
            long low    = bucket.stream().mapToLong(DailyCandle::getLowPrice).min().orElse(0);
            long volume = bucket.stream().mapToLong(DailyCandle::getVolume).sum();
            result.add(CandleResponse.ofAggregated(entry.getKey().atStartOfDay(), open, high, low, close, volume));
        }
        return result;
    }

    // 년봉 벤치마크: monthly_candle 집계 (table 방식)
    @Transactional(readOnly = true)
    public List<CandleResponse> getYearlyFromTable(String stockCode, int years) {
        LocalDateTime to = LocalDate.now(KST).plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusYears(years);
        List<MonthlyCandle> candles = monthlyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        TreeMap<LocalDate, List<MonthlyCandle>> buckets = new TreeMap<>();
        for (MonthlyCandle c : candles) {
            LocalDate yearStart = c.getCandleTime().toLocalDate().withDayOfYear(1);
            buckets.computeIfAbsent(yearStart, k -> new ArrayList<>()).add(c);
        }
        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<MonthlyCandle>> entry : buckets.entrySet()) {
            result.add(aggregateMonthlyCandles(entry.getValue(), entry.getKey().atStartOfDay()));
        }
        return result;
    }

    // 년봉 벤치마크: daily_candle 집계
    @Transactional(readOnly = true)
    public List<CandleResponse> getYearlyFromAggregate(String stockCode, int years) {
        LocalDateTime to = LocalDate.now(KST).plusDays(1).atStartOfDay();
        LocalDateTime from = to.minusYears(years);
        List<DailyCandle> candles = dailyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);
        TreeMap<LocalDate, List<DailyCandle>> buckets = new TreeMap<>();
        for (DailyCandle c : candles) {
            LocalDate yearStart = c.getCandleTime().toLocalDate().withDayOfYear(1);
            buckets.computeIfAbsent(yearStart, k -> new ArrayList<>()).add(c);
        }
        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<DailyCandle>> entry : buckets.entrySet()) {
            List<DailyCandle> bucket = entry.getValue();
            long open   = bucket.get(0).getOpenPrice();
            long close  = bucket.get(bucket.size() - 1).getClosePrice();
            long high   = bucket.stream().mapToLong(DailyCandle::getHighPrice).max().orElse(0);
            long low    = bucket.stream().mapToLong(DailyCandle::getLowPrice).min().orElse(0);
            long volume = bucket.stream().mapToLong(DailyCandle::getVolume).sum();
            result.add(CandleResponse.ofAggregated(entry.getKey().atStartOfDay(), open, high, low, close, volume));
        }
        return result;
    }

    // 30분봉 벤치마크: thirty_minute_candle 직접 조회
    @Transactional(readOnly = true)
    public List<CandleResponse> getThirtyMinFromTable(String stockCode, int days) {
        LocalDateTime to = LocalDate.now(KST).atTime(23, 59, 59);
        LocalDateTime from = to.minusDays(days - 1).toLocalDate().atStartOfDay();

        List<ThirtyMinuteCandle> candles = thirtyMinuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        return candles.stream().map(c -> CandleResponse.from(c, c.getCandleTime())).toList();
    }

    // 30분봉 벤치마크: minute_candle 집계
    @Transactional(readOnly = true)
    public List<CandleResponse> getThirtyMinFromAggregate(String stockCode, int days) {
        LocalDateTime to = LocalDate.now(KST).atTime(23, 59, 59);
        LocalDateTime from = to.minusDays(days - 1).toLocalDate().atStartOfDay();

        List<MinuteCandle> candles = minuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        TreeMap<LocalDateTime, List<MinuteCandle>> buckets = new TreeMap<>();
        for (MinuteCandle c : candles) {
            LocalDateTime bucketStart = toBucketStart(c.getCandleTime(), 30);
            buckets.computeIfAbsent(bucketStart, k -> new ArrayList<>()).add(c);
        }

        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<MinuteCandle>> entry : buckets.entrySet()) {
            List<MinuteCandle> bucket = entry.getValue();
            long open   = bucket.get(0).getOpenPrice();
            long close  = bucket.get(bucket.size() - 1).getClosePrice();
            long high   = bucket.stream().mapToLong(MinuteCandle::getHighPrice).max().orElse(0);
            long low    = bucket.stream().mapToLong(MinuteCandle::getLowPrice).min().orElse(0);
            long volume = bucket.stream().mapToLong(MinuteCandle::getVolume).sum();
            result.add(CandleResponse.ofAggregated(entry.getKey(), open, high, low, close, volume));
        }

        return result;
    }

    // 년봉: monthly_candle 12개씩 집계 + Redis 현재 월봉을 연 버킷에 merge
    @Transactional(readOnly = true)
    public List<CandleResponse> getYearlyCandles(String stockCode, int years, Long toEpoch) {
        LocalDateTime toDateTime = resolveToDateTime(toEpoch, LocalDate.now(KST).plusDays(1).atStartOfDay());
        LocalDateTime from = toDateTime.minusYears(years);

        List<MonthlyCandle> candles = monthlyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, toDateTime);

        TreeMap<LocalDate, List<MonthlyCandle>> buckets = new TreeMap<>();
        for (MonthlyCandle c : candles) {
            LocalDate yearStart = c.getCandleTime().toLocalDate().withDayOfYear(1);
            buckets.computeIfAbsent(yearStart, k -> new ArrayList<>()).add(c);
        }

        List<CandleResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<MonthlyCandle>> entry : buckets.entrySet()) {
            result.add(aggregateMonthlyCandles(entry.getValue(), entry.getKey().atStartOfDay()));
        }

        if (toEpoch == null) appendCurrentPeriodCandle(result, stockCode, "candle:1month:", this::toYearBucketStart);
        return result;
    }

    // to epoch(초)가 있으면 해당 LocalDateTime으로 변환, 없으면 기본값 사용
    private LocalDateTime resolveToDateTime(Long toEpoch, LocalDateTime defaultValue) {
        if (toEpoch == null) return defaultValue;
        return LocalDateTime.ofEpochSecond(toEpoch, 0, ZoneOffset.ofHours(9));
    }

    // Redis에서 현재 진행 중인 봉을 꺼내 리스트 끝에 추가 (1분/5분/30분/60분/일봉용)
    private void appendCurrentCandle(List<CandleResponse> result, String stockCode, String prefix, int bucketUnit) {
        Map<Object, Object> current = candleAccumulatorService.getCurrentCandle(stockCode, prefix);
        if (current.isEmpty()) return;

        String startTime = (String) current.get("startTime");
        if (startTime == null || current.get("open") == null || current.get("close") == null
                || current.get("high") == null || current.get("low") == null || current.get("volume") == null) return;

        LocalDateTime rawTime = LocalDateTime.parse(startTime, TIME_FORMATTER);
        int flooredMinute = (rawTime.getMinute() / bucketUnit) * bucketUnit;
        LocalDateTime candleTime = rawTime.toLocalDate().atTime(rawTime.getHour(), flooredMinute);
        long epochTime = candleTime.toEpochSecond(ZoneOffset.ofHours(9));

        result.removeIf(c -> c.time() == epochTime);
        result.add(CandleResponse.fromRedis(current, candleTime));
    }

    // Redis 봉을 버킷 함수로 변환해 주봉/월봉/년봉 마지막 봉에 merge
    private void appendCurrentPeriodCandle(
            List<CandleResponse> result,
            String stockCode,
            String redisPrefix,
            Function<LocalDate, LocalDate> bucketFn) {

        Map<Object, Object> current = candleAccumulatorService.getCurrentCandle(stockCode, redisPrefix);
        if (current.isEmpty()) return;

        String startTime = (String) current.get("startTime");
        if (startTime == null || current.get("open") == null || current.get("close") == null
                || current.get("high") == null || current.get("low") == null || current.get("volume") == null) return;

        LocalDateTime candleTime = LocalDateTime.parse(startTime, TIME_FORMATTER);
        LocalDate bucketDate = bucketFn.apply(candleTime.toLocalDate());
        LocalDateTime bucketStart = bucketDate.atStartOfDay();
        long bucketEpoch = bucketStart.toEpochSecond(ZoneOffset.ofHours(9));

        long redisOpen   = Long.parseLong((String) current.get("open"));
        long redisHigh   = Long.parseLong((String) current.get("high"));
        long redisLow    = Long.parseLong((String) current.get("low"));
        long redisClose  = Long.parseLong((String) current.get("close"));
        long redisVolume = Long.parseLong((String) current.get("volume"));

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

        result.add(new CandleResponse(bucketEpoch, redisOpen, redisHigh, redisLow, redisClose, redisVolume));
    }

    // 30분봉 리스트 → 60분봉 집계
    private CandleResponse aggregateThirtyMinuteCandles(List<ThirtyMinuteCandle> candles, LocalDateTime bucketStart) {
        long open   = candles.get(0).getOpenPrice();
        long close  = candles.get(candles.size() - 1).getClosePrice();
        long high   = candles.stream().mapToLong(ThirtyMinuteCandle::getHighPrice).max().orElse(0);
        long low    = candles.stream().mapToLong(ThirtyMinuteCandle::getLowPrice).min().orElse(0);
        long volume = candles.stream().mapToLong(ThirtyMinuteCandle::getVolume).sum();
        return CandleResponse.ofAggregated(bucketStart, open, high, low, close, volume);
    }

    // 월봉 리스트 → 년봉 집계
    private CandleResponse aggregateMonthlyCandles(List<MonthlyCandle> candles, LocalDateTime bucketStart) {
        long open   = candles.get(0).getOpenPrice();
        long close  = candles.get(candles.size() - 1).getClosePrice();
        long high   = candles.stream().mapToLong(MonthlyCandle::getHighPrice).max().orElse(0);
        long low    = candles.stream().mapToLong(MonthlyCandle::getLowPrice).min().orElse(0);
        long volume = candles.stream().mapToLong(MonthlyCandle::getVolume).sum();
        return CandleResponse.ofAggregated(bucketStart, open, high, low, close, volume);
    }

    // 절대 분 단위 버킷 시작 시각
    private LocalDateTime toBucketStart(LocalDateTime candleTime, int unit) {
        int minuteOfDay = candleTime.getHour() * 60 + candleTime.getMinute();
        int bucketMinute = Math.floorDiv(minuteOfDay, unit) * unit;
        return candleTime.toLocalDate().atTime(bucketMinute / 60, bucketMinute % 60);
    }

    private LocalDate toWeekBucketStart(LocalDate date) {
        return date.with(java.time.DayOfWeek.MONDAY);
    }

    private LocalDate toMonthBucketStart(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    private LocalDate toYearBucketStart(LocalDate date) {
        return date.withDayOfYear(1);
    }
}
