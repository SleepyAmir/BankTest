package com.springbank.analytics.controller;

import com.springbank.analytics.dto.SpendingSnapshotDto;
import com.springbank.analytics.service.AnalyticsService;
import com.springbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<SpendingSnapshotDto>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User analytics", analyticsService.getByUserId(userId), "/api/analytics/user/" + userId));
    }

    @GetMapping("/user/{userId}/month/{month}")
    public ResponseEntity<ApiResponse<SpendingSnapshotDto>> getByMonth(@PathVariable Long userId, @PathVariable YearMonth month) {
        return ResponseEntity.ok(ApiResponse.success("Monthly snapshot", analyticsService.getByUserAndMonth(userId, month), "/api/analytics/user/" + userId + "/month/" + month));
    }
}
