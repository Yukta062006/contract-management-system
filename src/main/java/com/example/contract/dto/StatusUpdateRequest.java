package com.example.contract.dto;

import com.example.contract.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating the status of an existing contract.
 */
public class StatusUpdateRequest {

    @NotNull(message = "Status cannot be null and must be one of: DRAFT, REVIEW, APPROVED")
    private ContractStatus status;

    public StatusUpdateRequest() {
    }

    public StatusUpdateRequest(ContractStatus status) {
        this.status = status;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }
}
