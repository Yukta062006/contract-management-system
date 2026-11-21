package com.example.contract.enums;

import com.example.contract.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests validating contract status transitions according to the
 * DRAFT -> REVIEW -> APPROVED workflow rules.
 */
class ContractStatusTest {

    @Test
    @DisplayName("Should allow valid transition from DRAFT to REVIEW")
    void testValidTransitionDraftToReview() {
        assertTrue(ContractStatus.DRAFT.canTransitionTo(ContractStatus.REVIEW));
        assertDoesNotThrow(() -> ContractStatus.DRAFT.validateTransition(ContractStatus.REVIEW));
    }

    @Test
    @DisplayName("Should allow valid transition from REVIEW to APPROVED")
    void testValidTransitionReviewToApproved() {
        assertTrue(ContractStatus.REVIEW.canTransitionTo(ContractStatus.APPROVED));
        assertDoesNotThrow(() -> ContractStatus.REVIEW.validateTransition(ContractStatus.APPROVED));
    }

    @Test
    @DisplayName("Should reject skipped transition from DRAFT to APPROVED")
    void testRejectSkippedTransitionDraftToApproved() {
        assertFalse(ContractStatus.DRAFT.canTransitionTo(ContractStatus.APPROVED));
        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.DRAFT.validateTransition(ContractStatus.APPROVED));
    }

    @Test
    @DisplayName("Should reject backward transition from APPROVED to REVIEW")
    void testRejectBackwardTransitionApprovedToReview() {
        assertFalse(ContractStatus.APPROVED.canTransitionTo(ContractStatus.REVIEW));
        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.APPROVED.validateTransition(ContractStatus.REVIEW));
    }

    @Test
    @DisplayName("Should reject backward transition from REVIEW to DRAFT")
    void testRejectBackwardTransitionReviewToDraft() {
        assertFalse(ContractStatus.REVIEW.canTransitionTo(ContractStatus.DRAFT));
        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.REVIEW.validateTransition(ContractStatus.DRAFT));
    }

    @Test
    @DisplayName("Should reject repeated status update from DRAFT to DRAFT")
    void testRejectRepeatedDraftStatus() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.DRAFT.validateTransition(ContractStatus.DRAFT));
    }

    @Test
    @DisplayName("Should reject repeated status update from REVIEW to REVIEW")
    void testRejectRepeatedReviewStatus() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.REVIEW.validateTransition(ContractStatus.REVIEW));
    }

    @Test
    @DisplayName("Should reject any transition once contract is APPROVED")
    void testRejectAnyTransitionAfterApproved() {
        assertFalse(ContractStatus.APPROVED.canTransitionTo(ContractStatus.DRAFT));
        assertFalse(ContractStatus.APPROVED.canTransitionTo(ContractStatus.REVIEW));
        assertFalse(ContractStatus.APPROVED.canTransitionTo(ContractStatus.APPROVED));

        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.APPROVED.validateTransition(ContractStatus.APPROVED));
        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.APPROVED.validateTransition(ContractStatus.REVIEW));
    }

    @Test
    @DisplayName("Should reject null target status")
    void testRejectNullTargetStatus() {
        assertFalse(ContractStatus.DRAFT.canTransitionTo(null));
        assertThrows(InvalidStatusTransitionException.class, () ->
                ContractStatus.DRAFT.validateTransition(null));
    }
}
