package com.axel20378.heat_exchanger_selector.security.exception;

public class SelfModificationForbiddenException extends RuntimeException {
    public SelfModificationForbiddenException(String message) {
        super(message);
    }
}
