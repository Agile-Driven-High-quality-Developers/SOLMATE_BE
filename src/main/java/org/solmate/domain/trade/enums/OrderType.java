package org.solmate.domain.trade.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {
    MARKET("시장가"),
    LIMIT("지정가");

    private final String label;
}
