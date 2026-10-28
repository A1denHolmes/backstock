package com.aidenholmes.backstock.common.web;

/**
 * Thrown when a request that changes a resource does not carry a usable precondition, such as a missing
 * {@code If-Match} header.
 *
 * <p>The message is returned to the client, so it must not contain internal details.
 */
public class PreconditionRequiredException extends RuntimeException {

    public PreconditionRequiredException(String message) {
        super(message);
    }
}
