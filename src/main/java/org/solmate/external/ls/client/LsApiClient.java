package org.solmate.external.ls.client;

import java.util.function.Function;

import org.solmate.external.ls.LsProperties;
import org.solmate.external.ls.dto.request.LsQuoteRequest;
import org.solmate.external.ls.dto.request.LsUnifiedDailyCandleRequest;
import org.solmate.external.ls.dto.request.LsUnifiedMinuteCandleRequest;
import org.solmate.external.ls.dto.response.LsQuoteResponse;
import org.solmate.external.ls.dto.response.LsUnifiedDailyCandleResponse;
import org.solmate.external.ls.dto.response.LsUnifiedMinuteCandleResponse;
import org.solmate.external.ls.service.LsTokenService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsApiClient {

    private final LsProperties lsProperties;
    private final LsTokenService lsTokenService;
    private final RestClient restClient = RestClient.create();

    private <T> T withTokenRetry(Function<String, T> call) {
        try {
            return call.apply(lsTokenService.getToken());
        } catch (HttpServerErrorException e) {
            if (e.getResponseBodyAsString().contains("IGW00121")) {
                log.warn("LS 토큰 무효화 감지 - 재발급 후 재시도");
                return call.apply(lsTokenService.refreshToken());
            }
            throw e;
        }
    }

    public LsQuoteResponse getQuote(String stockCode) {
        return withTokenRetry(token -> restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/market-data")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + token)
                .header("tr_cd", "t1102")
                .header("tr_cont", "N")
                .body(LsQuoteRequest.of(stockCode))
                .retrieve()
                .body(LsQuoteResponse.class));
    }

    /** t8452: 통합 N분봉 조회 (ncnt: 1/5/30/60 등) */
    public LsUnifiedMinuteCandleResponse getUnifiedMinuteCandles(String stockCode, int ncnt, String sdate, String edate,
                                                                   String exchgubun) {
        return withTokenRetry(token -> restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + token)
                .header("tr_cd", "t8452")
                .header("tr_cont", "N")
                .body(LsUnifiedMinuteCandleRequest.of(stockCode, ncnt, sdate, edate, exchgubun))
                .retrieve()
                .body(LsUnifiedMinuteCandleResponse.class));
    }

    /** t8452: 통합 N분봉 연속 조회 */
    public LsUnifiedMinuteCandleResponse getUnifiedMinuteCandlesContinue(String stockCode, int ncnt, String sdate, String edate,
                                                                           String ctsDate, String ctsTime,
                                                                           String exchgubun) {
        return withTokenRetry(token -> restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + token)
                .header("tr_cd", "t8452")
                .header("tr_cont", "Y")
                .header("tr_cont_key", ctsDate + ctsTime)
                .body(LsUnifiedMinuteCandleRequest.ofContinue(stockCode, ncnt, sdate, edate, ctsDate, ctsTime, exchgubun))
                .retrieve()
                .body(LsUnifiedMinuteCandleResponse.class));
    }

    /** t8451: 통합 일/주/월봉 조회 (gubun: 2=일, 3=주, 4=월) */
    public LsUnifiedDailyCandleResponse getUnifiedDailyCandles(String stockCode, String gubun, String sdate, String edate) {
        return withTokenRetry(token -> restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + token)
                .header("tr_cd", "t8451")
                .header("tr_cont", "N")
                .body(LsUnifiedDailyCandleRequest.of(stockCode, gubun, sdate, edate))
                .retrieve()
                .body(LsUnifiedDailyCandleResponse.class));
    }

    /** t8451: 통합 일/주/월봉 연속 조회 */
    public LsUnifiedDailyCandleResponse getUnifiedDailyCandlesContinue(String stockCode, String gubun, String sdate, String edate,
                                                                        String ctsDate) {
        return withTokenRetry(token -> restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + token)
                .header("tr_cd", "t8451")
                .header("tr_cont", "Y")
                .header("tr_cont_key", ctsDate)
                .body(LsUnifiedDailyCandleRequest.ofContinue(stockCode, gubun, sdate, edate, ctsDate))
                .retrieve()
                .body(LsUnifiedDailyCandleResponse.class));
    }

}