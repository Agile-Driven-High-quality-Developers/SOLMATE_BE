package org.solmate.external.ls.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsUnifiedMinuteCandleResponse(
        @JsonProperty("t8452OutBlock")  OutBlock t8452OutBlock,
        @JsonProperty("t8452OutBlock1") List<OutBlock1> t8452OutBlock1
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock(
            String cts_date,
            String cts_time,
            String nxt_fm_s_time,   // NXT 프리마켓 시작 (HHMMSS)
            String nxt_fm_e_time,   // NXT 프리마켓 종료 (HHMMSS)
            String nxt_am_s_time,   // NXT 에프터마켓 시작 (HHMMSS)
            String nxt_am_e_time    // NXT 에프터마켓 종료 (HHMMSS)
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock1(
            String date,
            String time,
            long open,
            long high,
            long low,
            long close,
            long jdiff_vol
    ) {}

    public boolean hasNext() {
        return t8452OutBlock != null
                && t8452OutBlock.cts_date() != null
                && !t8452OutBlock.cts_date().isBlank();
    }

    public List<OutBlock1> candles() {
        return t8452OutBlock1 != null ? t8452OutBlock1 : List.of();
    }
}
