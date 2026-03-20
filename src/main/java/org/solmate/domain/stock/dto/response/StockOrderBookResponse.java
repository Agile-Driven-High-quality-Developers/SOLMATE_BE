package org.solmate.domain.stock.dto.response;

import java.util.List;

import org.solmate.external.ls.dto.websocket.LsWsOrderBookResponse;

public record StockOrderBookResponse(
        String stockCode,
        long currentPrice,
        double changeRate,
        List<PriceLevel> sellLevels,
        List<PriceLevel> buyLevels,
        String timestamp
) {
    public record PriceLevel(long price, long quantity) {}

    public static StockOrderBookResponse from(LsWsOrderBookResponse.Body body, long currentPrice, double changeRate) {
        List<PriceLevel> sellLevels = List.of(
                new PriceLevel(parseLong(body.offerho1()), parseLong(body.unt_offerrem1())),
                new PriceLevel(parseLong(body.offerho2()), parseLong(body.unt_offerrem2())),
                new PriceLevel(parseLong(body.offerho3()), parseLong(body.unt_offerrem3())),
                new PriceLevel(parseLong(body.offerho4()), parseLong(body.unt_offerrem4())),
                new PriceLevel(parseLong(body.offerho5()), parseLong(body.unt_offerrem5()))
        );

        List<PriceLevel> buyLevels = List.of(
                new PriceLevel(parseLong(body.bidho1()), parseLong(body.unt_bidrem1())),
                new PriceLevel(parseLong(body.bidho2()), parseLong(body.unt_bidrem2())),
                new PriceLevel(parseLong(body.bidho3()), parseLong(body.unt_bidrem3())),
                new PriceLevel(parseLong(body.bidho4()), parseLong(body.unt_bidrem4())),
                new PriceLevel(parseLong(body.bidho5()), parseLong(body.unt_bidrem5()))
        );

        return new StockOrderBookResponse(body.shcode(), currentPrice, changeRate, sellLevels, buyLevels, body.hotime());
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        return Long.parseLong(value.trim());
    }
}
