package com.aidenholmes.backstock.common;

/**
 * Thrown when the version a client expects does not match the current version of the resource, including entity tags
 * that cannot match any version.
 */
public class VersionMismatchException extends DomainException {

    public VersionMismatchException(String message) {
        super(message);
    }
}
