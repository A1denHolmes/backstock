package com.aidenholmes.backstock.common;

/**
 * Base class for exceptions that signal a domain error.
 *
 * <p>The message is returned to the client, so it must not contain internal details such as SQL, class names or
 * personal data.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
