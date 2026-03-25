package org.solmate.domain.account.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.account.dto.response.AccountSummaryResponse;
import org.solmate.domain.account.service.AccountService;
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

    @Operation(summary = "내 계좌 요약 조회", description = "보유 총 자산, 보유 현금, 보유 종목 수, 수익률 카드 데이터를 조회합니다.")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> getSummary(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessStatus.ACCOUNT_SUMMARY_SUCCESS, accountService.getSummary(userId));
    }
}
