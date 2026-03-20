package org.solmate.domain.stock.service;

import java.util.Map;

import org.solmate.external.ls.dto.websocket.LsWsStockResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInfoService {

    private static final String INFO_KEY_PREFIX = "stock:info:";

    private final StringRedisTemplate redisTemplate;

    public void update(LsWsStockResponse.Body body) {
        redisTemplate.opsForHash().putAll(INFO_KEY_PREFIX + body.shcode(), Map.of(
                "cur", body.price() == null ? "0" : body.price().trim(),
                "open", body.open() == null ? "0" : body.open().trim(),
                "high", body.high() == null ? "0" : body.high().trim(),
                "low", body.low() == null ? "0" : body.low().trim(),
                "vol", body.volume() == null ? "0" : body.volume().trim(),
                "chgRate", body.drate() == null ? "0" : body.drate().trim()
        ));
    }
}
