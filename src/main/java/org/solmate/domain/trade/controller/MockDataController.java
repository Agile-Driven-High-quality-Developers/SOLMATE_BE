package org.solmate.domain.trade.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.trade.service.MockDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class MockDataController {

    private final MockDataService mockDataService;

    @PostMapping("/mock-stock-data")
    public ResponseEntity<ApiResponse<Void>> initMockStockData() {
        mockDataService.initMockStockData();
        return ApiResponse.success(SuccessStatus.MOCK_DATA_INIT_SUCCESS);
    }
}
