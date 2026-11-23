package com.mosek.classFlow.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends ClassFlowException {
    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", message);
    }
}
