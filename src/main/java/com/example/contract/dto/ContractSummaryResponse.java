package com.example.contract.dto;

import com.example.contract.entity.Contract;
import com.example.contract.enums.ContractStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a summary of a contract.
 * Used for list endpoints to avoid transferring large extracted text payloads.
 */
public class ContractSummaryResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private ContractStatus status;
    private String contractDescription;
    private LocalDateTime uploadedAt;

    public ContractSummaryResponse() {
    }

    /**
     * Backward-compatible 5-parameter constructor.
     */
    public ContractSummaryResponse(Long id, String fileName, String fileType,
                                   ContractStatus status, LocalDateTime uploadedAt) {
        this(id, fileName, fileType, status, null, uploadedAt);
    }

    /**
     * Full 6-parameter constructor including contractDescription.
     */
    public ContractSummaryResponse(Long id, String fileName, String fileType,
                                   ContractStatus status, String contractDescription, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.status = status;
        this.contractDescription = contractDescription;
        this.uploadedAt = uploadedAt;
    }

    /**
     * Factory method to convert a Contract entity to a lightweight ContractSummaryResponse DTO.
     */
    public static ContractSummaryResponse fromEntity(Contract contract) {
        if (contract == null) {
            return null;
        }
        return new ContractSummaryResponse(
                contract.getId(),
                contract.getFileName(),
                contract.getFileType(),
                contract.getStatus(),
                contract.getContractDescription(),
                contract.getUploadedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public String getContractDescription() {
        return contractDescription;
    }

    public void setContractDescription(String contractDescription) {
        this.contractDescription = contractDescription;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
