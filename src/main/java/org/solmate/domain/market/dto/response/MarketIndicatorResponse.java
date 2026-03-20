package org.solmate.domain.market.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarketIndicatorResponse {

    private IndexInfo kospi;
    private IndexInfo kosdaq;
    private IndexInfo usdKrw;

    @Getter
    @Builder
    public static class IndexInfo {
        private String cur;     // 현재 지수
        private String change;  // 전일 대비 (pt)
        private String rate;    // 등락률 (%)
        private String sign;    // 부호 (1=상한, 2=상승, 3=보합, 4=하한, 5=하락)
        private String high;    // 당일 고가
        private String low;     // 당일 저가
        private String asOf;    // 업데이트 시간 (HHMMSS)
    }
}