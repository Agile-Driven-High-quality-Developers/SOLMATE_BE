package org.solmate.external.ls.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsWsIndexResponse(Header header, Body body) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String tr_cd, String tr_key) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            String time,        // 시간 (HHMMSS)
            String jisu,        // 현재 지수
            String sign,        // 전일대비구분
            String change,      // 전일비
            String drate,       // 등락률
            String highjisu,    // 고가지수
            String lowjisu      // 저가지수
    ) {}
}