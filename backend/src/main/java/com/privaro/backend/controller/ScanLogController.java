package com.privaro.backend.controller;

import com.privaro.backend.dto.ScanLogDto.*;
import com.privaro.backend.entity.User;
import com.privaro.backend.service.ScanLogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class ScanLogController {

    private static final Logger logger = LoggerFactory.getLogger(ScanLogController.class);

    private final ScanLogService scanLogService;

    public ScanLogController(ScanLogService scanLogService) {
        this.scanLogService = scanLogService;
    }

    @PostMapping
    public ResponseEntity<ScanLogResponse> createLog(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ScanLogRequest request) {
        logger.info("Create log request from user: {}", user.getEmail());
        ScanLogResponse response = scanLogService.createLog(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchLogResponse> createLogs(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody List<ScanLogRequest> requests) {
        logger.info("Batch create log request from user: {} ({} logs)", user.getEmail(), requests.size());
        BatchLogResponse response = scanLogService.createLogs(user, requests);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ScanLogListResponse> getLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        logger.info("Get logs request from user: {} (limit: {}, offset: {})", user.getEmail(), limit, offset);
        ScanLogListResponse response = scanLogService.getUserLogs(user, limit, offset);
        return ResponseEntity.ok(response);
    }
}
