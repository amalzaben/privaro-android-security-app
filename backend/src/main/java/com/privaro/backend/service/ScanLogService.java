package com.privaro.backend.service;

import com.privaro.backend.dto.ScanLogDto.*;
import com.privaro.backend.entity.ScanLog;
import com.privaro.backend.entity.User;
import com.privaro.backend.repository.ScanLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScanLogService {

    private static final Logger logger = LoggerFactory.getLogger(ScanLogService.class);

    private final ScanLogRepository scanLogRepository;

    public ScanLogService(ScanLogRepository scanLogRepository) {
        this.scanLogRepository = scanLogRepository;
    }

    @Transactional
    public ScanLogResponse createLog(User user, ScanLogRequest request) {
        ScanLog scanLog = ScanLog.builder()
                .user(user)
                .eventType(request.getEventType())
                .contentType(request.getContentType())
                .action(request.getAction())
                .peopleDetected(request.getPeopleDetected())
                .packageName(request.getPackageName())
                .createdAt(request.getCreatedAt() != null ? request.getCreatedAt() : LocalDateTime.now())
                .build();

        ScanLog saved = scanLogRepository.save(scanLog);
        logger.info("Log created for user {}: {} - {}", user.getEmail(), request.getEventType(), request.getAction());

        return mapToResponse(saved);
    }

    @Transactional
    public BatchLogResponse createLogs(User user, List<ScanLogRequest> requests) {
        int created = 0;
        for (ScanLogRequest request : requests) {
            ScanLog scanLog = ScanLog.builder()
                    .user(user)
                    .eventType(request.getEventType())
                    .contentType(request.getContentType())
                    .action(request.getAction())
                    .peopleDetected(request.getPeopleDetected())
                    .packageName(request.getPackageName())
                    .createdAt(request.getCreatedAt() != null ? request.getCreatedAt() : LocalDateTime.now())
                    .build();
            scanLogRepository.save(scanLog);
            created++;
        }

        logger.info("Batch log created for user {}: {} logs", user.getEmail(), created);

        return BatchLogResponse.builder()
                .created(created)
                .message("Successfully created " + created + " logs")
                .build();
    }

    public ScanLogListResponse getUserLogs(User user, int limit, int offset) {
        PageRequest pageRequest = PageRequest.of(
                offset / limit,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ScanLog> page = scanLogRepository.findByUserId(user.getId(), pageRequest);

        List<ScanLogResponse> logs = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ScanLogListResponse.builder()
                .logs(logs)
                .total((int) page.getTotalElements())
                .limit(limit)
                .offset(offset)
                .build();
    }

    private ScanLogResponse mapToResponse(ScanLog scanLog) {
        return ScanLogResponse.builder()
                .id(scanLog.getId())
                .eventType(scanLog.getEventType())
                .contentType(scanLog.getContentType())
                .action(scanLog.getAction())
                .peopleDetected(scanLog.getPeopleDetected())
                .packageName(scanLog.getPackageName())
                .createdAt(scanLog.getCreatedAt())
                .build();
    }
}
