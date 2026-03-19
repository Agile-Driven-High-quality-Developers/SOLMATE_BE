package org.solmate.domain.stock.service;

import org.solmate.domain.stock.dto.response.StockQuoteResponse;
import org.solmate.external.ls.client.LsApiClient;
import org.solmate.external.ls.dto.response.LsQuoteResponse;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

    private final LsApiClient lsApiClient;

    public StockQuoteResponse getQuote(String stockCode) {
        LsQuoteResponse response = lsApiClient.getQuote(stockCode);
        return StockQuoteResponse.from(response);
    }
}
