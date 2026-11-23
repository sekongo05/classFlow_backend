package com.mosek.classFlow.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedActionException extends ClassFlowException {
    public UnauthorizedActionException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
