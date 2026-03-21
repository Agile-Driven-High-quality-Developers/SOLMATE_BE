package org.solmate.domain.trade.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeStatus {
    PENDING("대기"),
    FILLED("체결"),
    CANCELLED("취소");

    private final String label;
}
