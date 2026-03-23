package org.solmate.external.ls.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LsDailyCandleRequest(
        @JsonProperty("t8410InBlock") InBlock t8410InBlock
) {
    public record InBlock(
            String shcode,
            String gubun,
            int qrycnt,
            String sdate,
            String edate,
            String cts_date,
            String comp_yn,
            String sujung
    ) {}

    public static LsDailyCandleRequest of(String shcode, String sdate, String edate) {
        return new LsDailyCandleRequest(new InBlock(
                shcode,
                "2",     // 일봉
                500,     // 비압축 최대
                sdate,
                edate,
                " ",
                "N",     // 비압축 (OPENAPI 압축 미제공)
                "Y"      // 수정주가 적용
        ));
    }

    /** 연속조회용 */
    public static LsDailyCandleRequest ofContinue(String shcode, String sdate, String edate,
                                                   String ctsDate) {
        return new LsDailyCandleRequest(new InBlock(
                shcode,
                "2",
                500,
                sdate,
                edate,
                ctsDate,
                "N",
                "Y"
        ));
    }
}
