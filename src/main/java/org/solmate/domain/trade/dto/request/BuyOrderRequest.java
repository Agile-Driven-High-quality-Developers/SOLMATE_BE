package org.solmate.domain.trade.dto.request;

import java.math.BigDecimal;

public record BuyOrderRequest(
    String ticker,       // 종목코드 (ex: "005930")
    String orderType,    // 주문 유형 ("MARKET": 시장가, "LIMIT": 지정가)
    BigDecimal price,    // 주문 가격 (지정가일 때만 사용, 시장가면 무시)
    BigDecimal quantity, // 주문 수량
    String diary         // 매매일지
) {}
