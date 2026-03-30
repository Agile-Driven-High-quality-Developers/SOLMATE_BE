package org.solmate.external.ls.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LsUnifiedDailyCandleRequest(
        @JsonProperty("t8451InBlock") InBlock t8451InBlock
) {
    public record InBlock(
            String shcode,
            String gubun,
            int qrycnt,
            String sdate,
            String edate,
            String cts_date,
            String comp_yn,
            String sujung,
            String exchgubun
    ) {}

    public static LsUnifiedDailyCandleRequest of(String shcode, String sdate, String edate) {
        return new LsUnifiedDailyCandleRequest(new InBlock(
                shcode,
                "2",     // 일봉
                500,
                sdate,
                edate,
                " ",
                "N",     // 비압축
                "Y",     // 수정주가 적용
                "U"      // 통합 (KRX+NXT)
        ));
    }

    public static LsUnifiedDailyCandleRequest ofContinue(String shcode, String sdate, String edate,
                                                          String ctsDate) {
        return new LsUnifiedDailyCandleRequest(new InBlock(
                shcode,
                "2",
                500,
                sdate,
                edate,
                ctsDate,
                "N",
                "Y",
                "U"
        ));
    }
}
