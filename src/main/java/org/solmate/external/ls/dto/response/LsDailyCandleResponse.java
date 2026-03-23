package org.solmate.external.ls.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsDailyCandleResponse(
        @JsonProperty("t8410OutBlock") OutBlock t8410OutBlock,
        @JsonProperty("t8410OutBlock1") List<OutBlock1> t8410OutBlock1
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock(
            String cts_date
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock1(
            String date,        // 날짜 (YYYYMMDD)
            long open,          // 시가
            long high,          // 고가
            long low,           // 저가
            long close,         // 종가
            long jdiff_vol      // 거래량
    ) {}

    public boolean hasNext() {
        return t8410OutBlock != null
                && t8410OutBlock.cts_date() != null
                && !t8410OutBlock.cts_date().isBlank();
    }
}
