package org.solmate.external.ls.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.external.ls.service.LsTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class LsTokenTestController {

    private final LsTokenService lsTokenService;

    @GetMapping("/ls-token")
    public ResponseEntity<ApiResponse<String>> getToken() {
        String token = lsTokenService.getToken();
        return ApiResponse.success(SuccessStatus.SUCCESS_200, token);
    }
}
