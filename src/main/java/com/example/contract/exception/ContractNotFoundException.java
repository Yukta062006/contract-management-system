package com.example.contract.exception;

/**
 * Exception thrown when a requested contract is not found by its ID.
 */
public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException(String message) {
        super(message);
    }

    public ContractNotFoundException(Long id) {
        super("Contract with ID " + id + " was not found.");
    }
}
