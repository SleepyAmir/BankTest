package com.springbank.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String entity, Long id) {
        super(String.format("%s not found with id: %d", entity, id));
    }
    public ResourceNotFoundException(String entity, String identifier) {
        super(String.format("%s not found: %s", entity, identifier));
    }
}
