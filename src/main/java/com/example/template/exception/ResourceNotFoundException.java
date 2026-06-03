package com.example.template.exception;

/**
 * Exception untuk resource yang tidak ditemukan (404 Not Found).
 * Throw exception ini ketika data yang dicari tidak ada di database.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " dengan ID " + id + " tidak ditemukan");
    }
}
