package org.solmate.domain.stock.service;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.ZoneId;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.FiveMinuteCandle;
import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.domain.stock.entity.MonthlyCandle;
import org.solmate.domain.stock.entity.ThirtyMinuteCandle;
import org.solmate.domain.stock.entity.WeeklyCandle;
import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.FiveMinuteCandleRepository;
import org.solmate.domain.stock.repository.MonthlyCandleRepository;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.domain.stock.repository.ThirtyMinuteCandleRepository;
import org.solmate.domain.stock.repository.WeeklyCandleRepository;
import org.solmate.external.ls.client.LsApiClient;
import org.solmate.external.ls.dto.response.LsUnifiedDailyCandleResponse;
import org.solmate.external.ls.dto.response.LsUnifiedMinuteCandleResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleLoadService {

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final int MAX_PAGE = 200;
    private static final int MINUTE_CANDLE_LIMIT = 1500;

    private final LsApiClient lsApiClient;
    private final DataSource dataSource;
    private final StockRepository stockRepository;
    private final DailyCandleRepository dailyCandleRepository;
    private final FiveMinuteCandleRepository fiveMinuteCandleRepository;
    private final ThirtyMinuteCandleRepository thirtyMinuteCandleRepository;
    private final WeeklyCandleRepository weeklyCandleRepository;
    private final MonthlyCandleRepository monthlyCandleRepository;

    // t8452 통합 1분봉 과거 데이터 적재 (exchgubun: K=KRX, N=NXT, U=통합)
    public boolean loadUnifiedMinuteCandles(String stockCode, String sdate, String edate, String exchgubun) {
        List<MinuteCandle> candles = new ArrayList<>();

        try {
            LsUnifiedMinuteCandleResponse response =
                    lsApiClient.getUnifiedMinuteCandles(stockCode, 1, sdate, edate, exchgubun);
            collectUnifiedMinuteCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext()) {
                if (page >= MAX_PAGE) {
                    log.warn("  [통합분봉] 최대 페이지({}) 초과, 중단: stockCode={}", MAX_PAGE, stockCode);
                    break;
                }
                sleep();
                String ctsDate = response.t8452OutBlock().cts_date();
                String ctsTime = response.t8452OutBlock().cts_time();
                response = lsApiClient.getUnifiedMinuteCandlesContinue(
                        stockCode, 1, sdate, edate, ctsDate, ctsTime, exchgubun);
                collectUnifiedMinuteCandles(response, stockCode, candles);
                page++;
            }

            saveMinuteCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [통합분봉 적재 실패] stockCode={}, exchgubun={} - {}", stockCode, exchgubun, e.getMessage());
            sleep();
            return false;
        }
    }

    // t8452 통합 5분봉 과거 데이터 적재 (최근 1500개) - 초기 일괄 적재용
    public boolean loadFiveMinuteCandles(String stockCode) {
        List<FiveMinuteCandle> candles = new ArrayList<>();
        String today = LocalDate.now(KST).format(DATE_FMT);

        try {
            LsUnifiedMinuteCandleResponse response =
                    lsApiClient.getUnifiedMinuteCandles(stockCode, 5, "19560101", today, "U");
            collectFiveMinuteCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext() && candles.size() < MINUTE_CANDLE_LIMIT) {
                if (page >= MAX_PAGE) break;
                sleep();
                String ctsDate = response.t8452OutBlock().cts_date();
                String ctsTime = response.t8452OutBlock().cts_time();
                response = lsApiClient.getUnifiedMinuteCandlesContinue(stockCode, 5, "19560101", today, ctsDate, ctsTime, "U");
                collectFiveMinuteCandles(response, stockCode, candles);
                page++;
            }

            if (candles.size() > MINUTE_CANDLE_LIMIT) {
                candles = candles.subList(0, MINUTE_CANDLE_LIMIT);
            }

            saveFiveMinuteCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [5분봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            sleep();
            return false;
        }
    }

    // t8452 통합 5분봉 날짜 범위 적재 - 백필용 (어제 하루만 ~144개)
    public boolean loadFiveMinuteCandlesForDate(String stockCode, String sdate, String edate) {
        List<FiveMinuteCandle> candles = new ArrayList<>();

        try {
            LsUnifiedMinuteCandleResponse response =
                    lsApiClient.getUnifiedMinuteCandles(stockCode, 5, sdate, edate, "U");
            collectFiveMinuteCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext()) {
                if (page >= MAX_PAGE) break;
                sleep();
                String ctsDate = response.t8452OutBlock().cts_date();
                String ctsTime = response.t8452OutBlock().cts_time();
                response = lsApiClient.getUnifiedMinuteCandlesContinue(stockCode, 5, sdate, edate, ctsDate, ctsTime, "U");
                collectFiveMinuteCandles(response, stockCode, candles);
                page++;
            }

            saveFiveMinuteCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [5분봉 백필 실패] stockCode={} - {}", stockCode, e.getMessage());
            sleep();
            return false;
        }
    }

    // t8452 통합 30분봉 과거 데이터 적재 (최근 1500개) - 초기 일괄 적재용
    public boolean loadThirtyMinuteCandles(String stockCode) {
        List<ThirtyMinuteCandle> candles = new ArrayList<>();
        String today = LocalDate.now(KST).format(DATE_FMT);

        try {
            LsUnifiedMinuteCandleResponse response =
                    lsApiClient.getUnifiedMinuteCandles(stockCode, 30, "19560101", today, "U");
            collectThirtyMinuteCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext() && candles.size() < MINUTE_CANDLE_LIMIT) {
                if (page >= MAX_PAGE) break;
                sleep();
                String ctsDate = response.t8452OutBlock().cts_date();
                String ctsTime = response.t8452OutBlock().cts_time();
                response = lsApiClient.getUnifiedMinuteCandlesContinue(stockCode, 30, "19560101", today, ctsDate, ctsTime, "U");
                collectThirtyMinuteCandles(response, stockCode, candles);
                page++;
            }

            if (candles.size() > MINUTE_CANDLE_LIMIT) {
                candles = candles.subList(0, MINUTE_CANDLE_LIMIT);
            }

            saveThirtyMinuteCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [30분봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            sleep();
            return false;
        }
    }

    // t8452 통합 30분봉 날짜 범위 적재 - 백필용 (어제 하루만 ~24개)
    public boolean loadThirtyMinuteCandlesForDate(String stockCode, String sdate, String edate) {
        List<ThirtyMinuteCandle> candles = new ArrayList<>();

        try {
            LsUnifiedMinuteCandleResponse response =
                    lsApiClient.getUnifiedMinuteCandles(stockCode, 30, sdate, edate, "U");
            collectThirtyMinuteCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext()) {
                if (page >= MAX_PAGE) break;
                sleep();
                String ctsDate = response.t8452OutBlock().cts_date();
                String ctsTime = response.t8452OutBlock().cts_time();
                response = lsApiClient.getUnifiedMinuteCandlesContinue(stockCode, 30, sdate, edate, ctsDate, ctsTime, "U");
                collectThirtyMinuteCandles(response, stockCode, candles);
                page++;
            }

            saveThirtyMinuteCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [30분봉 백필 실패] stockCode={} - {}", stockCode, e.getMessage());
            sleep();
            return false;
        }
    }

    private void collectUnifiedMinuteCandles(LsUnifiedMinuteCandleResponse response, String stockCode,
                                              List<MinuteCandle> candles) {
        if (response.candles().isEmpty()) return;

        LsUnifiedMinuteCandleResponse.OutBlock1 first = response.candles().get(0);
        LsUnifiedMinuteCandleResponse.OutBlock1 last  = response.candles().get(response.candles().size() - 1);
        log.info("  [t8452 응답] 건수={}, 첫 봉={} {}, 마지막 봉={} {}",
                response.candles().size(), first.date(), first.time(), last.date(), last.time());

        if (response.t8452OutBlock() != null) {
            log.info("  [NXT 세션] 프리마켓={}-{}, 에프터마켓={}-{}",
                    response.t8452OutBlock().nxt_fm_s_time(), response.t8452OutBlock().nxt_fm_e_time(),
                    response.t8452OutBlock().nxt_am_s_time(), response.t8452OutBlock().nxt_am_e_time());
        }

        for (LsUnifiedMinuteCandleResponse.OutBlock1 item : response.candles()) {
            try {
                String timeStr = item.time().length() >= 6 ? item.time().substring(0, 6) : item.time();
                LocalDateTime candleTime = LocalDateTime.parse(item.date() + timeStr, DATETIME_FMT);

                candles.add(MinuteCandle.builder()
                        .stockCode(stockCode)
                        .openPrice(item.open())
                        .highPrice(item.high())
                        .lowPrice(item.low())
                        .closePrice(item.close())
                        .volume(item.jdiff_vol())
                        .candleTime(candleTime)
                        .build());
            } catch (Exception e) {
                // 파싱 실패 항목 스킵
            }
        }
    }

    // t8451 통합 일봉 과거 데이터 적재
    public boolean loadUnifiedDailyCandles(String stockCode, String sdate, String edate) {
        List<DailyCandle> candles = new ArrayList<>();

        try {
            LsUnifiedDailyCandleResponse response = lsApiClient.getUnifiedDailyCandles(stockCode, "2", sdate, edate);
            collectUnifiedDailyCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext()) {
                if (page >= MAX_PAGE) {
                    log.warn("  [통합일봉] 최대 페이지({}) 초과, 중단: stockCode={}", MAX_PAGE, stockCode);
                    break;
                }
                sleep();
                String ctsDate = response.t8451OutBlock().cts_date();
                response = lsApiClient.getUnifiedDailyCandlesContinue(stockCode, "2", sdate, edate, ctsDate);
                collectUnifiedDailyCandles(response, stockCode, candles);
                page++;
            }

            saveDailyCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [통합일봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            sleep();
            return false;
        }
    }

    // 일봉 전체 적재 (상장일부터 오늘까지, 중복은 ON CONFLICT DO NOTHING으로 스킵)
    public boolean loadMissingDailyCandles(String stockCode) {
        String today = LocalDate.now(KST).format(DATE_FMT);
        return loadUnifiedDailyCandles(stockCode, "19560101", today);
    }

    // t8451 통합 주봉 과거 데이터 적재
    public boolean loadWeeklyCandles(String stockCode, String sdate, String edate) {
        List<WeeklyCandle> candles = new ArrayList<>();

        try {
            LsUnifiedDailyCandleResponse response = lsApiClient.getUnifiedDailyCandles(stockCode, "3", sdate, edate);
            collectWeeklyCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext()) {
                if (page >= MAX_PAGE) break;
                sleep();
                String ctsDate = response.t8451OutBlock().cts_date();
                response = lsApiClient.getUnifiedDailyCandlesContinue(stockCode, "3", sdate, edate, ctsDate);
                collectWeeklyCandles(response, stockCode, candles);
                page++;
            }

            saveWeeklyCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [주봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            sleep();
            return false;
        }
    }

    // t8451 통합 월봉 과거 데이터 적재
    public boolean loadMonthlyCandles(String stockCode, String sdate, String edate) {
        List<MonthlyCandle> candles = new ArrayList<>();

        try {
            LsUnifiedDailyCandleResponse response = lsApiClient.getUnifiedDailyCandles(stockCode, "4", sdate, edate);
            collectMonthlyCandles(response, stockCode, candles);

            int page = 1;
            while (response.hasNext()) {
                if (page >= MAX_PAGE) break;
                sleep();
                String ctsDate = response.t8451OutBlock().cts_date();
                response = lsApiClient.getUnifiedDailyCandlesContinue(stockCode, "4", sdate, edate, ctsDate);
                collectMonthlyCandles(response, stockCode, candles);
                page++;
            }

            saveMonthlyCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [월봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            sleep();
            return false;
        }
    }

    private void collectFiveMinuteCandles(LsUnifiedMinuteCandleResponse response, String stockCode,
                                          List<FiveMinuteCandle> candles) {
        for (LsUnifiedMinuteCandleResponse.OutBlock1 item : response.candles()) {
            try {
                String timeStr = item.time().length() >= 6 ? item.time().substring(0, 6) : item.time();
                LocalDateTime raw = LocalDateTime.parse(item.date() + timeStr, DATETIME_FMT);
                int floored = (raw.getMinute() / 5) * 5;
                LocalDateTime candleTime = raw.toLocalDate().atTime(raw.getHour(), floored);
                candles.add(FiveMinuteCandle.builder()
                        .stockCode(stockCode)
                        .openPrice(item.open())
                        .highPrice(item.high())
                        .lowPrice(item.low())
                        .closePrice(item.close())
                        .volume(item.jdiff_vol())
                        .candleTime(candleTime)
                        .build());
            } catch (Exception e) {
                // 파싱 실패 항목 스킵
            }
        }
    }

    private void collectThirtyMinuteCandles(LsUnifiedMinuteCandleResponse response, String stockCode,
                                             List<ThirtyMinuteCandle> candles) {
        for (LsUnifiedMinuteCandleResponse.OutBlock1 item : response.candles()) {
            try {
                String timeStr = item.time().length() >= 6 ? item.time().substring(0, 6) : item.time();
                LocalDateTime raw = LocalDateTime.parse(item.date() + timeStr, DATETIME_FMT);
                int floored = (raw.getMinute() / 30) * 30;
                LocalDateTime candleTime = raw.toLocalDate().atTime(raw.getHour(), floored);
                candles.add(ThirtyMinuteCandle.builder()
                        .stockCode(stockCode)
                        .openPrice(item.open())
                        .highPrice(item.high())
                        .lowPrice(item.low())
                        .closePrice(item.close())
                        .volume(item.jdiff_vol())
                        .candleTime(candleTime)
                        .build());
            } catch (Exception e) {
                // 파싱 실패 항목 스킵
            }
        }
    }

    private void collectWeeklyCandles(LsUnifiedDailyCandleResponse response, String stockCode,
                                       List<WeeklyCandle> candles) {
        for (LsUnifiedDailyCandleResponse.OutBlock1 item : response.candles()) {
            try {
                LocalDateTime candleTime = LocalDate.parse(item.date(), DATE_FMT).atStartOfDay();
                candles.add(WeeklyCandle.builder()
                        .stockCode(stockCode)
                        .openPrice(item.open())
                        .highPrice(item.high())
                        .lowPrice(item.low())
                        .closePrice(item.close())
                        .volume(item.jdiff_vol())
                        .candleTime(candleTime)
                        .build());
            } catch (Exception e) {
                // 파싱 실패 항목 스킵
            }
        }
    }

    private void collectMonthlyCandles(LsUnifiedDailyCandleResponse response, String stockCode,
                                        List<MonthlyCandle> candles) {
        for (LsUnifiedDailyCandleResponse.OutBlock1 item : response.candles()) {
            try {
                LocalDateTime candleTime = LocalDate.parse(item.date(), DATE_FMT).atStartOfDay();
                candles.add(MonthlyCandle.builder()
                        .stockCode(stockCode)
                        .openPrice(item.open())
                        .highPrice(item.high())
                        .lowPrice(item.low())
                        .closePrice(item.close())
                        .volume(item.jdiff_vol())
                        .candleTime(candleTime)
                        .build());
            } catch (Exception e) {
                // 파싱 실패 항목 스킵
            }
        }
    }

    private void collectUnifiedDailyCandles(LsUnifiedDailyCandleResponse response, String stockCode,
                                             List<DailyCandle> candles) {
        for (LsUnifiedDailyCandleResponse.OutBlock1 item : response.candles()) {
            try {
                LocalDateTime candleTime = LocalDate.parse(item.date(), DATE_FMT).atStartOfDay();
                candles.add(DailyCandle.builder()
                        .stockCode(stockCode)
                        .openPrice(item.open())
                        .highPrice(item.high())
                        .lowPrice(item.low())
                        .closePrice(item.close())
                        .volume(item.jdiff_vol())
                        .candleTime(candleTime)
                        .build());
            } catch (Exception e) {
                // 파싱 실패 항목 스킵
            }
        }
    }

    // 전체 종목 일괄 적재 (백그라운드)
    @Async
    public void loadAllAsync(int minuteDays) {
        List<String> codes = stockRepository.findAllTickerCodes();
        int total = codes.size();
        String today      = LocalDate.now(KST).format(DATE_FMT);
        String minuteFrom = LocalDate.now(KST).minusDays(minuteDays).format(DATE_FMT);

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 일괄 적재 시작] 종목={}개, 분봉={}일, 일봉=상장일 전체", total, minuteDays);
        log.info("└─────────────────────────────────────────────");

        AtomicInteger minuteSuccess = new AtomicInteger(), minuteFail = new AtomicInteger();
        AtomicInteger dailySuccess  = new AtomicInteger(), dailyFail  = new AtomicInteger();

        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            log.info("[{}/{}] 적재 중: {}", i + 1, total, code);
            if (loadUnifiedMinuteCandles(code, minuteFrom, today, "U")) minuteSuccess.incrementAndGet();
            else minuteFail.incrementAndGet();
            if (loadUnifiedDailyCandles(code, "19560101", today)) dailySuccess.incrementAndGet();
            else dailyFail.incrementAndGet();
        }

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 일괄 적재 완료]");
        log.info("│ 분봉: 성공 {}건 / 실패 {}건", minuteSuccess, minuteFail);
        log.info("│ 일봉: 성공 {}건 / 실패 {}건", dailySuccess, dailyFail);
        log.info("└─────────────────────────────────────────────");
    }

    // 일봉 누락분 + 5분봉/30분봉/주봉/월봉 전체 종목 일괄 적재 (백그라운드)
    @Async
    public void loadExtendedAsync() {
        List<String> codes = stockRepository.findAllTickerCodes();
        int total = codes.size();
        String today = LocalDate.now(KST).format(DATE_FMT);

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [확장 캔들 일괄 적재 시작] 종목={}개", total);
        log.info("└─────────────────────────────────────────────");

        int fiveSuccess = 0, fiveFail = 0;
        int thirtySuccess = 0, thirtyFail = 0;
        int weeklySuccess = 0, weeklyFail = 0;
        int monthlySuccess = 0, monthlyFail = 0;

        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            log.info("[{}/{}] 확장 캔들 적재 중: {}", i + 1, total, code);
            if (loadFiveMinuteCandles(code)) fiveSuccess++; else fiveFail++;
            if (loadThirtyMinuteCandles(code)) thirtySuccess++; else thirtyFail++;
            if (loadWeeklyCandles(code, "19560101", today)) weeklySuccess++; else weeklyFail++;
            if (loadMonthlyCandles(code, "19560101", today)) monthlySuccess++; else monthlyFail++;
        }

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [확장 캔들 일괄 적재 완료]");
        log.info("│ 5분봉: 성공 {}건 / 실패 {}건", fiveSuccess, fiveFail);
        log.info("│ 30분봉: 성공 {}건 / 실패 {}건", thirtySuccess, thirtyFail);
        log.info("│ 주봉: 성공 {}건 / 실패 {}건", weeklySuccess, weeklyFail);
        log.info("│ 월봉: 성공 {}건 / 실패 {}건", monthlySuccess, monthlyFail);
        log.info("└─────────────────────────────────────────────");
    }

    // 전날 캔들 백필 (스케줄러용) - DB 전체 종목 대상
    public void backfillYesterday(String date) {
        backfill(date);
    }

    // 특정 날짜 5분봉/30분봉/주봉/월봉 백필 (백그라운드) - 수동 보정용
    @Async
    public void backfillForDateAsync(String date) {
        backfill(date);
    }

    private void backfill(String date) {
        List<String> codes = stockRepository.findAllTickerCodes();
        int total = codes.size();

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [백필 시작] 종목={}개, 대상={}", total, date);
        log.info("└─────────────────────────────────────────────");

        int minuteSuccess = 0, minuteFail = 0;
        int dailySuccess = 0, dailyFail = 0;
        int fiveSuccess = 0, fiveFail = 0;
        int thirtySuccess = 0, thirtyFail = 0;
        int weeklySuccess = 0, weeklyFail = 0;
        int monthlySuccess = 0, monthlyFail = 0;

        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            log.info("[{}/{}] 백필 중: {}", i + 1, total, code);
            if (loadUnifiedMinuteCandles(code, date, date, "U")) minuteSuccess++; else minuteFail++;
            if (loadUnifiedDailyCandles(code, date, date)) dailySuccess++; else dailyFail++;
            if (loadFiveMinuteCandlesForDate(code, date, date)) fiveSuccess++; else fiveFail++;
            if (loadThirtyMinuteCandlesForDate(code, date, date)) thirtySuccess++; else thirtyFail++;
            if (loadWeeklyCandles(code, date, date)) weeklySuccess++; else weeklyFail++;
            if (loadMonthlyCandles(code, date, date)) monthlySuccess++; else monthlyFail++;
        }

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [백필 완료] 대상={}", date);
        log.info("│ 1분봉:  성공={}건/실패={}건", minuteSuccess, minuteFail);
        log.info("│ 5분봉:  성공={}건/실패={}건", fiveSuccess, fiveFail);
        log.info("│ 30분봉: 성공={}건/실패={}건", thirtySuccess, thirtyFail);
        log.info("│ 일봉:   성공={}건/실패={}건", dailySuccess, dailyFail);
        log.info("│ 주봉:   성공={}건/실패={}건", weeklySuccess, weeklyFail);
        log.info("│ 월봉:   성공={}건/실패={}건", monthlySuccess, monthlyFail);
        log.info("└─────────────────────────────────────────────");
    }

    // 실패 종목 재적재 (백그라운드)
    @Async
    public void retryAsync(List<String> stockCodes, int minuteDays) {
        int total = stockCodes.size();
        String today      = LocalDate.now(KST).format(DATE_FMT);
        String minuteFrom = LocalDate.now(KST).minusDays(minuteDays).format(DATE_FMT);

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 재적재 시작] 종목={}개", total);
        log.info("└─────────────────────────────────────────────");

        AtomicInteger minuteSuccess = new AtomicInteger(), minuteFail = new AtomicInteger();
        AtomicInteger dailySuccess  = new AtomicInteger(), dailyFail  = new AtomicInteger();

        for (int i = 0; i < stockCodes.size(); i++) {
            String code = stockCodes.get(i);
            log.info("[{}/{}] 재적재 중: {}", i + 1, total, code);
            if (loadUnifiedMinuteCandles(code, minuteFrom, today, "U")) minuteSuccess.incrementAndGet();
            else minuteFail.incrementAndGet();
            if (loadUnifiedDailyCandles(code, "19560101", today)) dailySuccess.incrementAndGet();
            else dailyFail.incrementAndGet();
        }

        log.info("┌─────────────────────────────────────────────");
        log.info("│ [캔들 재적재 완료]");
        log.info("│ 분봉: 성공 {}건 / 실패 {}건", minuteSuccess, minuteFail);
        log.info("│ 일봉: 성공 {}건 / 실패 {}건", dailySuccess, dailyFail);
        log.info("└─────────────────────────────────────────────");
    }

    // LS API rate limit 준수 대기
    private void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final DateTimeFormatter COPY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private void saveMinuteCandles(List<MinuteCandle> candles, String stockCode) {
        if (candles.isEmpty()) return;
        try {
            StringBuilder csv = new StringBuilder();
            for (MinuteCandle c : candles) {
                csv.append(c.getStockCode()).append(',')
                   .append(c.getOpenPrice()).append(',')
                   .append(c.getHighPrice()).append(',')
                   .append(c.getLowPrice()).append(',')
                   .append(c.getClosePrice()).append(',')
                   .append(c.getVolume()).append(',')
                   .append(c.getCandleTime().format(COPY_FMT)).append('\n');
            }
            int inserted = bulkCopy("minute_candle", csv.toString());
            log.info("  ✓ [분봉] stockCode={}, 조회={}건, 신규삽입={}건", stockCode, candles.size(), inserted);
        } catch (Exception e) {
            log.error("  ✗ [분봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
        }
    }

    private void saveFiveMinuteCandles(List<FiveMinuteCandle> candles, String stockCode) {
        if (candles.isEmpty()) return;
        try {
            StringBuilder csv = new StringBuilder();
            for (FiveMinuteCandle c : candles) {
                csv.append(c.getStockCode()).append(',')
                   .append(c.getOpenPrice()).append(',')
                   .append(c.getHighPrice()).append(',')
                   .append(c.getLowPrice()).append(',')
                   .append(c.getClosePrice()).append(',')
                   .append(c.getVolume()).append(',')
                   .append(c.getCandleTime().format(COPY_FMT)).append('\n');
            }
            int inserted = bulkCopy("five_minute_candle", csv.toString());
            log.info("  ✓ [5분봉] stockCode={}, 조회={}건, 신규삽입={}건", stockCode, candles.size(), inserted);
        } catch (Exception e) {
            log.error("  ✗ [5분봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
        }
    }

    private void saveThirtyMinuteCandles(List<ThirtyMinuteCandle> candles, String stockCode) {
        if (candles.isEmpty()) return;
        try {
            StringBuilder csv = new StringBuilder();
            for (ThirtyMinuteCandle c : candles) {
                csv.append(c.getStockCode()).append(',')
                   .append(c.getOpenPrice()).append(',')
                   .append(c.getHighPrice()).append(',')
                   .append(c.getLowPrice()).append(',')
                   .append(c.getClosePrice()).append(',')
                   .append(c.getVolume()).append(',')
                   .append(c.getCandleTime().format(COPY_FMT)).append('\n');
            }
            int inserted = bulkCopy("thirty_minute_candle", csv.toString());
            log.info("  ✓ [30분봉] stockCode={}, 조회={}건, 신규삽입={}건", stockCode, candles.size(), inserted);
        } catch (Exception e) {
            log.error("  ✗ [30분봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
        }
    }

    private void saveWeeklyCandles(List<WeeklyCandle> candles, String stockCode) {
        if (candles.isEmpty()) return;
        try {
            StringBuilder csv = new StringBuilder();
            for (WeeklyCandle c : candles) {
                csv.append(c.getStockCode()).append(',')
                   .append(c.getOpenPrice()).append(',')
                   .append(c.getHighPrice()).append(',')
                   .append(c.getLowPrice()).append(',')
                   .append(c.getClosePrice()).append(',')
                   .append(c.getVolume()).append(',')
                   .append(c.getCandleTime().format(COPY_FMT)).append('\n');
            }
            int inserted = bulkCopy("weekly_candle", csv.toString());
            log.info("  ✓ [주봉] stockCode={}, 조회={}건, 신규삽입={}건", stockCode, candles.size(), inserted);
        } catch (Exception e) {
            log.error("  ✗ [주봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
        }
    }

    private void saveMonthlyCandles(List<MonthlyCandle> candles, String stockCode) {
        if (candles.isEmpty()) return;
        try {
            StringBuilder csv = new StringBuilder();
            for (MonthlyCandle c : candles) {
                csv.append(c.getStockCode()).append(',')
                   .append(c.getOpenPrice()).append(',')
                   .append(c.getHighPrice()).append(',')
                   .append(c.getLowPrice()).append(',')
                   .append(c.getClosePrice()).append(',')
                   .append(c.getVolume()).append(',')
                   .append(c.getCandleTime().format(COPY_FMT)).append('\n');
            }
            int inserted = bulkCopy("monthly_candle", csv.toString());
            log.info("  ✓ [월봉] stockCode={}, 조회={}건, 신규삽입={}건", stockCode, candles.size(), inserted);
        } catch (Exception e) {
            log.error("  ✗ [월봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
        }
    }

    private void saveDailyCandles(List<DailyCandle> candles, String stockCode) {
        if (candles.isEmpty()) return;
        try {
            StringBuilder csv = new StringBuilder();
            for (DailyCandle c : candles) {
                csv.append(c.getStockCode()).append(',')
                   .append(c.getOpenPrice()).append(',')
                   .append(c.getHighPrice()).append(',')
                   .append(c.getLowPrice()).append(',')
                   .append(c.getClosePrice()).append(',')
                   .append(c.getVolume()).append(',')
                   .append(c.getCandleTime().format(COPY_FMT)).append('\n');
            }
            int inserted = bulkCopy("daily_candle", csv.toString());
            log.info("  ✓ [일봉] stockCode={}, 조회={}건, 신규삽입={}건", stockCode, candles.size(), inserted);
        } catch (Exception e) {
            log.error("  ✗ [일봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
        }
    }

    private int bulkCopy(String table, String csv) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String cols = "stock_code, open_price, high_price, low_price, close_price, volume, candle_time";
            conn.createStatement().execute(
                "CREATE TEMP TABLE tmp_" + table + " " +
                "(stock_code varchar(12), open_price bigint, high_price bigint, " +
                " low_price bigint, close_price bigint, volume bigint, candle_time timestamp) " +
                "ON COMMIT DROP"
            );
            CopyManager copyManager = new CopyManager(conn.unwrap(BaseConnection.class));
            copyManager.copyIn(
                "COPY tmp_" + table + " (" + cols + ") FROM STDIN WITH (FORMAT csv)",
                new StringReader(csv)
            );
            int inserted = conn.createStatement().executeUpdate(
                "INSERT INTO " + table + " (" + cols + ") " +
                "SELECT " + cols + " FROM tmp_" + table + " ON CONFLICT DO NOTHING"
            );
            conn.commit();
            return inserted;
        }
    }
}
