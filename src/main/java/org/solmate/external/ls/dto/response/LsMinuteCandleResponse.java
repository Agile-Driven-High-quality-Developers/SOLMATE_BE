package org.solmate.external.ls.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsMinuteCandleResponse(
        @JsonProperty("t8412OutBlock") OutBlock t8412OutBlock,
        @JsonProperty("t8412OutBlock1") List<OutBlock1> t8412OutBlock1
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock(
            String cts_date,
            String cts_time
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock1(
            String date,        // 날짜 (YYYYMMDD)
            String time,        // 시간 (HHMMSS)
            long open,          // 시가
            long high,          // 고가
            long low,           // 저가
            long close,         // 종가
            long jdiff_vol      // 거래량
    ) {}

    public boolean hasNext() {
        return t8412OutBlock != null
                && t8412OutBlock.cts_date() != null
                && !t8412OutBlock.cts_date().isBlank();
    }
}
