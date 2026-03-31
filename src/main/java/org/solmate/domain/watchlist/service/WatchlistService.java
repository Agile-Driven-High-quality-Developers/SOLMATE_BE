package org.solmate.domain.watchlist.service;

import lombok.RequiredArgsConstructor;
import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.stock.repository.StockRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.solmate.domain.watchlist.entity.Watchlist;
import org.solmate.domain.watchlist.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    @Transactional
    public void addWatchlist(Long userId, String tickerCode) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Stock stock = stockRepository.findByTickerCode(tickerCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.STOCK_NOT_FOUND));

        if (watchlistRepository.existsByUserAndStock(user, stock)) {
            throw new GeneralException(ErrorStatus.WATCHLIST_ALREADY_EXISTS);
        }

        watchlistRepository.save(Watchlist.builder()
                .user(user)
                .stock(stock)
                .build());
    }

    @Transactional
    public void removeWatchlist(Long userId, String tickerCode) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Stock stock = stockRepository.findByTickerCode(tickerCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.STOCK_NOT_FOUND));

        Watchlist watchlist = watchlistRepository.findByUserAndStock(user, stock)
                .orElseThrow(() -> new GeneralException(ErrorStatus.WATCHLIST_NOT_FOUND));

        watchlistRepository.delete(watchlist);
    }

    public List<String> getWatchlist(Long userId) {
        return watchlistRepository.findTickerCodesByUserId(userId);
    }
}
