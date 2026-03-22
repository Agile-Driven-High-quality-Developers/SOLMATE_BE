package org.solmate.domain.trade.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeDiaryStatus {
    PENDING("대기"),
    FILLED("체결");

    private final String label;
}
