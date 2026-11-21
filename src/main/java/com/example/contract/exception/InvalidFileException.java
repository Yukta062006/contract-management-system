package com.example.contract.exception;

/**
 * Exception thrown when an uploaded file is invalid (empty, unsupported format, unsafe name, etc.).
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
