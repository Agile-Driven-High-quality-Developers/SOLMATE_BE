package org.solmate.domain.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solmate.domain.market.dto.response.MarketIndicatorResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndicatorService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KOSPI_KEY = "market:indicator:KOSPI";
    private static final String KOSDAQ_KEY = "market:indicator:KOSDAQ";
    private static final String USD_KRW_KEY = "market:indicator:USD_KRW";

    public MarketIndicatorResponse getMarketIndicators() {
        return MarketIndicatorResponse.builder()
                .kospi(getIndexInfo(KOSPI_KEY))
                .kosdaq(getIndexInfo(KOSDAQ_KEY))
                .usdKrw(getIndexInfo(USD_KRW_KEY))
                .build();
    }

    private MarketIndicatorResponse.IndexInfo getIndexInfo(String redisKey) {
        try {
            // Redis에서 JSON 문자열 조회
            String json = stringRedisTemplate.opsForValue().get(redisKey);

            // 데이터 없으면 null 반환
            if (json == null) {
                log.warn("Redis에 데이터 없음 - {}", redisKey);
                return null;
            }

            // JSON 문자열 → IndexInfo 객체로 변환
            JsonNode node = objectMapper.readTree(json);
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