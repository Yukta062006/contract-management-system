package com.example.contract.exception;

/**
 * Exception thrown when an invalid contract status transition is requested.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
