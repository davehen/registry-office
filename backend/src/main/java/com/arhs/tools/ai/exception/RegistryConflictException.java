package com.arhs.tools.ai.exception;

public class RegistryConflictException extends RuntimeException {
    private final String resource;
    public RegistryConflictException(String message, String resource) {
        super(message);
        this.resource = resource;
    }
    public String getResource() { return resource; }
}
