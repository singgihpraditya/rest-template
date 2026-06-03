package com.example.template.exception;

/**
 * Exception untuk pelanggaran business rule (409 Conflict).
 * Contoh: username sudah dipakai, email duplikat, stok habis, dll.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
