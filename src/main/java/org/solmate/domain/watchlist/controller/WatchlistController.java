package org.solmate.domain.watchlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.watchlist.service.WatchlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Watchlist", description = "관심종목 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @Operation(summary = "관심종목 추가")
    @PostMapping("/{tickerCode}")
    public ResponseEntity<ApiResponse<Void>> addWatchlist(
            @AuthenticationPrincipal Long userId,
            @PathVariable String tickerCode
    ) {
        watchlistService.addWatchlist(userId, tickerCode);
        return ApiResponse.success(SuccessStatus.WATCHLIST_ADD_SUCCESS);
    }

    @Operation(summary = "관심종목 제거")
    @DeleteMapping("/{tickerCode}")
    public ResponseEntity<ApiResponse<Void>> removeWatchlist(
            @AuthenticationPrincipal Long userId,
            @PathVariable String tickerCode
    ) {
        watchlistService.removeWatchlist(userId, tickerCode);
        return ApiResponse.success(SuccessStatus.WATCHLIST_REMOVE_SUCCESS);
    }

    @Operation(summary = "내 관심종목 ticker 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getWatchlist(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(SuccessStatus.WATCHLIST_GET_SUCCESS, watchlistService.getWatchlist(userId));
    }
}
