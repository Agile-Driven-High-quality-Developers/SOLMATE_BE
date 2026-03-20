package org.solmate.domain.stock.service;

import java.util.List;
import java.util.Map;

import org.solmate.domain.stock.dto.response.StockOrderBookResponse;
import org.solmate.external.ls.dto.websocket.LsWsOrderBookResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookService {

    private static final String INFO_KEY_PREFIX = "stock:info:";

    private final StringRedisTemplate redisTemplate;

    public void save(LsWsOrderBookResponse.Body body) {
        String key = INFO_KEY_PREFIX + body.shcode();
        redisTemplate.opsForHash().putAll(key, Map.ofEntries(
                Map.entry("ask1", trim(body.offerho1())),
                Map.entry("ask2", trim(body.offerho2())),
                Map.entry("ask3", trim(body.offerho3())),
                Map.entry("ask4", trim(body.offerho4())),
                Map.entry("ask5", trim(body.offerho5())),
                Map.entry("askVol1", trim(body.unt_offerrem1())),
                Map.entry("askVol2", trim(body.unt_offerrem2())),
                Map.entry("askVol3", trim(body.unt_offerrem3())),
                Map.entry("askVol4", trim(body.unt_offerrem4())),
                Map.entry("askVol5", trim(body.unt_offerrem5())),
                Map.entry("bid1", trim(body.bidho1())),
                Map.entry("bid2", trim(body.bidho2())),
                Map.entry("bid3", trim(body.bidho3())),
                Map.entry("bid4", trim(body.bidho4())),
                Map.entry("bid5", trim(body.bidho5())),
                Map.entry("bidVol1", trim(body.unt_bidrem1())),
                Map.entry("bidVol2", trim(body.unt_bidrem2())),
                Map.entry("bidVol3", trim(body.unt_bidrem3())),
                Map.entry("bidVol4", trim(body.unt_bidrem4())),
                Map.entry("bidVol5", trim(body.unt_bidrem5()))
        ));
    }

    public StockOrderBookResponse getOrderBook(String stockCode) {
        Map<Object, Object> info = redisTemplate.opsForHash().entries(INFO_KEY_PREFIX + stockCode);
        if (info.isEmpty()) return null;

        long currentPrice = parseLong((String) info.get("cur"));
        double changeRate = parseDouble((String) info.get("chgRate"));

        List<StockOrderBookResponse.PriceLevel> sellLevels = List.of(
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("ask1")), parseLong((String) info.get("askVol1"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("ask2")), parseLong((String) info.get("askVol2"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("ask3")), parseLong((String) info.get("askVol3"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("ask4")), parseLong((String) info.get("askVol4"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("ask5")), parseLong((String) info.get("askVol5")))
        );

        List<StockOrderBookResponse.PriceLevel> buyLevels = List.of(
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("bid1")), parseLong((String) info.get("bidVol1"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("bid2")), parseLong((String) info.get("bidVol2"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("bid3")), parseLong((String) info.get("bidVol3"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("bid4")), parseLong((String) info.get("bidVol4"))),
                new StockOrderBookResponse.PriceLevel(parseLong((String) info.get("bid5")), parseLong((String) info.get("bidVol5")))
        );

        return new StockOrderBookResponse(stockCode, currentPrice, changeRate, sellLevels, buyLevels);
    }

    private String trim(String value) {
        return value == null ? "0" : value.trim();
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        return Long.parseLong(value.trim());
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        return Double.parseDouble(value.trim());
    }
}
