package org.solmate.external.ls.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LsUnifiedDailyCandleResponse(
        @JsonProperty("t8451OutBlock")  OutBlock t8451OutBlock,
        @JsonProperty("t8451OutBlock1") List<OutBlock1> t8451OutBlock1
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock(
            String cts_date
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutBlock1(
            String date,
            long open,
            long high,
            long low,
            long close,
            long jdiff_vol
    ) {}

    public boolean hasNext() {
        return t8451OutBlock != null
                && t8451OutBlock.cts_date() != null
                && !t8451OutBlock.cts_date().isBlank();
    }

    public List<OutBlock1> candles() {
        return t8451OutBlock1 != null ? t8451OutBlock1 : List.of();
    }
}
