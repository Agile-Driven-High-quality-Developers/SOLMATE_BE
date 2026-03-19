package org.solmate.external.ls.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LsQuoteResponse(
        @JsonProperty("t1102OutBlock") OutBlock t1102OutBlock
) {
    public record OutBlock(
            String shcode,      // 단축코드
            String hname,       // 종목명
            long price,         // 현재가
            String sign,        // 전일대비구분 (1:상한 2:상승 3:보합 4:하한 5:하락)
            long change,        // 전일대비
            double diff,        // 등락율
            long recprice,      // 기준가(전일종가)
            long open,          // 시가
            long high,          // 고가
            long low,           // 저가
            long volume,        // 누적거래량
            long total          // 시가총액
    ) {}
}
