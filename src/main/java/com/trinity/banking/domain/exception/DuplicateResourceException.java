package com.trinity.banking.domain.exception;

/** El recurso ya existe (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
