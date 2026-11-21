package com.example.contract.service;

import com.example.contract.dto.ContractResponse;
import com.example.contract.dto.ContractSummaryResponse;
import com.example.contract.dto.QuestionResponse;
import com.example.contract.entity.Contract;
import com.example.contract.enums.ContractStatus;
import com.example.contract.exception.ContractNotFoundException;
import com.example.contract.exception.DocumentParsingException;
import com.example.contract.exception.InvalidFileException;
import com.example.contract.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Core business service handling contract lifecycle operations:
 * file uploading, document parsing, retrieval, status workflows, and Q&A queries.
 */
@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final FileStorageService fileStorageService;
    private final DocumentParserService documentParserService;
    private final QuestionAnswerService questionAnswerService;

    /**
     * Constructor injection of all required dependencies.
     */
    public ContractService(ContractRepository contractRepository,
                           FileStorageService fileStorageService,
                           DocumentParserService documentParserService,
                           QuestionAnswerService questionAnswerService) {
        this.contractRepository = contractRepository;
        this.fileStorageService = fileStorageService;
        this.documentParserService = documentParserService;
        this.questionAnswerService = questionAnswerService;
    }

    /**
     * Uploads and processes a contract document (PDF or DOCX) with a meaningful description.
     * Extracts text, stores the file securely, creates a database record with DRAFT status,
     * and performs file cleanup if parsing fails.
     *
     * @param file        The uploaded multipart file
     * @param description User-provided meaningful description
     * @return {@link ContractResponse} containing persisted metadata, description, and extracted text
     */
    @Transactional
    public ContractResponse uploadContract(MultipartFile file, String description) {
        validateUploadedFile(file);

        String originalFilename = file.getOriginalFilename();
        String fileType = determineFileType(originalFilename);

        // Store file locally
        Path storedFilePath = fileStorageService.storeFile(file);

        String extractedText;
        try {
            // Extract text from the stored file
            extractedText = documentParserService.extractText(storedFilePath, fileType);
        } catch (DocumentParsingException ex) {
            // Clean up stored file if parsing fails
            fileStorageService.deleteFile(storedFilePath);
            throw ex;
        } catch (Exception ex) {
            fileStorageService.deleteFile(storedFilePath);
            throw new DocumentParsingException("Failed to parse document: " + ex.getMessage(), ex);
        }

        // Clean description
        String cleanDescription = (description != null && !description.isBlank()) ? description.trim() : null;

        // Create and persist Contract entity
        Contract contract = new Contract(
                originalFilename,
                storedFilePath.toString(),
                fileType,
                ContractStatus.DRAFT,
                extractedText,
                cleanDescription
        );

        Contract savedContract = contractRepository.save(contract);
        return ContractResponse.fromEntity(savedContract);
    }

    /**
     * Overload for uploading without description (backward compatible).
     *
     * @param file The uploaded multipart file
     * @return {@link ContractResponse}
     */
    @Transactional
    public ContractResponse uploadContract(MultipartFile file) {
        return uploadContract(file, null);
    }

    /**
     * Retrieves all contracts in summary form (without full extracted text payload).
     *
     * @return List of {@link ContractSummaryResponse}
     */
    @Transactional(readOnly = true)
    public List<ContractSummaryResponse> getAllContracts() {
        return contractRepository.findAll()
                .stream()
                .map(ContractSummaryResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single contract by its ID including full extracted text and description.
     *
     * @param id The contract ID
     * @return {@link ContractResponse}
     * @throws ContractNotFoundException if no contract exists with the given ID
     */
    @Transactional(readOnly = true)
    public ContractResponse getContractById(Long id) {
        Contract contract = findContractEntityById(id);
        return ContractResponse.fromEntity(contract);
    }

    /**
     * Updates the status of an existing contract following the strict sequence:
     * DRAFT -> REVIEW -> APPROVED.
     *
     * @param id        The contract ID
     * @param newStatus The target status
     * @return Updated {@link ContractResponse}
     * @throws ContractNotFoundException         if contract does not exist
     * @throws com.example.contract.exception.InvalidStatusTransitionException if transition is disallowed
     */
    @Transactional
    public ContractResponse updateContractStatus(Long id, ContractStatus newStatus) {
        Contract contract = findContractEntityById(id);

        // Validate transition using ContractStatus workflow logic
        contract.getStatus().validateTransition(newStatus);

        contract.setStatus(newStatus);
        Contract updated = contractRepository.save(contract);
        return ContractResponse.fromEntity(updated);
    }

    /**
     * Searches the extracted text of a contract and answers a user's question.
     *
     * @param id       The contract ID
     * @param question Natural language question
     * @return {@link QuestionResponse} with the extracted answer or not-found status
     * @throws ContractNotFoundException if contract does not exist
     */
    @Transactional(readOnly = true)
    public QuestionResponse askQuestion(Long id, String question) {
        Contract contract = findContractEntityById(id);
        return questionAnswerService.answerQuestion(contract.getId(), contract.getExtractedText(), question);
    }

    /**
     * Helper to retrieve entity or throw ContractNotFoundException.
     */
    private Contract findContractEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Contract ID cannot be null.");
        }
        return contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
    }

    /**
     * Validates that the uploaded file is present, non-empty, and has an allowed format.
     */
    private void validateUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file cannot be empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("Uploaded file must have a valid filename.");
        }

        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
            throw new InvalidFileException(
                    "Unsupported file format: '" + originalFilename + "'. Only PDF (.pdf) and DOCX (.docx) files are supported."
            );
        }
    }

    /**
     * Resolves the normalized file type ("PDF" or "DOCX") from filename.
     */
    private String determineFileType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "PDF";
        }
        if (lower.endsWith(".docx")) {
            return "DOCX";
        }
        throw new InvalidFileException("Unable to determine file type for: " + filename);
    }
}
