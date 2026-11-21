package com.example.contract.dto;

import com.example.contract.entity.Contract;
import com.example.contract.enums.ContractStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing detailed contract information,
 * including extracted document content and contract description.
 */
public class ContractResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private ContractStatus status;
    private String extractedText;
    private String contractDescription;
    private LocalDateTime uploadedAt;

    public ContractResponse() {
    }

    /**
     * Backward-compatible 6-parameter constructor.
     */
    public ContractResponse(Long id, String fileName, String fileType, ContractStatus status,
                            String extractedText, LocalDateTime uploadedAt) {
        this(id, fileName, fileType, status, extractedText, null, uploadedAt);
    }

    /**
     * Full 7-parameter constructor including contractDescription.
     */
    public ContractResponse(Long id, String fileName, String fileType, ContractStatus status,
                            String extractedText, String contractDescription, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.status = status;
        this.extractedText = extractedText;
        this.contractDescription = contractDescription;
        this.uploadedAt = uploadedAt;
    }

    /**
     * Factory method to convert a Contract entity to a detailed ContractResponse DTO.
     */
    public static ContractResponse fromEntity(Contract contract) {
        if (contract == null) {
            return null;
        }
        return new ContractResponse(
                contract.getId(),
                contract.getFileName(),
                contract.getFileType(),
                contract.getStatus(),
                contract.getExtractedText(),
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

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
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
