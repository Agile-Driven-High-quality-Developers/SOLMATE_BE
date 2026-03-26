package org.solmate.external.ls.client;

import org.solmate.external.ls.LsProperties;
import org.solmate.external.ls.dto.request.LsDailyCandleRequest;
import org.solmate.external.ls.dto.request.LsMinuteCandleRequest;
import org.solmate.external.ls.dto.request.LsQuoteRequest;
import org.solmate.external.ls.dto.request.LsUnifiedMinuteCandleRequest;
import org.solmate.external.ls.dto.response.LsDailyCandleResponse;
import org.solmate.external.ls.dto.response.LsMinuteCandleResponse;
import org.solmate.external.ls.dto.response.LsQuoteResponse;
import org.solmate.external.ls.dto.response.LsUnifiedMinuteCandleResponse;
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

    /** t8412: 1분봉 조회 (처음 조회) */
    public LsMinuteCandleResponse getMinuteCandles(String stockCode, String sdate, String edate) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + lsTokenService.getToken())
                .header("tr_cd", "t8412")
                .header("tr_cont", "N")
                .body(LsMinuteCandleRequest.of(stockCode, sdate, edate))
                .retrieve()
                .body(LsMinuteCandleResponse.class);
    }

    /** t8412: 1분봉 연속 조회 */
    public LsMinuteCandleResponse getMinuteCandlesContinue(String stockCode, String sdate, String edate,
                                                            String ctsDate, String ctsTime) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + lsTokenService.getToken())
                .header("tr_cd", "t8412")
                .header("tr_cont", "Y")
                .header("tr_cont_key", ctsDate + ctsTime)
                .body(LsMinuteCandleRequest.ofContinue(stockCode, sdate, edate, ctsDate, ctsTime))
                .retrieve()
                .body(LsMinuteCandleResponse.class);
    }

    /** t8452: 통합 1분봉 조회 (KRX+NXT, exchgubun: K/N/U) */
    public LsUnifiedMinuteCandleResponse getUnifiedMinuteCandles(String stockCode, String sdate, String edate,
                                                                   String exchgubun) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + lsTokenService.getToken())
                .header("tr_cd", "t8452")
                .header("tr_cont", "N")
                .body(LsUnifiedMinuteCandleRequest.of(stockCode, sdate, edate, exchgubun))
                .retrieve()
                .body(LsUnifiedMinuteCandleResponse.class);
    }

    /** t8452: 통합 1분봉 연속 조회 */
    public LsUnifiedMinuteCandleResponse getUnifiedMinuteCandlesContinue(String stockCode, String sdate, String edate,
                                                                           String ctsDate, String ctsTime,
                                                                           String exchgubun) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + lsTokenService.getToken())
                .header("tr_cd", "t8452")
                .header("tr_cont", "Y")
                .header("tr_cont_key", ctsDate + ctsTime)
                .body(LsUnifiedMinuteCandleRequest.ofContinue(stockCode, sdate, edate, ctsDate, ctsTime, exchgubun))
                .retrieve()
                .body(LsUnifiedMinuteCandleResponse.class);
    }

    /** t8410: 일봉 조회 (처음 조회) */
    public LsDailyCandleResponse getDailyCandles(String stockCode, String sdate, String edate) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + lsTokenService.getToken())
                .header("tr_cd", "t8410")
                .header("tr_cont", "N")
                .body(LsDailyCandleRequest.of(stockCode, sdate, edate))
                .retrieve()
                .body(LsDailyCandleResponse.class);
    }

    /** t8410: 일봉 연속 조회 */
    public LsDailyCandleResponse getDailyCandlesContinue(String stockCode, String sdate, String edate,
                                                          String ctsDate) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/stock/chart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + lsTokenService.getToken())
                .header("tr_cd", "t8410")
                .header("tr_cont", "Y")
                .header("tr_cont_key", ctsDate)
                .body(LsDailyCandleRequest.ofContinue(stockCode, sdate, edate, ctsDate))
                .retrieve()
                .body(LsDailyCandleResponse.class);
    }
}
