package com.aidenholmes.backstock.common.error;

/**
 * Thrown when an operation is not allowed by a business rule, regardless of the current state of the data.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
