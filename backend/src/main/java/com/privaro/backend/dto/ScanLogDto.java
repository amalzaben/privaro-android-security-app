package com.privaro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public class ScanLogDto {

    // Request DTO for creating logs
    public static class ScanLogRequest {

        @NotBlank(message = "Event type is required")
        private String eventType;

        private String contentType;

        @NotBlank(message = "Action is required")
        private String action;

        private Integer peopleDetected;

        private String packageName;

        private LocalDateTime createdAt;

        public ScanLogRequest() {}

        public ScanLogRequest(String eventType, String contentType, String action,
                              Integer peopleDetected, String packageName, LocalDateTime createdAt) {
            this.eventType = eventType;
            this.contentType = contentType;
            this.action = action;
            this.peopleDetected = peopleDetected;
            this.packageName = packageName;
            this.createdAt = createdAt;
        }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public Integer getPeopleDetected() { return peopleDetected; }
        public void setPeopleDetected(Integer peopleDetected) { this.peopleDetected = peopleDetected; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // Response DTO for single log
    public static class ScanLogResponse {

        private Long id;
        private String eventType;
        private String contentType;
        private String action;
        private Integer peopleDetected;
        private String packageName;
        private LocalDateTime createdAt;

        public ScanLogResponse() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public Integer getPeopleDetected() { return peopleDetected; }
        public void setPeopleDetected(Integer peopleDetected) { this.peopleDetected = peopleDetected; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final ScanLogResponse response = new ScanLogResponse();

            public Builder id(Long id) { response.id = id; return this; }
            public Builder eventType(String eventType) { response.eventType = eventType; return this; }
            public Builder contentType(String contentType) { response.contentType = contentType; return this; }
            public Builder action(String action) { response.action = action; return this; }
            public Builder peopleDetected(Integer peopleDetected) { response.peopleDetected = peopleDetected; return this; }
            public Builder packageName(String packageName) { response.packageName = packageName; return this; }
            public Builder createdAt(LocalDateTime createdAt) { response.createdAt = createdAt; return this; }

            public ScanLogResponse build() { return response; }
        }
    }

    // Response DTO for paginated list of logs
    public static class ScanLogListResponse {

        private List<ScanLogResponse> logs;
        private int total;
        private int limit;
        private int offset;

        public ScanLogListResponse() {}

        public List<ScanLogResponse> getLogs() { return logs; }
        public void setLogs(List<ScanLogResponse> logs) { this.logs = logs; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final ScanLogListResponse response = new ScanLogListResponse();

            public Builder logs(List<ScanLogResponse> logs) { response.logs = logs; return this; }
            public Builder total(int total) { response.total = total; return this; }
            public Builder limit(int limit) { response.limit = limit; return this; }
            public Builder offset(int offset) { response.offset = offset; return this; }

            public ScanLogListResponse build() { return response; }
        }
    }

    // Response DTO for batch log creation
    public static class BatchLogResponse {

        private int created;
        private String message;

        public BatchLogResponse() {}

        public int getCreated() { return created; }
        public void setCreated(int created) { this.created = created; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final BatchLogResponse response = new BatchLogResponse();

            public Builder created(int created) { response.created = created; return this; }
            public Builder message(String message) { response.message = message; return this; }

            public BatchLogResponse build() { return response; }
        }
    }
}
