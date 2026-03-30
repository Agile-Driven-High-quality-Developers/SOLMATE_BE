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
import org.solmate.external.ls.dto.response.LsUnifiedDailyCandleResponse;
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
     * t8451: 통합 일봉 과거 데이터 적재 (KRX+NXT, 프리/에프터마켓 포함)
     */
    public boolean loadUnifiedDailyCandles(String stockCode, String sdate, String edate) {
        List<DailyCandle> candles = new ArrayList<>();

        try {
            LsUnifiedDailyCandleResponse response = lsApiClient.getUnifiedDailyCandles(stockCode, sdate, edate);
            collectUnifiedDailyCandles(response, stockCode, candles);

            while (response.hasNext()) {
                sleep();
                String ctsDate = response.t8451OutBlock().cts_date();
                response = lsApiClient.getUnifiedDailyCandlesContinue(stockCode, sdate, edate, ctsDate);
                collectUnifiedDailyCandles(response, stockCode, candles);
            }

            saveDailyCandles(candles, stockCode);
            sleep();
            return true;
        } catch (Exception e) {
            log.error("  ✗ [통합일봉 적재 실패] stockCode={} - {}", stockCode, e.getMessage());
            return false;
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
                // log.warn("[통합일봉 파싱 실패] stockCode={}, date={}", stockCode, item.date());
            }
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
