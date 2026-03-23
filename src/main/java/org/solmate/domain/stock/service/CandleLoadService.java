package org.solmate.domain.stock.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.solmate.domain.stock.entity.DailyCandle;
import org.solmate.domain.stock.entity.MinuteCandle;
import org.solmate.domain.stock.repository.DailyCandleRepository;
import org.solmate.domain.stock.repository.MinuteCandleRepository;
import org.solmate.external.ls.client.LsApiClient;
import org.solmate.external.ls.dto.response.LsDailyCandleResponse;
import org.solmate.external.ls.dto.response.LsMinuteCandleResponse;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final MinuteCandleRepository minuteCandleRepository;
    private final DailyCandleRepository dailyCandleRepository;

    /**
     * 1분봉 과거 데이터 적재
     * - sdate ~ edate 범위 (최근 3영업일 기준으로 호출)
     * - 연속조회로 전체 데이터 수집
     * - 호출 후 200ms 대기 (LS API 초당 1건 rate limit 준수)
     */
    public void loadMinuteCandles(String stockCode, String sdate, String edate) {
        log.info("[분봉 적재 시작] stockCode={}, {}~{}", stockCode, sdate, edate);
        List<MinuteCandle> candles = new ArrayList<>();

        try {
            LsMinuteCandleResponse response = lsApiClient.getMinuteCandles(stockCode, sdate, edate);
            collectMinuteCandles(response, stockCode, candles);

            while (response.hasNext()) {
                sleep(); // 연속조회 사이에도 rate limit 준수
                String ctsDate = response.t8412OutBlock().cts_date();
                String ctsTime = response.t8412OutBlock().cts_time();
                response = lsApiClient.getMinuteCandlesContinue(stockCode, sdate, edate, ctsDate, ctsTime);
                collectMinuteCandles(response, stockCode, candles);
            }

            saveMinuteCandles(candles, stockCode);
            sleep(); // 다음 종목 호출 전 대기
        } catch (Exception e) {
            log.error("[분봉 적재 실패] stockCode={}", stockCode, e);
        }
    }

    /**
     * 일봉 과거 데이터 적재
     * - sdate ~ edate 범위 (최근 365일 기준으로 호출)
     * - 연속조회로 전체 데이터 수집
     * - 호출 후 200ms 대기 (LS API 초당 1건 rate limit 준수)
     */
    public void loadDailyCandles(String stockCode, String sdate, String edate) {
        log.info("[일봉 적재 시작] stockCode={}, {}~{}", stockCode, sdate, edate);
        List<DailyCandle> candles = new ArrayList<>();

        try {
            LsDailyCandleResponse response = lsApiClient.getDailyCandles(stockCode, sdate, edate);
            collectDailyCandles(response, stockCode, candles);

            while (response.hasNext()) {
                sleep(); // 연속조회 사이에도 rate limit 준수
                String ctsDate = response.t8410OutBlock().cts_date();
                response = lsApiClient.getDailyCandlesContinue(stockCode, sdate, edate, ctsDate);
                collectDailyCandles(response, stockCode, candles);
            }

            saveDailyCandles(candles, stockCode);
            sleep(); // 다음 종목 호출 전 대기
        } catch (Exception e) {
            log.error("[일봉 적재 실패] stockCode={}", stockCode, e);
        }
    }

    /** LS API 초당 1건 rate limit 준수를 위한 대기 (200ms = 초당 최대 5건) */
    private void sleep() {
        try {
            Thread.sleep(200);
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

    private void saveMinuteCandles(List<MinuteCandle> candles, String stockCode) {
        int saved = 0;
        for (MinuteCandle candle : candles) {
            try {
                minuteCandleRepository.save(candle);
                saved++;
            } catch (DataIntegrityViolationException e) {
                // unique constraint 위반 = 이미 존재하는 데이터 → 스킵
            }
        }
        log.info("[분봉 적재 완료] stockCode={}, {}건 저장 (총 {}건 중)", stockCode, saved, candles.size());
    }

    private void saveDailyCandles(List<DailyCandle> candles, String stockCode) {
        int saved = 0;
        for (DailyCandle candle : candles) {
            try {
                dailyCandleRepository.save(candle);
                saved++;
            } catch (DataIntegrityViolationException e) {
                // unique constraint 위반 = 이미 존재하는 데이터 → 스킵
            }
        }
        log.info("[일봉 적재 완료] stockCode={}, {}건 저장 (총 {}건 중)", stockCode, saved, candles.size());
    }
}
