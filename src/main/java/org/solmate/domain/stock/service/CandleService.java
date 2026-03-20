package org.solmate.domain.stock.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final MinuteCandleRepository minuteCandleRepository;
    private final DailyCandleRepository dailyCandleRepository;
    private final CandleAccumulatorService candleAccumulatorService;

    // 오늘 날짜 기준 1분봉 조회 (DB + 현재 진행 중인 봉 포함)
    public List<CandleResponse> getMinuteCandles(String stockCode) {
        LocalDateTime from = LocalDate.now().atTime(LocalTime.of(9, 0));
        LocalDateTime to   = LocalDate.now().atTime(LocalTime.of(15, 30));

        List<MinuteCandle> candles = minuteCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream()
                        .map(c -> CandleResponse.from(c, c.getCandleTime()))
                        .toList()
        );

        // 현재 진행 중인 봉 Redis에서 추가
        appendCurrentCandle(result, stockCode, "candle:1min:");
        return result;
    }

    // 특정 기간 일봉 조회 (DB + 오늘 진행 중인 봉 포함)
    public List<CandleResponse> getDailyCandles(String stockCode, int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        LocalDateTime to   = LocalDate.now().atStartOfDay();

        List<DailyCandle> candles = dailyCandleRepository
                .findByStockCodeAndCandleTimeBetweenOrderByCandleTimeAsc(stockCode, from, to);

        List<CandleResponse> result = new ArrayList<>(
                candles.stream()
                        .map(c -> CandleResponse.from(c, c.getCandleTime()))
                        .toList()
        );

        // 오늘 진행 중인 일봉 Redis에서 추가
        appendCurrentCandle(result, stockCode, "candle:1day:");
        return result;
    }

    // Redis에서 현재 진행 중인 봉을 꺼내 리스트 끝에 추가
    private void appendCurrentCandle(List<CandleResponse> result, String stockCode, String prefix) {
        Map<Object, Object> current = candleAccumulatorService.getCurrentCandle(stockCode, prefix);
        if (current.isEmpty()) return;

        String startTime = (String) current.get("startTime");
        LocalDateTime candleTime = LocalDateTime.parse(startTime, TIME_FORMATTER);
        result.add(CandleResponse.fromRedis(current, candleTime));
    }
}
