package org.solmate.domain.account.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.account.dto.response.AccountSummaryResponse;
import org.solmate.domain.account.service.AccountService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Account", description = "내 계좌 API")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "내 계좌 요약 조회",
            description = """
                    보유 총 자산·현금·종목 수·수익률 카드와 종목 비중(도넛 차트) 데이터를 한 번에 조회합니다.

                    **클라이언트 권장:** 내 계좌 화면에서는 약 10초 간격 폴링으로 갱신해도 무방합니다. 주문·체결 등 거래 직후에는 즉시 재조회하여 수치를 맞추는 것을 권장합니다.

                    응답은 개인 자산 정보이므로 캐시하지 않습니다 (Cache-Control: no-store).""")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> getSummary(
            @AuthenticationPrincipal Long userId) {
        ResponseEntity<ApiResponse<AccountSummaryResponse>> entity = ApiResponse.success(
                SuccessStatus.ACCOUNT_SUMMARY_SUCCESS,
                accountService.getSummary(userId));
        return ResponseEntity.status(entity.getStatusCode())
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(entity.getBody());
    }
}
