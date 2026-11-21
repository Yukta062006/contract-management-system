package com.example.contract.enums;

import com.example.contract.exception.InvalidStatusTransitionException;

/**
 * Enumeration representing the lifecycle status of a contract.
 *
 * <p>Allowed status workflow:
 * DRAFT -> REVIEW -> APPROVED
 *
 * <p>Status transition rules are strictly encapsulated within this enum to ensure
 * business rules are cohesive and not scattered across the application.
 */
public enum ContractStatus {
    DRAFT,
    REVIEW,
    APPROVED;

    /**
     * Determines whether transitioning from this status to the given target status is valid.
     *
     * @param targetStatus The requested new status
     * @return true if the transition is allowed according to workflow rules, false otherwise
     */
    public boolean canTransitionTo(ContractStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        return switch (this) {
            case DRAFT -> targetStatus == REVIEW;
            case REVIEW -> targetStatus == APPROVED;
            case APPROVED -> false; // No transitions allowed after APPROVED
        };
    }

    /**
     * Validates a status transition. If the transition is invalid, a descriptive
     * {@link InvalidStatusTransitionException} is thrown.
     *
     * @param targetStatus The requested new status
     * @throws InvalidStatusTransitionException if the status change is invalid, repeated, backward, or skipped
     */
    public void validateTransition(ContractStatus targetStatus) {
        if (targetStatus == null) {
            throw new InvalidStatusTransitionException("Target status cannot be null");
        }

        if (this == targetStatus) {
            throw new InvalidStatusTransitionException(
                    "Contract is already in " + this + " status. Repeated status updates are not allowed."
            );
        }

        if (this == APPROVED) {
            throw new InvalidStatusTransitionException(
                    "Contract is already APPROVED. No further status changes are permitted."
            );
        }

        if (!canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format(
                            "Invalid status transition from %s to %s. Allowed sequence is strictly: DRAFT -> REVIEW -> APPROVED.",
                            this, targetStatus
                    )
            );
        }
    }
}
