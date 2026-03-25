package org.solmate.domain.account.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.account.dto.response.AccountSummaryResponse;
import org.solmate.domain.account.service.AccountService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Account", description = "계좌 요약 API (본인·타인 조회)")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "내 계좌 요약 조회",
            description = """
                    로그인한 사용자 본인의 보유 총 자산·현금·종목 수·수익률 카드와 종목 비중(도넛 차트) 데이터를 한 번에 조회합니다.

                    **클라이언트 권장:** 내 계좌 화면에서는 약 10초 간격 폴링으로 갱신해도 무방합니다. 주문·체결 등 거래 직후에는 즉시 재조회하여 수치를 맞추는 것을 권장합니다.

                    응답은 개인 자산 정보이므로 캐시하지 않습니다 (Cache-Control: no-store).""")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> getSummary(
            @AuthenticationPrincipal Long userId) {
        return accountSummaryResponse(userId);
    }

    @Operation(
            summary = "타인 계좌 요약 조회",
            description = """
                    특정 사용자의 계좌 요약을 조회합니다. 응답 스키마는 `GET /api/account/summary`(본인)와 동일합니다.

                    보유 종목 API(`GET /api/holdings/{userId}`)와 동일하게 프로필·포트폴리오 탭 등에서 타인 정보 표시에 사용할 수 있습니다.""")
    @GetMapping("/summary/{userId}")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> getOtherUserSummary(
            @PathVariable Long userId) {
        return accountSummaryResponse(userId);
    }

    private ResponseEntity<ApiResponse<AccountSummaryResponse>> accountSummaryResponse(Long targetUserId) {
        ResponseEntity<ApiResponse<AccountSummaryResponse>> entity = ApiResponse.success(
                SuccessStatus.ACCOUNT_SUMMARY_SUCCESS,
                accountService.getSummary(targetUserId));
        return ResponseEntity.status(entity.getStatusCode())
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(entity.getBody());
    }
}
