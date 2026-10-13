package com.systemguard.common.exception;

public class ResourceNotFoundException extends SystemGuardException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super("NOT_FOUND", resourceType + " not found: " + id);
    }
}
