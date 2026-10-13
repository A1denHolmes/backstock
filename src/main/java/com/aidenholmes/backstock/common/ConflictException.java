package com.aidenholmes.backstock.common;

/**
 * Thrown when an operation conflicts with the current state of the data, such as a unique value that is already
 * taken.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
