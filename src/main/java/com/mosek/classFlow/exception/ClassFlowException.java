package com.mosek.classFlow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ClassFlowException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ClassFlowException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
