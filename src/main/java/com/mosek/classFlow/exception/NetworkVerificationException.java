package com.mosek.classFlow.exception;

import org.springframework.http.HttpStatus;

public class NetworkVerificationException extends ClassFlowException {
    public NetworkVerificationException(String message) {
        super(HttpStatus.FORBIDDEN, "NETWORK_VERIFICATION_FAILED", message);
    }
}
