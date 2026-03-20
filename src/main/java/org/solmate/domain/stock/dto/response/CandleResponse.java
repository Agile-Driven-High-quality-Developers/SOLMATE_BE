package org.solmate.domain.stock.dto.response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.solmate.domain.stock.entity.BaseCandle;

public record CandleResponse(
        long time,
        long open,
        long high,
        long low,
        long close,
        long volume
) {
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    /** DB에서 조회한 MinuteCandle / DailyCandle 변환용 */
    public static CandleResponse from(BaseCandle candle, LocalDateTime candleTime) {
        return new CandleResponse(
                candleTime.toEpochSecond(KST),
                candle.getOpenPrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getClosePrice(),
                candle.getVolume()
        );
    }

    /** Redis에서 꺼낸 현재 진행 중인 봉 변환용 */
    public static CandleResponse fromRedis(Map<Object, Object> data, LocalDateTime candleTime) {
        return new CandleResponse(
                candleTime.toEpochSecond(KST),
                Long.parseLong((String) data.get("open")),
                Long.parseLong((String) data.get("high")),
                Long.parseLong((String) data.get("low")),
                Long.parseLong((String) data.get("close")),
                Long.parseLong((String) data.get("volume"))
        );
    }
}
