package org.solmate.domain.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solmate.domain.market.dto.response.MarketIndicatorResponse;
import org.solmate.external.ls.dto.websocket.LsWsCurrencyResponse;
import org.solmate.external.ls.dto.websocket.LsWsIndexResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndicatorService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KOSPI_KEY = "market:indicator:KOSPI";
    private static final String KOSDAQ_KEY = "market:indicator:KOSDAQ";
    private static final String USD_KRW_KEY = "market:indicator:USD_KRW";

    // 조회
    @Transactional(readOnly = true)
    public MarketIndicatorResponse getMarketIndicators() {
        return MarketIndicatorResponse.builder()
                .kospi(getIndexInfo(KOSPI_KEY))
                .kosdaq(getIndexInfo(KOSDAQ_KEY))
                .usdKrw(getIndexInfo(USD_KRW_KEY))
                .build();
    }

    // 지수 Redis 저장 (LsWebSocketClient에서 호출)
    public void saveIndex(LsWsIndexResponse response) {
        try {
            if (response.body() == null) return;

            String trKey = response.header().tr_key();
            String redisKey = "001".equals(trKey) ? KOSPI_KEY : KOSDAQ_KEY;

            String json = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("cur", response.body().jisu())
                            .put("change", response.body().change())
                            .put("rate", response.body().drate())
                            .put("sign", response.body().sign())
                            .put("high", response.body().highjisu())
                            .put("low", response.body().lowjisu())
                            .put("asOf", response.body().time())
            );

            stringRedisTemplate.opsForValue().set(redisKey, json);
            log.debug("시장 지표 Redis 저장 완료 - {}: {}", redisKey, json);

        } catch (Exception e) {
            log.error("시장 지표 Redis 저장 실패: {}", e.getMessage());
        }
    }

    // 환율 Redis 저장 (LsWebSocketClient에서 호출)
    public void saveCurrency(LsWsCurrencyResponse response) {
        try {
            if (response.body() == null) return;

            String json = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("cur", response.body().price())
                            .put("change", response.body().change())
                            .put("rate", response.body().drate())
                            .put("sign", response.body().sign())
                            .put("high", response.body().high())
                            .put("low", response.body().low())
                            .put("asOf", response.body().time())
            );

            stringRedisTemplate.opsForValue().set(USD_KRW_KEY, json);
            log.debug("환율 Redis 저장 완료 - {}: {}", USD_KRW_KEY, json);

        } catch (Exception e) {
            log.error("환율 Redis 저장 실패: {}", e.getMessage());
        }
    }

    private MarketIndicatorResponse.IndexInfo getIndexInfo(String redisKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey);
            if (json == null) {
                log.debug("Redis에 데이터 없음 - {}", redisKey);
                return null;
            }

            var node = objectMapper.readTree(json);
            return MarketIndicatorResponse.IndexInfo.builder()
                    .cur(node.get("cur").asText())
                    .change(node.get("change").asText())
                    .rate(node.get("rate").asText())
                    .sign(node.get("sign").asText())
                    .high(node.get("high").asText())
                    .low(node.get("low").asText())
                    .asOf(node.get("asOf").asText())
                    .build();

        } catch (Exception e) {
            log.error("시장 지표 Redis 조회 실패 - {}: {}", redisKey, e.getMessage());
            return null;
        }
    }
}