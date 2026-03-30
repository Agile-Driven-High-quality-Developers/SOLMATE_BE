package org.solmate.domain.stock.service;

import java.io.StringReader;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.domain.stock.repository.MinuteCandleRepository;
import org.solmate.external.ls.client.LsApiClient;
import org.solmate.external.ls.dto.response.LsDailyCandleResponse;
import org.solmate.external.ls.dto.response.LsMinuteCandleResponse;
import org.solmate.external.ls.dto.response.LsUnifiedMinuteCandleResponse;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleLoadService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final LsApiClient lsApiClient;
    private final DataSource dataSource;
    private final MinuteCandleRepository minuteCandleRepository;

    /**
     * 1분봉 과거 데이터 적재
     * - sdate ~ edate 범위 (최근 3영업일 기준으로 호출)
     * - 연속조회로 전체 데이터 수집
     * - 호출 후 200ms 대기 (LS API 초당 1건 rate limit 준수)
     */
    public boolean loadMinuteCandles(String stockCode, String sdate, String edate) {
        List<MinuteCandle> candles = new ArrayList<>();

        try {
            LsMinuteCandleResponse response = lsApiClient.getMinuteCandles(stockCode, sdate, edate);
            collectMinuteCandles(response, stockCode, candles);

            while (response.hasNext()) {
                sleep();
                String ctsDate = response.t8412OutBlock().cts_date();
                String ctsTime = response.t8412OutBlock().cts_time();
                response = lsApiClient.getMinuteCandlesContinue(stockCode, sdate, edate, ctsDate, ctsTime);
                collectMinuteCandles(response, stockCode, candles);
            }

            saveMinuteCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [분봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            return false;
        }
    }

    /**
     * t8452 통합 1분봉 과거 데이터 적재 (KRX+NXT, 프리/에프터마켓 포함)
     * exchgubun: "K"=KRX, "N"=NXT, "U"=통합
     */
    public boolean loadUnifiedMinuteCandles(String stockCode, String sdate, String edate, String exchgubun) {
        List<MinuteCandle> candles = new ArrayList<>();

        try {
            LsUnifiedMinuteCandleResponse response =
                    lsApiClient.getUnifiedMinuteCandles(stockCode, sdate, edate, exchgubun);
            collectUnifiedMinuteCandles(response, stockCode, candles);

            while (response.hasNext()) {
                sleep();
                String ctsDate = response.t8452OutBlock().cts_date();
                String ctsTime = response.t8452OutBlock().cts_time();
                response = lsApiClient.getUnifiedMinuteCandlesContinue(
                        stockCode, sdate, edate, ctsDate, ctsTime, exchgubun);
                collectUnifiedMinuteCandles(response, stockCode, candles);
            }

            saveMinuteCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [통합분봉 적재 실패] stockCode={}, exchgubun={} - {}", stockCode, exchgubun, e.getMessage());
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

        // NXT 세션 시간 로깅 (첫 페이지에만 출력됨)
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
                // log.warn("[통합분봉 파싱 실패] stockCode={}, date={}, time={}", stockCode, item.date(), item.time());
            }
        }
    }

    /**
     * 일봉 과거 데이터 적재
     * - sdate ~ edate 범위 (최근 365일 기준으로 호출)
     * - 연속조회로 전체 데이터 수집
     * - 호출 후 200ms 대기 (LS API 초당 1건 rate limit 준수)
     */
    public boolean loadDailyCandles(String stockCode, String sdate, String edate) {
        List<DailyCandle> candles = new ArrayList<>();

        try {
            LsDailyCandleResponse response = lsApiClient.getDailyCandles(stockCode, sdate, edate);
            collectDailyCandles(response, stockCode, candles);

            while (response.hasNext()) {
                sleep();
                String ctsDate = response.t8410OutBlock().cts_date();
                response = lsApiClient.getDailyCandlesContinue(stockCode, sdate, edate, ctsDate);
                collectDailyCandles(response, stockCode, candles);
            }

            saveDailyCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [일봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            return false;
        }
    }

    /**
     * 통합 일봉 적재 - DB의 통합 분봉(t8452)을 날짜별로 집계하여 daily_candle에 저장
     * loadUnifiedMinuteCandles() 호출 후 사용해야 분봉 데이터가 존재함
     */
    public boolean loadDailyFromMinuteCandles(String stockCode, String sdate, String edate) {
        try {
            LocalDateTime from = LocalDate.parse(sdate, DATE_FMT).atStartOfDay();
            LocalDateTime to   = LocalDate.parse(edate, DATE_FMT).atTime(23, 59, 59);

            List<MinuteCandle> minuteCandles = minuteCandleRepository
                    .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

            if (minuteCandles.isEmpty()) return true;

            Map<LocalDate, List<MinuteCandle>> byDate = minuteCandles.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getCandleTime().toLocalDate(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<DailyCandle> dailyCandles = new ArrayList<>();
            for (Map.Entry<LocalDate, List<MinuteCandle>> entry : byDate.entrySet()) {
                List<MinuteCandle> day = entry.getValue();
                dailyCandles.add(DailyCandle.builder()
                        .stockCode(stockCode)
                        .openPrice(day.get(0).getOpenPrice())
                        .highPrice(day.stream().mapToLong(MinuteCandle::getHighPrice).max().orElse(0))
                        .lowPrice(day.stream().mapToLong(MinuteCandle::getLowPrice).min().orElse(0))
                        .closePrice(day.get(day.size() - 1).getClosePrice())
                        .volume(day.stream().mapToLong(MinuteCandle::getVolume).sum())
                        .candleTime(entry.getKey().atStartOfDay())
                        .build());
            }

            saveDailyCandles(dailyCandles, stockCode);
            return true;
        } catch (Exception e) {
            log.error("  ✗ [통합일봉 집계 실패] stockCode={} - {}", stockCode, e.getMessage());
            return false;
        }
    }

    /** LS API rate limit 준수를 위한 대기 (2000ms, WebSocket 호출과 합산 고려) */
    private void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void collectMinuteCandles(LsMinuteCandleResponse response, String stockCode,
                                       List<MinuteCandle> candles) {
        if (response.t8412OutBlock1() == null) return;

        List<LsMinuteCandleResponse.OutBlock1> items = response.t8412OutBlock1();
        if (!items.isEmpty()) {
            LsMinuteCandleResponse.OutBlock1 first = items.get(0);
            LsMinuteCandleResponse.OutBlock1 last  = items.get(items.size() - 1);
            log.info("  [LS API 응답] 건수={}, 첫 봉={} {}, 마지막 봉={} {}",
                    items.size(), first.date(), first.time(), last.date(), last.time());
        }

        for (LsMinuteCandleResponse.OutBlock1 item : items) {
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
                // log.warn("[분봉 파싱 실패] stockCode={}, date={}, time={}", stockCode, item.date(), item.time());
            }
        }
    }

    private void collectDailyCandles(LsDailyCandleResponse response, String stockCode,
                                      List<DailyCandle> candles) {
        if (response.t8410OutBlock1() == null) return;

        for (LsDailyCandleResponse.OutBlock1 item : response.t8410OutBlock1()) {
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
                // log.warn("[일봉 파싱 실패] stockCode={}, date={}", stockCode, item.date());
            }
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
            bulkCopy("minute_candle", csv.toString());
            log.info("  ✓ [분봉] stockCode={}, {}건 적재 완료", stockCode, candles.size());
        } catch (Exception e) {
            log.error("  ✗ [분봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
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
            bulkCopy("daily_candle", csv.toString());
            log.info("  ✓ [일봉] stockCode={}, {}건 적재 완료", stockCode, candles.size());
        } catch (Exception e) {
            log.error("  ✗ [일봉 저장 실패] stockCode={} - {}", stockCode, e.getMessage());
        }
    }

    private void bulkCopy(String table, String csv) throws Exception {
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
            conn.createStatement().execute(
                "INSERT INTO " + table + " (" + cols + ") " +
                "SELECT " + cols + " FROM tmp_" + table + " ON CONFLICT DO NOTHING"
            );
            conn.commit();
        }
    }
}
