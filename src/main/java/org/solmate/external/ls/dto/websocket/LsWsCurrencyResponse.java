package org.solmate.external.ls.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsWsCurrencyResponse(Header header, Body body) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String tr_cd, String tr_key) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            String time,    // 시간 (HHMMSS)
            String price,   // 현재 환율
            String sign,    // 전일대비구분
            String change,  // 전일 대비
            String drate,   // 등락률
            String high,    // 당일 고가
            String low      // 당일 저가
    ) {}
}