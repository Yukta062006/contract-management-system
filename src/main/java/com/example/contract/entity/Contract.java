package com.example.contract.entity;

import com.example.contract.enums.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a Contract stored in the database.
 * Corresponds to the "contracts" table in MySQL.
 */
@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "contract_description", columnDefinition = "TEXT")
    private String contractDescription;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    /**
     * Default constructor required by JPA/Hibernate.
     */
    public Contract() {
    }

    /**
     * Parameterized constructor without description (backward compatible).
     */
    public Contract(String fileName, String filePath, String fileType, ContractStatus status, String extractedText) {
        this(fileName, filePath, fileType, status, extractedText, null);
    }

    /**
     * Full parameterized constructor including contract description.
     *
     * @param fileName            Original uploaded filename
     * @param filePath            Filesystem path where the file is stored
     * @param fileType            File extension/type (e.g., "PDF" or "DOCX")
     * @param status              Initial lifecycle status (typically DRAFT)
     * @param extractedText       Text content extracted from the document
     * @param contractDescription User-entered meaningful contract description
     */
    public Contract(String fileName, String filePath, String fileType, ContractStatus status,
                    String extractedText, String contractDescription) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.status = status;
        this.extractedText = extractedText;
        this.contractDescription = contractDescription;
        this.uploadedAt = LocalDateTime.now();
    }

    /**
     * Lifecycle callback to automatically populate uploadedAt if not already set,
     * and default status to DRAFT if null.
     */
    @PrePersist
    protected void onPrePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ContractStatus.DRAFT;
        }
    }

    // --- Getters and Setters (manually implemented, no Lombok) ---

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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    @Override
    public String toString() {
        return "Contract{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileType='" + fileType + '\'' +
                ", status=" + status +
                ", contractDescription='" + contractDescription + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
