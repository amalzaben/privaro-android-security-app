package com.privaro.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_logs")
public class ScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false)
    private String eventType;  // "scan_performed", "sensitive_content_detected"

    @Column(name = "content_type")
    private String contentType;  // "PASSWORD", "OTP", etc.

    @Column(name = "action", nullable = false)
    private String action;  // "continued", "cancelled", "safe", "people_detected"

    @Column(name = "people_detected")
    private Integer peopleDetected;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ScanLog() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getEventType() { return eventType; }
    public String getContentType() { return contentType; }
    public String getAction() { return action; }
    public Integer getPeopleDetected() { return peopleDetected; }
    public String getPackageName() { return packageName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setAction(String action) { this.action = action; }
    public void setPeopleDetected(Integer peopleDetected) { this.peopleDetected = peopleDetected; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ScanLog scanLog = new ScanLog();

        public Builder user(User user) { scanLog.user = user; return this; }
        public Builder eventType(String eventType) { scanLog.eventType = eventType; return this; }
        public Builder contentType(String contentType) { scanLog.contentType = contentType; return this; }
        public Builder action(String action) { scanLog.action = action; return this; }
        public Builder peopleDetected(Integer peopleDetected) { scanLog.peopleDetected = peopleDetected; return this; }
        public Builder packageName(String packageName) { scanLog.packageName = packageName; return this; }
        public Builder createdAt(LocalDateTime createdAt) { scanLog.createdAt = createdAt; return this; }

        public ScanLog build() { return scanLog; }
    }
}
