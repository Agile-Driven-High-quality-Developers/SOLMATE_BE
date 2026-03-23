package org.solmate.domain.stock.service;

import java.io.StringReader;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.external.ls.client.LsApiClient;
import org.solmate.external.ls.dto.response.LsDailyCandleResponse;
import org.solmate.external.ls.dto.response.LsMinuteCandleResponse;
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

        for (LsMinuteCandleResponse.OutBlock1 item : response.t8412OutBlock1()) {
            try {
                // time 필드: HHMMSS 형식 (6자리) → 앞 4자리만 사용(HHMM)
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
                log.warn("[분봉 파싱 실패] stockCode={}, date={}, time={}", stockCode, item.date(), item.time());
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
                log.warn("[일봉 파싱 실패] stockCode={}, date={}", stockCode, item.date());
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
