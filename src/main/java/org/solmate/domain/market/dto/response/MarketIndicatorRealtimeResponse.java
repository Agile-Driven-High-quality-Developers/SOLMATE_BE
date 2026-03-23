package org.solmate.domain.market.dto.response;

import org.solmate.external.ls.dto.websocket.LsWsCurrencyResponse;
import org.solmate.external.ls.dto.websocket.LsWsIndexResponse;

public record MarketIndicatorRealtimeResponse(
        String type,        // "KOSPI", "KOSDAQ", "USD_KRW"
        double current,     // 현재 지수/환율
        double change,      // 전일 대비
        double changeRate,  // 등락률
        String sign,        // 전일대비구분 (1=상한, 2=상승, 3=보합, 4=하한, 5=하락)
        double high,        // 당일 고가
        double low,         // 당일 저가
        String asOf         // 시간 (HHMMSS)
) {
    public static MarketIndicatorRealtimeResponse fromIndex(LsWsIndexResponse response) {
        String type = "001".equals(response.header().tr_key()) ? "KOSPI" : "KOSDAQ";
        LsWsIndexResponse.Body body = response.body();
        return new MarketIndicatorRealtimeResponse(
                type,
                Double.parseDouble(body.jisu().trim()),
                Double.parseDouble(body.change().trim()),
                Double.parseDouble(body.drate().trim()),
                body.sign(),
                Double.parseDouble(body.highjisu().trim()),
                Double.parseDouble(body.lowjisu().trim()),
                body.time()
        );
    }

    public static MarketIndicatorRealtimeResponse fromCurrency(LsWsCurrencyResponse response) {
        LsWsCurrencyResponse.Body body = response.body();
        return new MarketIndicatorRealtimeResponse(
                "USD_KRW",
                Double.parseDouble(body.price().trim()),
                Double.parseDouble(body.change().trim()),
                Double.parseDouble(body.drate().trim()),
                body.sign(),
                Double.parseDouble(body.high().trim()),
                Double.parseDouble(body.low().trim()),
                body.time()
        );
    }
}