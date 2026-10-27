package com.aidenholmes.backstock.common;

/**
 * A service result together with the version of the entity it was built from.
 */
public record Versioned<T>(T body, long version) {}
