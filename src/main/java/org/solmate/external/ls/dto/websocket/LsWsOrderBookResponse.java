package org.solmate.external.ls.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsWsOrderBookResponse(Header header, Body body) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String tr_cd, String tr_key) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            String shcode,
            String hotime,
            // 매도호가 1~5
            String offerho1, String offerho2, String offerho3, String offerho4, String offerho5,
            // 통합 매도호가잔량 1~5
            String unt_offerrem1, String unt_offerrem2, String unt_offerrem3, String unt_offerrem4, String unt_offerrem5,
            // 매수호가 1~5
            String bidho1, String bidho2, String bidho3, String bidho4, String bidho5,
            // 통합 매수호가잔량 1~5
            String unt_bidrem1, String unt_bidrem2, String unt_bidrem3, String unt_bidrem4, String unt_bidrem5
    ) {}
}
