package com.bonitasoft.connectors.dynamics;

public class DynamicsException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public DynamicsException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public DynamicsException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public DynamicsException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public DynamicsException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
