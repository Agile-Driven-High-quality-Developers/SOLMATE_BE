package org.solmate.domain.stock.dto.response;

import org.solmate.external.ls.dto.response.LsQuoteResponse;

public record StockQuoteResponse(
        String stockCode,
        String stockName,
        String stockLogo,
        long currentPrice,
        long changePrice,
        double changeRate,
        long previousClosePrice,
        long openPrice,
        long highPrice,
        long lowPrice,
        long volume,
        long total
) {
    public static StockQuoteResponse from(LsQuoteResponse response, String stockLogo) {
        LsQuoteResponse.OutBlock block = response.t1102OutBlock();
        return new StockQuoteResponse(
                block.shcode(),
                block.hname(),
                stockLogo,
                block.price(),
                block.change(),
                block.diff(),
                block.recprice(),
                block.open(),
                block.high(),
                block.low(),
                block.volume(),
                block.total()
        );
    }
}
