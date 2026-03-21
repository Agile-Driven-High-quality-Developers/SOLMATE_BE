package org.solmate.domain.trade.dto.request;

import java.math.BigDecimal;

import org.solmate.domain.trade.enums.OrderType;

public record BuyOrderRequest(
    String ticker,       // 종목코드 (ex: "005930")
    OrderType orderType, // 주문 유형 (MARKET: 시장가, LIMIT: 지정가)
    BigDecimal price,    // 주문 가격 (지정가일 때만 사용, 시장가면 무시)
    BigDecimal quantity, // 주문 수량
    String diary         // 매매일지
) {}
