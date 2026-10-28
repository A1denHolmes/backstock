package com.aidenholmes.backstock.common.error;

/**
 * Thrown when a resource does not exist or belongs to another user. Both cases are reported the same way, so the
 * existence of another user's resource is not revealed.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
