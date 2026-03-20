package org.solmate.external.ls.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsWsStockResponse(Header header, Body body) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String tr_cd, String tr_key) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            String shcode,      // 단축코드
            String chetime,     // 체결시간 (HHMMSS)
            String price,       // 현재가(체결가)
            String cvolume,     // 체결량
            String volume,      // 누적거래량
            String value,       // 누적거래대금
            String open,        // 시가
            String high,        // 고가
            String low,         // 저가
            String sign,        // 전일대비구분
            String change,      // 전일대비
            String drate        // 등락율
    ) {}
}
