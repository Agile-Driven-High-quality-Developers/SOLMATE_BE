package org.solmate.external.ls.client;

import org.solmate.external.ls.LsProperties;
import org.solmate.external.ls.dto.request.LsQuoteRequest;
import org.solmate.external.ls.dto.response.LsQuoteResponse;
import org.solmate.external.ls.service.LsTokenService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LsApiClient {

    private final LsProperties lsProperties;
    private final LsTokenService lsTokenService;
    private final RestClient restClient = RestClient.create();

    public LsQuoteResponse getQuote(String stockCode) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/market-data")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + lsTokenService.getToken())
                .header("tr_cd", "t1102")
                .header("tr_cont", "N")
                .body(LsQuoteRequest.of(stockCode))
                .retrieve()
                .body(LsQuoteResponse.class);
    }
}
