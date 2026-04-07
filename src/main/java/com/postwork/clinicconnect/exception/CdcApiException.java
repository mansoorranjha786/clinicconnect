package com.postwork.clinicconnect.exception;

public class CdcApiException extends RuntimeException {

    public CdcApiException(String message) {
        super(message);
    }

    public CdcApiException(String message, Throwable cause) {
        super(message, cause);
    }
}