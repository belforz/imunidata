package com.example.imunidata.model;

public class ErrorResponse {
    private String message;
    private String status;
    private String error;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private long timestamp;

    public ErrorResponse(String message, String status, String error, long timestamp){
        this.message = message;
        this.status = status;
        this.error = error;
        this.timestamp = timestamp;
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) { super(message); }
    }

    public static class ResourceAlreadyExistsException extends RuntimeException {
        public ResourceAlreadyExistsException(String message) { super(message); }
    }

    public static class GenericServiceException extends RuntimeException {
        public GenericServiceException(String message) { super(message); }
    }


}
