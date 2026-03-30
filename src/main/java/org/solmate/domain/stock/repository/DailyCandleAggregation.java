package org.solmate.domain.stock.repository;

import java.time.LocalDate;

public interface DailyCandleAggregation {
    LocalDate getCandleDate();
    long getOpenPrice();
    long getHighPrice();
    long getLowPrice();
    long getClosePrice();
    long getVolume();
}
