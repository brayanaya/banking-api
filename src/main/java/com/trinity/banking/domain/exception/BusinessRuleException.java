package com.trinity.banking.domain.exception;

/** Se violó una regla de negocio (HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
