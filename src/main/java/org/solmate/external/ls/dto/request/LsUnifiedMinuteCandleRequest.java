package org.solmate.external.ls.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LsUnifiedMinuteCandleRequest(
        @JsonProperty("t8452InBlock") InBlock t8452InBlock
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
            String comp_yn,
            String exchgubun
    ) {}

    public static LsUnifiedMinuteCandleRequest of(String shcode, int ncnt, String sdate, String edate, String exchgubun) {
        return new LsUnifiedMinuteCandleRequest(new InBlock(
                shcode,
                ncnt,
                500,
                "0",
                sdate,
                "000000",
                edate,
                "235959",
                " ",
                " ",
                "N",
                exchgubun
        ));
    }

    public static LsUnifiedMinuteCandleRequest ofContinue(String shcode, int ncnt, String sdate, String edate,
                                                           String ctsDate, String ctsTime, String exchgubun) {
        return new LsUnifiedMinuteCandleRequest(new InBlock(
                shcode,
                ncnt,
                500,
                "0",
                sdate,
                "000000",
                edate,
                "235959",
                ctsDate,
                ctsTime,
                "N",
                exchgubun
        ));
    }
}
