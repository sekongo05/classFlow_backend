package com.mosek.classFlow.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ClassFlowException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
    }
}
