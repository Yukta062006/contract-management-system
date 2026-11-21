package com.example.contract.exception;

/**
 * Exception thrown when document parsing fails (corrupted file, unreadable content, encryption).
 */
public class DocumentParsingException extends RuntimeException {

    public DocumentParsingException(String message) {
        super(message);
    }

    public DocumentParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
