package com.mosek.classFlow.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ClassFlowException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
