package com.springbank.fraud.controller;

import com.springbank.common.dto.ApiResponse;
import com.springbank.fraud.dto.FraudAlertDto;
import com.springbank.fraud.dto.AmlAlertDto;
import com.springbank.fraud.service.FraudAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAnalysisService fraudAnalysisService;

    @GetMapping("/alerts/user/{userId}")
    public ResponseEntity<ApiResponse<List<FraudAlertDto>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Fraud alerts", fraudAnalysisService.getFraudAlertsByUser(userId), "/api/fraud/alerts/user/" + userId));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<FraudAlertDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All fraud alerts", fraudAnalysisService.getAllFraudAlerts(), "/api/fraud/alerts"));
    }

    @GetMapping("/alerts/blocked")
    public ResponseEntity<ApiResponse<List<FraudAlertDto>>> getBlocked() {
        return ResponseEntity.ok(ApiResponse.success("Blocked transactions",
                fraudAnalysisService.getBlockedAlerts(), "/api/fraud/alerts/blocked"));
    }

    @GetMapping("/aml/user/{userId}")
    public ResponseEntity<ApiResponse<List<AmlAlertDto>>> getAmlByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("AML alerts", fraudAnalysisService.getAmlAlertsByUser(userId), "/api/fraud/aml/user/" + userId));
    }

    @PostMapping("/alerts/{id}/review")
    public ResponseEntity<ApiResponse<FraudAlertDto>> review(@PathVariable Long id, @RequestParam String reviewedBy, @RequestParam String note, @RequestParam boolean confirmed) {
        return ResponseEntity.ok(ApiResponse.success("Fraud alert reviewed", fraudAnalysisService.reviewFraudAlert(id, reviewedBy, note, confirmed), "/api/fraud/alerts/" + id + "/review"));
    }
}
