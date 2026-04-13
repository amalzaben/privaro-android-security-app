package com.privaro.backend.dto;

import java.time.LocalDateTime;

public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse() {}

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static ErrorResponse of(int status, String error, String message) {
        ErrorResponse response = new ErrorResponse();
        response.status = status;
        response.error = error;
        response.message = message;
        response.timestamp = LocalDateTime.now();
        return response;
    }
}
