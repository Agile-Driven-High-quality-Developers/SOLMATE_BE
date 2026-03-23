package org.solmate.domain.trade.service;

import java.math.BigDecimal;
import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.trade.dto.response.HoldingsResponse;
import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.trade.repository.HoldingsRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingsService {

    private final HoldingsRepository holdingsRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public List<HoldingsResponse> getHoldings(Long userId) {
        List<Holdings> holdings = holdingsRepository.findByUserId(userId);

        return holdings.stream()
            .map(h -> HoldingsResponse.of(h, getCurrentPrice(h.getTickerCode())))
            .toList();
    }

    private BigDecimal getCurrentPrice(String ticker) {
        String curStr = (String) redisTemplate.opsForHash().get("stock:info:" + ticker, "cur");
        if (curStr == null) throw new GeneralException(ErrorStatus.STOCK_PRICE_NOT_FOUND);
        return new BigDecimal(curStr);
    }
}
