package org.solmate.external.ls.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LsMinuteCandleRequest(
        @JsonProperty("t8412InBlock") InBlock t8412InBlock
) {
    public record InBlock(
            String shcode,
            int ncnt,
            int qrycnt,
            String nday,
            String sdate,
            String stime,
            String edate,
            String etime,
            String cts_date,
            String cts_time,
            String comp_yn
    ) {}

    public static LsMinuteCandleRequest of(String shcode, String sdate, String edate) {
        return new LsMinuteCandleRequest(new InBlock(
                shcode,
                1,       // 1분봉
                500,     // 비압축 최대
                "0",     // nday 미사용
                sdate,
                "000000",
                edate,
                "235959",
                " ",
                " ",
                "N"      // 비압축
        ));
    }

    /** 연속조회용 */
    public static LsMinuteCandleRequest ofContinue(String shcode, String sdate, String edate,
                                                    String ctsDate, String ctsTime) {
        return new LsMinuteCandleRequest(new InBlock(
                shcode,
                1,
                500,
                "0",
                sdate,
                "000000",
                edate,
                "235959",
                ctsDate,
                ctsTime,
                "N"
        ));
    }
}
