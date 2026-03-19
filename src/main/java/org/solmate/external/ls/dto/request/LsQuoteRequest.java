package org.solmate.external.ls.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LsQuoteRequest(
        @JsonProperty("t1102InBlock") InBlock t1102InBlock
) {
    public record InBlock(String shcode) {}

    public static LsQuoteRequest of(String shcode) {
        return new LsQuoteRequest(new InBlock(shcode));
    }
}
